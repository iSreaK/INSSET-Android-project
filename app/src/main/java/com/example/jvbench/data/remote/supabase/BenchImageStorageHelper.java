package com.example.jvbench.data.remote.supabase;

public final class BenchImageStorageHelper {
    public static final String BUCKET_NAME = "bench-images";

    private BenchImageStorageHelper() {
    }

    public static String buildObjectPath(String benchId, String extension) {
        String safeExtension = extension == null || extension.isBlank() ? "jpg" : extension;
        return benchId + "/main." + safeExtension;
    }

    public static String buildPublicUrl(SupabaseClientProvider provider, String objectPath) {
        return provider.getStorageBaseUrl() + "/object/public/" + BUCKET_NAME + "/" + objectPath;
    }
}
