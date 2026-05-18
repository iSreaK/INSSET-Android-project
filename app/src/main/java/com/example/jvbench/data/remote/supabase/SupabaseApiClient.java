package com.example.jvbench.data.remote.supabase;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SupabaseApiClient {
    private static final MediaType JSON_MEDIA_TYPE = MediaType.get("application/json; charset=utf-8");

    private final SupabaseClientProvider provider;
    private final SupabaseSessionStore sessionStore;

    public SupabaseApiClient(SupabaseClientProvider provider, SupabaseSessionStore sessionStore) {
        this.provider = provider;
        this.sessionStore = sessionStore;
    }

    public SupabaseResponse get(String absoluteUrl, boolean requiresAuth) {
        return execute("GET", absoluteUrl, null, requiresAuth, null);
    }

    public SupabaseResponse post(String absoluteUrl, JSONObject payload, boolean requiresAuth) {
        return execute("POST", absoluteUrl, payload, requiresAuth, null);
    }

    public SupabaseResponse patch(String absoluteUrl, JSONObject payload, boolean requiresAuth) {
        return execute("PATCH", absoluteUrl, payload, requiresAuth, null);
    }

    public SupabaseResponse delete(String absoluteUrl, boolean requiresAuth) {
        return execute("DELETE", absoluteUrl, null, requiresAuth, null);
    }

    public SupabaseResponse post(String absoluteUrl, JSONObject payload, boolean requiresAuth, String preferHeader) {
        return execute("POST", absoluteUrl, payload, requiresAuth, preferHeader);
    }

    public SupabaseResponse patch(String absoluteUrl, JSONObject payload, boolean requiresAuth, String preferHeader) {
        return execute("PATCH", absoluteUrl, payload, requiresAuth, preferHeader);
    }

    public SupabaseResponse uploadBinary(String absoluteUrl, byte[] body, String contentType, boolean requiresAuth, boolean upsert) {
        if (!provider.isConfigured()) {
            return SupabaseResponse.failure(0, null, "Supabase is not configured. Set SUPABASE_URL and SUPABASE_ANON_KEY.");
        }
        SupabaseResponse first = doUploadBinary(absoluteUrl, body, contentType, requiresAuth, upsert);
        if (first.isSuccessful() || !shouldRetryAfterRefresh(first, requiresAuth)) {
            return first;
        }
        if (!refreshAccessToken()) {
            return first;
        }
        return doUploadBinary(absoluteUrl, body, contentType, requiresAuth, upsert);
    }

    private SupabaseResponse doUploadBinary(String absoluteUrl, byte[] body, String contentType, boolean requiresAuth, boolean upsert) {
        if (requiresAuth && !sessionStore.isAuthenticated()) {
            return SupabaseResponse.failure(401, null, "User is not authenticated.");
        }
        MediaType mediaType = MediaType.parse(contentType == null || contentType.isBlank() ? "application/octet-stream" : contentType);
        RequestBody requestBody = RequestBody.create(body == null ? new byte[0] : body, mediaType);

        Request.Builder builder = new Request.Builder()
                .url(absoluteUrl)
                .addHeader("apikey", provider.getAnonKey());
        String bearer = sessionStore.isAuthenticated() ? sessionStore.getAccessToken() : provider.getAnonKey();
        builder.addHeader("Authorization", "Bearer " + bearer);
        if (upsert) {
            builder.addHeader("x-upsert", "true");
        }
        builder.post(requestBody);

        try (Response response = provider.getHttpClient().newCall(builder.build()).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";
            if (response.isSuccessful()) {
                return SupabaseResponse.success(response.code(), responseBody);
            }
            String error = responseBody != null && !responseBody.isBlank()
                    ? responseBody
                    : "Supabase error code " + response.code();
            return SupabaseResponse.failure(response.code(), responseBody, error);
        } catch (IOException exception) {
            return SupabaseResponse.failure(0, null, exception.getMessage());
        }
    }

    private SupabaseResponse execute(
            String method,
            String absoluteUrl,
            JSONObject payload,
            boolean requiresAuth,
            String preferHeader
    ) {
        if (!provider.isConfigured()) {
            return SupabaseResponse.failure(0, null, "Supabase is not configured. Set SUPABASE_URL and SUPABASE_ANON_KEY.");
        }
        SupabaseResponse first = doExecute(method, absoluteUrl, payload, requiresAuth, preferHeader);
        if (first.isSuccessful() || !shouldRetryAfterRefresh(first, requiresAuth)) {
            return first;
        }
        if (!refreshAccessToken()) {
            return first;
        }
        return doExecute(method, absoluteUrl, payload, requiresAuth, preferHeader);
    }

    private SupabaseResponse doExecute(
            String method,
            String absoluteUrl,
            JSONObject payload,
            boolean requiresAuth,
            String preferHeader
    ) {
        if (requiresAuth && !sessionStore.isAuthenticated()) {
            return SupabaseResponse.failure(401, null, "User is not authenticated.");
        }

        Request.Builder builder = new Request.Builder()
                .url(absoluteUrl)
                .addHeader("apikey", provider.getAnonKey())
                .addHeader("Content-Type", "application/json");
        if (preferHeader != null && !preferHeader.isBlank()) {
            builder.addHeader("Prefer", preferHeader);
        }

        String bearer = sessionStore.isAuthenticated() ? sessionStore.getAccessToken() : provider.getAnonKey();
        builder.addHeader("Authorization", "Bearer " + bearer);

        RequestBody body = payload == null
                ? RequestBody.create(new byte[0], JSON_MEDIA_TYPE)
                : RequestBody.create(payload.toString(), JSON_MEDIA_TYPE);

        switch (method) {
            case "GET":
                builder.get();
                break;
            case "POST":
                builder.post(body);
                break;
            case "PATCH":
                builder.patch(body);
                break;
            case "DELETE":
                builder.delete();
                break;
            default:
                return SupabaseResponse.failure(0, null, "Unsupported HTTP method: " + method);
        }

        try (Response response = provider.getHttpClient().newCall(builder.build()).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";
            if (response.isSuccessful()) {
                return SupabaseResponse.success(response.code(), responseBody);
            }
            String error = responseBody != null && !responseBody.isBlank()
                    ? responseBody
                    : "Supabase error code " + response.code();
            return SupabaseResponse.failure(response.code(), responseBody, error);
        } catch (IOException exception) {
            return SupabaseResponse.failure(0, null, exception.getMessage());
        }
    }

    private boolean shouldRetryAfterRefresh(SupabaseResponse response, boolean requiresAuth) {
        if (response.getCode() != 401) return false;
        if (!sessionStore.isAuthenticated()) return false;
        if (sessionStore.getRefreshToken() == null || sessionStore.getRefreshToken().isBlank()) return false;
        String body = response.getBody() != null ? response.getBody().toLowerCase() : "";
        // Only retry on auth-related 401: either we sent Authorization (requiresAuth=true)
        // or the body explicitly mentions an expired JWT (PostgREST returns that even on
        // unauthenticated reads when our token is just stale).
        if (requiresAuth) return true;
        return body.contains("jwt") || body.contains("expired");
    }

    /**
     * Calls Supabase /auth/v1/token?grant_type=refresh_token with the stored refresh token.
     * Returns true if a new access token was persisted; false otherwise (and clears the
     * session if the refresh token itself is invalid).
     */
    private synchronized boolean refreshAccessToken() {
        String refresh = sessionStore.getRefreshToken();
        if (refresh == null || refresh.isBlank()) return false;
        try {
            JSONObject payload = new JSONObject().put("refresh_token", refresh);
            String url = provider.getAuthBaseUrl() + "/token?grant_type=refresh_token";
            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("apikey", provider.getAnonKey())
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(payload.toString(), JSON_MEDIA_TYPE))
                    .build();
            try (Response response = provider.getHttpClient().newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    // Refresh token is dead -> clear session so the UI shows a clean "not logged in" state
                    if (response.code() == 400 || response.code() == 401) {
                        sessionStore.clearSession();
                    }
                    return false;
                }
                String bodyStr = response.body() != null ? response.body().string() : "";
                JSONObject json = new JSONObject(bodyStr);
                String newAccess = json.optString("access_token", null);
                String newRefresh = json.optString("refresh_token", null);
                if (newAccess == null || newAccess.isBlank()) return false;
                sessionStore.updateAccessToken(newAccess, newRefresh);
                return true;
            }
        } catch (IOException | JSONException exception) {
            return false;
        }
    }
}
