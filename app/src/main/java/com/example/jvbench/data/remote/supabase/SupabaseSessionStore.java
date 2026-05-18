package com.example.jvbench.data.remote.supabase;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;

public class SupabaseSessionStore {
    private static final String PREF_NAME = "supabase_session";
    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USER_EMAIL = "user_email";

    private final SharedPreferences preferences;
    private String accessToken;
    private String refreshToken;
    private String userId;
    private String userEmail;

    public SupabaseSessionStore(Context context) {
        preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        accessToken = preferences.getString(KEY_ACCESS_TOKEN, null);
        refreshToken = preferences.getString(KEY_REFRESH_TOKEN, null);
        userId = preferences.getString(KEY_USER_ID, null);
        userEmail = preferences.getString(KEY_USER_EMAIL, null);
    }

    public void saveSession(String accessToken, String userId, String userEmail) {
        saveSession(accessToken, refreshToken, userId, userEmail);
    }

    public void saveSession(String accessToken, String refreshToken, String userId, String userEmail) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.userId = userId;
        this.userEmail = userEmail;
        SharedPreferences.Editor editor = preferences.edit()
                .putString(KEY_ACCESS_TOKEN, accessToken)
                .putString(KEY_USER_ID, userId)
                .putString(KEY_USER_EMAIL, userEmail);
        if (refreshToken != null) {
            editor.putString(KEY_REFRESH_TOKEN, refreshToken);
        }
        editor.apply();
    }

    public void updateAccessToken(String accessToken, @Nullable String refreshToken) {
        this.accessToken = accessToken;
        SharedPreferences.Editor editor = preferences.edit().putString(KEY_ACCESS_TOKEN, accessToken);
        if (refreshToken != null) {
            this.refreshToken = refreshToken;
            editor.putString(KEY_REFRESH_TOKEN, refreshToken);
        }
        editor.apply();
    }

    public void clearSession() {
        accessToken = null;
        refreshToken = null;
        userId = null;
        userEmail = null;
        preferences.edit().clear().apply();
    }

    @Nullable
    public String getAccessToken() {
        return accessToken;
    }

    @Nullable
    public String getRefreshToken() {
        return refreshToken;
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
