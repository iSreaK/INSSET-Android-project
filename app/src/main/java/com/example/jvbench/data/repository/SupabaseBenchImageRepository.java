package com.example.jvbench.data.repository;

import com.example.jvbench.core.common.ResultCallback;
import com.example.jvbench.data.remote.supabase.BenchImageStorageHelper;
import com.example.jvbench.data.remote.supabase.SupabaseApiClient;
import com.example.jvbench.data.remote.supabase.SupabaseClientProvider;
import com.example.jvbench.data.remote.supabase.SupabaseResponse;
import com.example.jvbench.domain.repository.BenchImageRepository;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SupabaseBenchImageRepository implements BenchImageRepository {
    private final SupabaseClientProvider clientProvider;
    private final SupabaseApiClient apiClient;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public SupabaseBenchImageRepository(SupabaseClientProvider clientProvider, SupabaseApiClient apiClient) {
        this.clientProvider = clientProvider;
        this.apiClient = apiClient;
    }

    @Override
    public void uploadBenchImage(String benchId, byte[] bytes, String mimeType, ResultCallback<String> callback) {
        executor.execute(() -> {
            if (benchId == null || benchId.isBlank()) {
                callback.onError("Bench id is required.");
                return;
            }
            if (bytes == null || bytes.length == 0) {
                callback.onError("Image is empty.");
                return;
            }
            String extension = extensionFromMime(mimeType);
            String objectPath = BenchImageStorageHelper.buildObjectPath(benchId, extension);
            String url = clientProvider.getStorageBaseUrl() + "/object/" + BenchImageStorageHelper.BUCKET_NAME + "/" + objectPath;
            SupabaseResponse response = apiClient.uploadBinary(url, bytes, mimeType, true, true);
            if (!response.isSuccessful()) {
                callback.onError(response.getError());
                return;
            }
            callback.onSuccess(BenchImageStorageHelper.buildPublicUrl(clientProvider, objectPath));
        });
    }

    @Override
    public void deleteBenchImage(String benchId, String extension, ResultCallback<Void> callback) {
        executor.execute(() -> {
            if (benchId == null || benchId.isBlank()) {
                callback.onError("Bench id is required.");
                return;
            }
            String objectPath = BenchImageStorageHelper.buildObjectPath(benchId, extension);
            String url = clientProvider.getStorageBaseUrl() + "/object/" + BenchImageStorageHelper.BUCKET_NAME + "/" + objectPath;
            SupabaseResponse response = apiClient.delete(url, true);
            if (!response.isSuccessful()) {
                callback.onError(response.getError());
                return;
            }
            callback.onSuccess(null);
        });
    }

    private static String extensionFromMime(String mimeType) {
        if (mimeType == null) {
            return "jpg";
        }
        String lower = mimeType.toLowerCase();
        if (lower.contains("png")) return "png";
        if (lower.contains("webp")) return "webp";
        if (lower.contains("gif")) return "gif";
        return "jpg";
    }
}
