package com.example.jvbench.data.repository;

import com.example.jvbench.core.common.AuthCallback;
import com.example.jvbench.core.common.ResultCallback;
import com.example.jvbench.data.mapper.UserMapper;
import com.example.jvbench.data.remote.supabase.SupabaseApiClient;
import com.example.jvbench.data.remote.supabase.SupabaseClientProvider;
import com.example.jvbench.data.remote.supabase.SupabaseResponse;
import com.example.jvbench.data.remote.supabase.SupabaseSessionStore;
import com.example.jvbench.domain.model.User;
import com.example.jvbench.domain.model.UserRole;
import com.example.jvbench.domain.repository.AuthRepository;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SupabaseAuthRepository implements AuthRepository {
    private final SupabaseClientProvider clientProvider;
    private final SupabaseApiClient apiClient;
    private final SupabaseSessionStore sessionStore;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private User currentUser;

    public SupabaseAuthRepository(
            SupabaseClientProvider clientProvider,
            SupabaseApiClient apiClient,
            SupabaseSessionStore sessionStore
    ) {
        this.clientProvider = clientProvider;
        this.apiClient = apiClient;
        this.sessionStore = sessionStore;
    }

    @Override
    public void signIn(String email, String password, AuthCallback callback) {
        executor.execute(() -> {
            if (email == null || email.isBlank() || password == null || password.isBlank()) {
                callback.onError("Email et mot de passe obligatoires.");
                return;
            }
            try {
                JSONObject payload = new JSONObject()
                        .put("email", email)
                        .put("password", password);
                SupabaseResponse response = apiClient.post(
                        clientProvider.getAuthBaseUrl() + "/token?grant_type=password",
                        payload,
                        false
                );
                if (!response.isSuccessful()) {
                    callback.onError(mapAuthError(response));
                    return;
                }
                JSONObject json = new JSONObject(response.getBody());
                String token = json.getString("access_token");
                JSONObject userJson = json.getJSONObject("user");
                String userId = userJson.getString("id");
                String userEmail = userJson.optString("email", email);
                sessionStore.saveSession(token, userId, userEmail);

                User profileUser = fetchProfileOrFail(userId, userEmail);
                currentUser = profileUser;
                callback.onSuccess(profileUser);
            } catch (Exception exception) {
                callback.onError(exception.getMessage());
            }
        });
    }

    @Override
    public void signUp(String email, String password, AuthCallback callback) {
        executor.execute(() -> {
            if (email == null || email.isBlank() || password == null || password.length() < 6) {
                callback.onError("Les informations d’inscription sont invalides.");
                return;
            }
            try {
                JSONObject payload = new JSONObject()
                        .put("email", email)
                        .put("password", password);

                SupabaseResponse response = apiClient.post(
                        clientProvider.getAuthBaseUrl() + "/signup",
                        payload,
                        false
                );
                if (!response.isSuccessful()) {
                    callback.onError(mapAuthError(response));
                    return;
                }

                JSONObject json = new JSONObject(response.getBody());
                JSONObject userJson = json.optJSONObject("user");
                String userId = userJson != null
                        ? userJson.optString("id", UUID.randomUUID().toString())
                        : UUID.randomUUID().toString();
                String userEmail = userJson != null ? userJson.optString("email", email) : email;
                String token = json.optString("access_token", null);
                if (token == null || token.isBlank()) {
                    // If signup does not return a session (email confirmation mode), we try sign-in.
                    token = trySignInAndGetToken(email, password);
                }
                if (token == null || token.isBlank()) {
                    currentUser = new User(userId, userEmail, UserRole.USER);
                    callback.onSuccess(currentUser);
                    return;
                }
                sessionStore.saveSession(token, userId, userEmail);

                User syncedUser = fetchProfileOrFail(userId, userEmail);
                currentUser = syncedUser;
                callback.onSuccess(currentUser);
            } catch (Exception exception) {
                callback.onError(exception.getMessage());
            }
        });
    }

    @Override
    public void signOut(ResultCallback<Void> callback) {
        executor.execute(() -> {
            SupabaseResponse response = apiClient.post(
                    clientProvider.getAuthBaseUrl() + "/logout",
                    new JSONObject(),
                    true
            );
            if (!response.isSuccessful() && response.getCode() != 401) {
                callback.onError(response.getError());
                return;
            }
            currentUser = null;
            sessionStore.clearSession();
            callback.onSuccess(null);
        });
    }

    @Override
    public User getCurrentUser() {
        if (currentUser != null) {
            return currentUser;
        }
        if (!sessionStore.isAuthenticated() || sessionStore.getUserId() == null || sessionStore.getUserEmail() == null) {
            return null;
        }
        currentUser = new User(sessionStore.getUserId(), sessionStore.getUserEmail(), UserRole.USER);
        return currentUser;
    }

    @Override
    public void loadCurrentUser(ResultCallback<User> callback) {
        executor.execute(() -> {
            if (!sessionStore.isAuthenticated()
                    || sessionStore.getUserId() == null
                    || sessionStore.getUserEmail() == null) {
                callback.onError("Aucune session active.");
                return;
            }
            try {
                User user = fetchProfileOrFail(sessionStore.getUserId(), sessionStore.getUserEmail());
                currentUser = user;
                callback.onSuccess(user);
            } catch (Exception exception) {
                callback.onError(exception.getMessage());
            }
        });
    }

    private User fetchProfileOrFail(String userId, String userEmail) {
        String url = clientProvider.getRestBaseUrl()
                + "/profiles?select=id,email,role&id=eq." + userId + "&limit=1";
        SupabaseResponse response = apiClient.get(url, true);
        if (!response.isSuccessful()) {
            throw new RuntimeException("Impossible de récupérer le profil: " + response.getError());
        }
        try {
            JSONArray array = new JSONArray(response.getBody());
            if (array.length() == 0) {
                createOrUpdateProfileOrFail(userId, userEmail, UserRole.USER);
                SupabaseResponse recheck = apiClient.get(url, true);
                if (!recheck.isSuccessful()) {
                    throw new RuntimeException("Échec de création du profil: " + recheck.getError());
                }
                JSONArray recheckArray = new JSONArray(recheck.getBody());
                if (recheckArray.length() == 0) {
                    throw new RuntimeException("Le profil est introuvable après création.");
                }
                return UserMapper.fromProfileJson(recheckArray.getJSONObject(0));
            }
            return UserMapper.fromProfileJson(array.getJSONObject(0));
        } catch (JSONException jsonException) {
            throw new RuntimeException(jsonException.getMessage());
        }
    }

    private void createOrUpdateProfileOrFail(String userId, String email, UserRole role) {
        try {
            JSONObject payload = new JSONObject()
                    .put("id", userId)
                    .put("email", email)
                    .put("role", role.name());
            SupabaseResponse response = apiClient.post(
                    clientProvider.getRestBaseUrl() + "/profiles",
                    payload,
                    true,
                    "resolution=merge-duplicates,return=minimal"
            );
            if (!response.isSuccessful()) {
                throw new RuntimeException(response.getError());
            }
        } catch (JSONException jsonException) {
            throw new RuntimeException(jsonException.getMessage());
        }
    }

    private String trySignInAndGetToken(String email, String password) {
        try {
            JSONObject payload = new JSONObject()
                    .put("email", email)
                    .put("password", password);
            SupabaseResponse signInResponse = apiClient.post(
                    clientProvider.getAuthBaseUrl() + "/token?grant_type=password",
                    payload,
                    false
            );
            if (!signInResponse.isSuccessful()) {
                return null;
            }
            JSONObject signInJson = new JSONObject(signInResponse.getBody());
            return signInJson.optString("access_token", null);
        } catch (JSONException exception) {
            return null;
        }
    }

    private String mapAuthError(SupabaseResponse response) {
        String raw = response.getError() != null ? response.getError() : "";
        String lower = raw.toLowerCase();
        if (response.getCode() == 429 || lower.contains("over_email_send_rate_limit")) {
            return "Trop de tentatives d’inscription. Réessayez dans quelques minutes.";
        }
        if (lower.contains("invalid login credentials")) {
            return "Email ou mot de passe incorrect.";
        }
        if (lower.contains("user already registered")) {
            return "Un compte existe déjà avec cet email.";
        }
        return raw.isBlank() ? "Erreur d’authentification." : raw;
    }
}
