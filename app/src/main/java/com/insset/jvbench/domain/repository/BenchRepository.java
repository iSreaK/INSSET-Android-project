package com.insset.jvbench.domain.repository;

import com.insset.jvbench.core.common.ResultCallback;
import com.insset.jvbench.domain.model.Bench;

import java.util.List;

public interface BenchRepository {
    void getBenches(ResultCallback<List<Bench>> callback);

    void getBenchesByAuthor(String authorId, ResultCallback<List<Bench>> callback);

    void getBenchById(String id, ResultCallback<Bench> callback);

    void createBench(Bench bench, ResultCallback<Void> callback);

    void updateBench(Bench bench, ResultCallback<Void> callback);

    void deleteBench(String benchId, ResultCallback<Void> callback);
}
