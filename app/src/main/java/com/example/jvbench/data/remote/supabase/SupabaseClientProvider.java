package com.example.jvbench.data.remote.supabase;

import com.example.jvbench.BuildConfig;

import okhttp3.OkHttpClient;

public class SupabaseClientProvider {
    private final OkHttpClient httpClient;
    private final String baseUrl;
    private final String anonKey;

    public SupabaseClientProvider() {
        this.httpClient = new OkHttpClient();
        this.baseUrl = BuildConfig.SUPABASE_URL;
        this.anonKey = BuildConfig.SUPABASE_ANON_KEY;
    }

    public OkHttpClient getHttpClient() { return httpClient; }
    public String getBaseUrl() { return baseUrl; }
    public String getAnonKey() { return anonKey; }

    public boolean isConfigured() {
        return !"TODO_SUPABASE_URL".equals(baseUrl) && !"TODO_SUPABASE_ANON_KEY".equals(anonKey);
    }
}
