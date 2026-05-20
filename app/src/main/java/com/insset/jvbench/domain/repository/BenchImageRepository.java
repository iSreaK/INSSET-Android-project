package com.insset.jvbench.domain.repository;

import com.insset.jvbench.core.common.ResultCallback;

public interface BenchImageRepository {
    void uploadBenchImage(String benchId, byte[] bytes, String mimeType, ResultCallback<String> callback);

    void deleteBenchImage(String benchId, String extension, ResultCallback<Void> callback);
}
