package com.example.jvbench.data.repository;

import com.example.jvbench.core.common.ResultCallback;
import com.example.jvbench.data.mapper.BenchMapper;
import com.example.jvbench.data.remote.supabase.SupabaseApiClient;
import com.example.jvbench.data.remote.supabase.SupabaseClientProvider;
import com.example.jvbench.data.remote.supabase.SupabaseResponse;
import com.example.jvbench.domain.model.Bench;
import com.example.jvbench.domain.repository.BenchRepository;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SupabaseBenchRepository implements BenchRepository {
    private final SupabaseClientProvider clientProvider;
    private final SupabaseApiClient apiClient;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public SupabaseBenchRepository(SupabaseClientProvider clientProvider, SupabaseApiClient apiClient) {
        this.clientProvider = clientProvider;
        this.apiClient = apiClient;
    }

    @Override
    public void getBenches(ResultCallback<List<Bench>> callback) {
        executor.execute(() -> {
            String url = clientProvider.getRestBaseUrl()
                    + "/benches?select=id,name,description,latitude,longitude,image_url,author_id,average_rating,review_count,created_at"
                    + "&order=created_at.desc";
            SupabaseResponse response = apiClient.get(url, false);
            if (!response.isSuccessful()) {
                callback.onError(response.getError());
                return;
            }
            try {
                JSONArray array = new JSONArray(response.getBody());
                List<Bench> benches = new ArrayList<>();
                for (int index = 0; index < array.length(); index++) {
                    benches.add(BenchMapper.toDomain(BenchMapper.fromJson(array.getJSONObject(index))));
                }
                callback.onSuccess(benches);
            } catch (JSONException exception) {
                callback.onError(exception.getMessage());
            }
        });
    }

    @Override
    public void getBenchById(String id, ResultCallback<Bench> callback) {
        executor.execute(() -> {
            String url = clientProvider.getRestBaseUrl()
                    + "/benches?select=id,name,description,latitude,longitude,image_url,author_id,average_rating,review_count,created_at"
                    + "&id=eq." + id
                    + "&limit=1";
            SupabaseResponse response = apiClient.get(url, false);
            if (!response.isSuccessful()) {
                callback.onError(response.getError());
                return;
            }
            try {
                JSONArray array = new JSONArray(response.getBody());
                if (array.length() == 0) {
                    callback.onError("Bench not found.");
                    return;
                }
                callback.onSuccess(BenchMapper.toDomain(BenchMapper.fromJson(array.getJSONObject(0))));
            } catch (JSONException exception) {
                callback.onError(exception.getMessage());
            }
        });
    }

    @Override
    public void createBench(Bench bench, ResultCallback<Void> callback) {
        executor.execute(() -> {
            if (bench == null) {
                callback.onError("Bench is null.");
                return;
            }
            try {
                JSONObject payload = BenchMapper.toInsertJson(bench);
                SupabaseResponse response = apiClient.post(
                        clientProvider.getRestBaseUrl() + "/benches",
                        payload,
                        true,
                        "return=minimal"
                );
                if (!response.isSuccessful()) {
                    callback.onError(response.getError());
                    return;
                }
                callback.onSuccess(null);
            } catch (JSONException exception) {
                callback.onError(exception.getMessage());
            }
        });
    }

    @Override
    public void updateBench(Bench bench, ResultCallback<Void> callback) {
        executor.execute(() -> {
            if (bench == null || bench.getId() == null || bench.getId().isBlank()) {
                callback.onError("Bench id is required.");
                return;
            }
            try {
                JSONObject payload = new JSONObject()
                        .put("name", bench.getName())
                        .put("description", bench.getDescription())
                        .put("latitude", bench.getLatitude())
                        .put("longitude", bench.getLongitude())
                        .put("image_url", bench.getImageUrl());
                String url = clientProvider.getRestBaseUrl() + "/benches?id=eq." + bench.getId();
                SupabaseResponse response = apiClient.patch(url, payload, true, "return=minimal");
                if (!response.isSuccessful()) {
                    callback.onError(response.getError());
                    return;
                }
                callback.onSuccess(null);
            } catch (JSONException exception) {
                callback.onError(exception.getMessage());
            }
        });
    }

    @Override
    public void deleteBench(String benchId, ResultCallback<Void> callback) {
        executor.execute(() -> {
            if (benchId == null || benchId.isBlank()) {
                callback.onError("Bench id is required.");
                return;
            }
            String url = clientProvider.getRestBaseUrl() + "/benches?id=eq." + benchId;
            SupabaseResponse response = apiClient.delete(url, true);
            if (!response.isSuccessful()) {
                callback.onError(response.getError());
                return;
            }
            callback.onSuccess(null);
        });
    }
}
