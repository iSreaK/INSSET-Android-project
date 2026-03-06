package com.example.jvbench.data.remote.supabase;

public class SupabaseResponse {
    private final boolean successful;
    private final int code;
    private final String body;
    private final String error;

    private SupabaseResponse(boolean successful, int code, String body, String error) {
        this.successful = successful;
        this.code = code;
        this.body = body;
        this.error = error;
    }

    public static SupabaseResponse success(int code, String body) {
        return new SupabaseResponse(true, code, body, null);
    }

    public static SupabaseResponse failure(int code, String body, String error) {
        return new SupabaseResponse(false, code, body, error);
    }

    public boolean isSuccessful() {
        return successful;
    }

    public int getCode() {
        return code;
    }

    public String getBody() {
        return body;
    }

    public String getError() {
        return error;
    }
}
