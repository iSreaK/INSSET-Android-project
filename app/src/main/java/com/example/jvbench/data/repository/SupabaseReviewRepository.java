package com.example.jvbench.data.repository;

import com.example.jvbench.core.common.ResultCallback;
import com.example.jvbench.data.mapper.ReviewMapper;
import com.example.jvbench.data.remote.supabase.SupabaseApiClient;
import com.example.jvbench.data.remote.supabase.SupabaseClientProvider;
import com.example.jvbench.data.remote.supabase.SupabaseResponse;
import com.example.jvbench.data.remote.supabase.dto.ReviewDto;
import com.example.jvbench.domain.model.Review;
import com.example.jvbench.domain.repository.ReviewRepository;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
            // Step 1: fetch reviews
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
                List<ReviewDto> dtos = new ArrayList<>();
                List<String> userIds = new ArrayList<>();

                for (int i = 0; i < array.length(); i++) {
                    ReviewDto dto = ReviewMapper.fromJson(array.getJSONObject(i));
                    dtos.add(dto);
                    if (!userIds.contains(dto.userId)) {
                        userIds.add(dto.userId);
                    }
                }

                // Step 2: fetch usernames in one batch query
                Map<String, String> usernameMap = fetchUsernames(userIds);

                // Step 3: build final review list with usernames
                List<Review> reviews = new ArrayList<>();
                for (ReviewDto dto : dtos) {
                    dto.username = usernameMap.getOrDefault(dto.userId, "Utilisateur");
                    reviews.add(ReviewMapper.toDomain(dto));
                }
                callback.onSuccess(reviews);
            } catch (JSONException exception) {
                callback.onError(exception.getMessage());
            }
        });
    }

    private Map<String, String> fetchUsernames(List<String> userIds) {
        Map<String, String> map = new HashMap<>();
        if (userIds.isEmpty()) return map;

        StringBuilder inClause = new StringBuilder("(");
        for (int i = 0; i < userIds.size(); i++) {
            if (i > 0) inClause.append(",");
            inClause.append(userIds.get(i));
        }
        inClause.append(")");

        String url = clientProvider.getRestBaseUrl()
                + "/profiles?select=id,username&id=in." + inClause;
        SupabaseResponse response = apiClient.get(url, true);
        if (!response.isSuccessful()) return map;

        try {
            JSONArray profiles = new JSONArray(response.getBody());
            for (int i = 0; i < profiles.length(); i++) {
                JSONObject p = profiles.getJSONObject(i);
                map.put(p.getString("id"), p.optString("username", "Utilisateur"));
            }
        } catch (JSONException ignored) {}
        return map;
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
