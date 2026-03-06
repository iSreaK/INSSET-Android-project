package com.example.jvbench.data.remote.supabase;

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
}
