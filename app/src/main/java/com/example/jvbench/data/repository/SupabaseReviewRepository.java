package com.example.jvbench.data.repository;

import com.example.jvbench.core.common.ResultCallback;
import com.example.jvbench.data.mapper.ReviewMapper;
import com.example.jvbench.data.remote.supabase.SupabaseApiClient;
import com.example.jvbench.data.remote.supabase.SupabaseClientProvider;
import com.example.jvbench.data.remote.supabase.SupabaseResponse;
import com.example.jvbench.domain.model.Review;
import com.example.jvbench.domain.repository.ReviewRepository;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SupabaseReviewRepository implements ReviewRepository {
    private final SupabaseClientProvider clientProvider;
    private final SupabaseApiClient apiClient;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public SupabaseReviewRepository(SupabaseClientProvider clientProvider, SupabaseApiClient apiClient) {
        this.clientProvider = clientProvider;
        this.apiClient = apiClient;
    }

    @Override
    public void getReviewsForBench(String benchId, ResultCallback<List<Review>> callback) {
        executor.execute(() -> {
            String url = clientProvider.getRestBaseUrl()
                    + "/reviews?select=id,bench_id,user_id,rating,comment,created_at"
                    + "&bench_id=eq." + benchId
                    + "&order=created_at.desc";
            SupabaseResponse response = apiClient.get(url, false);
            if (!response.isSuccessful()) {
                callback.onError(response.getError());
                return;
            }
            try {
                JSONArray array = new JSONArray(response.getBody());
                List<Review> reviews = new ArrayList<>();
                for (int index = 0; index < array.length(); index++) {
                    reviews.add(ReviewMapper.toDomain(ReviewMapper.fromJson(array.getJSONObject(index))));
                }
                callback.onSuccess(reviews);
            } catch (JSONException exception) {
                callback.onError(exception.getMessage());
            }
        });
    }

    @Override
    public void addReview(Review review, ResultCallback<Void> callback) {
        executor.execute(() -> {
            if (review == null) {
                callback.onError("Review is null.");
                return;
            }
            try {
                JSONObject payload = ReviewMapper.toInsertJson(review);
                SupabaseResponse response = apiClient.post(
                        clientProvider.getRestBaseUrl() + "/reviews",
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
    public void updateReview(Review review, ResultCallback<Void> callback) {
        executor.execute(() -> {
            if (review == null || review.getId() == null || review.getId().isBlank()) {
                callback.onError("Review id is required.");
                return;
            }
            try {
                JSONObject payload = new JSONObject()
                        .put("rating", review.getRating())
                        .put("comment", review.getComment());
                String url = clientProvider.getRestBaseUrl() + "/reviews?id=eq." + review.getId();
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
    public void deleteReview(String reviewId, ResultCallback<Void> callback) {
        executor.execute(() -> {
            if (reviewId == null || reviewId.isBlank()) {
                callback.onError("Review id is required.");
                return;
            }
            String url = clientProvider.getRestBaseUrl() + "/reviews?id=eq." + reviewId;
            SupabaseResponse response = apiClient.delete(url, true);
            if (!response.isSuccessful()) {
                callback.onError(response.getError());
                return;
            }
            callback.onSuccess(null);
        });
    }
}
