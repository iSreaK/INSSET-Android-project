package com.example.jvbench.data.repository;

import com.example.jvbench.core.common.ResultCallback;
import com.example.jvbench.data.remote.supabase.SupabaseClientProvider;
import com.example.jvbench.domain.model.Bench;
import com.example.jvbench.domain.repository.BenchRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SupabaseBenchRepository implements BenchRepository {
    private final SupabaseClientProvider clientProvider;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final List<Bench> localCache = new ArrayList<>();

    public SupabaseBenchRepository(SupabaseClientProvider clientProvider) {
        this.clientProvider = clientProvider;
        localCache.add(new Bench(
                "bench-demo-1",
                "Bench near campus",
                "Sample bench for initial map marker.",
                49.8941,
                2.2958,
                "",
                "demo-author",
                System.currentTimeMillis(),
                0.0,
                0
        ));
    }

    @Override
    public void getBenches(ResultCallback<List<Bench>> callback) {
        executor.execute(() -> {
            // TODO: Replace local cache with Supabase benches query.
            callback.onSuccess(new ArrayList<>(localCache));
        });
    }

    @Override
    public void getBenchById(String id, ResultCallback<Bench> callback) {
        executor.execute(() -> {
            for (Bench bench : localCache) {
                if (bench.getId().equals(id)) {
                    callback.onSuccess(bench);
                    return;
                }
            }
            callback.onError("Bench not found.");
        });
    }

    @Override
    public void createBench(Bench bench, ResultCallback<Void> callback) {
        executor.execute(() -> {
            if (bench == null) {
                callback.onError("Bench is null.");
                return;
            }
            // TODO: Send bench insert to Supabase.
            localCache.add(bench);
            callback.onSuccess(null);
        });
    }
}
