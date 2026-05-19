package com.example.jvbench.data.repository;

import com.example.jvbench.core.common.ResultCallback;
import com.example.jvbench.data.remote.supabase.BenchImageStorageHelper;
import com.example.jvbench.data.remote.supabase.SupabaseApiClient;
import com.example.jvbench.data.remote.supabase.SupabaseClientProvider;
import com.example.jvbench.data.remote.supabase.SupabaseResponse;
import com.example.jvbench.domain.repository.StorageRepository;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SupabaseStorageRepository implements StorageRepository {
    private final SupabaseClientProvider clientProvider;
    private final SupabaseApiClient apiClient;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public SupabaseStorageRepository(SupabaseClientProvider clientProvider, SupabaseApiClient apiClient) {
        this.clientProvider = clientProvider;
        this.apiClient = apiClient;
    }

    @Override
    public void uploadBenchImage(String benchId, byte[] bytes, String mimeType, ResultCallback<String> callback) {
        executor.execute(() -> {
            if (benchId == null || benchId.isBlank() || bytes == null || bytes.length == 0) {
                callback.onError("Image invalide.");
                return;
            }
            String extension = guessExtension(mimeType);
            String objectPath = BenchImageStorageHelper.buildObjectPath(benchId, extension);
            String url = clientProvider.getStorageBaseUrl()
                    + "/object/" + BenchImageStorageHelper.BUCKET_NAME + "/" + objectPath;
            String safeMime = mimeType != null && !mimeType.isBlank() ? mimeType : "image/jpeg";
            SupabaseResponse response = apiClient.uploadBytes(url, bytes, safeMime, true);
            if (!response.isSuccessful()) {
                callback.onError(response.getError());
                return;
            }
            callback.onSuccess(BenchImageStorageHelper.buildPublicUrl(clientProvider, objectPath));
        });
    }

    private static String guessExtension(String mimeType) {
        if (mimeType == null) {
            return "jpg";
        }
        switch (mimeType.toLowerCase()) {
            case "image/png":
                return "png";
            case "image/webp":
                return "webp";
            case "image/gif":
                return "gif";
            case "image/jpg":
            case "image/jpeg":
            default:
                return "jpg";
        }
    }
}
