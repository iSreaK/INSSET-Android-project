package com.example.jvbench.domain.repository;

import com.example.jvbench.core.common.ResultCallback;

public interface StorageRepository {
    void uploadBenchImage(String benchId, byte[] bytes, String mimeType, ResultCallback<String> callback);
}
