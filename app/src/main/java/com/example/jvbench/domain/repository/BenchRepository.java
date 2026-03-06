package com.example.jvbench.domain.repository;

import com.example.jvbench.core.common.ResultCallback;
import com.example.jvbench.domain.model.Bench;

import java.util.List;

public interface BenchRepository {
    void getBenches(ResultCallback<List<Bench>> callback);

    void getBenchById(String id, ResultCallback<Bench> callback);

    void createBench(Bench bench, ResultCallback<Void> callback);

    void updateBench(Bench bench, ResultCallback<Void> callback);

    void deleteBench(String benchId, ResultCallback<Void> callback);
}
