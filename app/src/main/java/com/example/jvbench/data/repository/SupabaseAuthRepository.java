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
                callback.onError("Email and password are required.");
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
                    callback.onError(response.getError());
                    return;
                }
                JSONObject json = new JSONObject(response.getBody());
                String token = json.getString("access_token");
                JSONObject userJson = json.getJSONObject("user");
                String userId = userJson.getString("id");
                String userEmail = userJson.optString("email", email);
                sessionStore.saveSession(token, userId, userEmail);

                User profileUser = fetchProfileOrDefault(userId, userEmail);
                currentUser = profileUser;
                callback.onSuccess(profileUser);
            } catch (JSONException exception) {
                callback.onError(exception.getMessage());
            }
        });
    }

    @Override
    public void signUp(String email, String password, AuthCallback callback) {
        executor.execute(() -> {
            if (email == null || email.isBlank() || password == null || password.length() < 6) {
                callback.onError("Invalid sign up payload.");
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
                    callback.onError(response.getError());
                    return;
                }

                JSONObject json = new JSONObject(response.getBody());
                JSONObject userJson = json.optJSONObject("user");
                String userId = userJson != null
                        ? userJson.optString("id", UUID.randomUUID().toString())
                        : UUID.randomUUID().toString();
                String userEmail = userJson != null ? userJson.optString("email", email) : email;
                String token = json.optString("access_token", null);
                if (token != null && !token.isBlank()) {
                    sessionStore.saveSession(token, userId, userEmail);
                    createOrUpdateProfile(userId, userEmail, UserRole.USER);
                }

                currentUser = new User(userId, userEmail, UserRole.USER);
                callback.onSuccess(currentUser);
            } catch (JSONException exception) {
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
        return currentUser;
    }

    private User fetchProfileOrDefault(String userId, String userEmail) {
        String url = clientProvider().getRestBaseUrl()
                + "/profiles?select=id,email,role&id=eq." + userId + "&limit=1";
        SupabaseResponse response = apiClient.get(url, true);
        if (!response.isSuccessful()) {
            return new User(userId, userEmail, UserRole.USER);
        }
        try {
            JSONArray array = new JSONArray(response.getBody());
            if (array.length() == 0) {
                createOrUpdateProfile(userId, userEmail, UserRole.USER);
                return new User(userId, userEmail, UserRole.USER);
            }
            return UserMapper.fromProfileJson(array.getJSONObject(0));
        } catch (JSONException exception) {
            return new User(userId, userEmail, UserRole.USER);
        }
    }

    private void createOrUpdateProfile(String userId, String email, UserRole role) {
        try {
            JSONObject payload = new JSONObject()
                    .put("id", userId)
                    .put("email", email)
                    .put("role", role.name());
            apiClient.post(
                    clientProvider.getRestBaseUrl() + "/profiles",
                    payload,
                    true,
                    "resolution=merge-duplicates,return=minimal"
            );
        } catch (JSONException ignored) {
            // Keep auth flow simple: profile sync failure does not block login/signup.
        }
    }

    private SupabaseClientProvider clientProvider() {
        return clientProvider;
    }
}
