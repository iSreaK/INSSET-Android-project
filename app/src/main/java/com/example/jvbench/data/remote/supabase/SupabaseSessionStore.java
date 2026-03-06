package com.example.jvbench.data.remote.supabase;

import androidx.annotation.Nullable;

public class SupabaseSessionStore {
    private String accessToken;
    private String userId;
    private String userEmail;

    public void saveSession(String accessToken, String userId, String userEmail) {
        this.accessToken = accessToken;
        this.userId = userId;
        this.userEmail = userEmail;
    }

    public void clearSession() {
        accessToken = null;
        userId = null;
        userEmail = null;
    }

    @Nullable
    public String getAccessToken() {
        return accessToken;
    }

    @Nullable
    public String getUserId() {
        return userId;
    }

    @Nullable
    public String getUserEmail() {
        return userEmail;
    }

    public boolean isAuthenticated() {
        return accessToken != null && !accessToken.isBlank();
    }
}
