package com.example.jvbench.data.repository;

import com.example.jvbench.core.common.ResultCallback;
import com.example.jvbench.data.remote.supabase.SupabaseClientProvider;
import com.example.jvbench.domain.model.Review;
import com.example.jvbench.domain.repository.ReviewRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SupabaseReviewRepository implements ReviewRepository {
    private final SupabaseClientProvider clientProvider;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final List<Review> localCache = new ArrayList<>();

    public SupabaseReviewRepository(SupabaseClientProvider clientProvider) {
        this.clientProvider = clientProvider;
    }

    @Override
    public void getReviewsForBench(String benchId, ResultCallback<List<Review>> callback) {
        executor.execute(() -> {
            List<Review> reviews = new ArrayList<>();
            for (Review review : localCache) {
                if (review.getBenchId().equals(benchId)) {
                    reviews.add(review);
                }
            }
            // TODO: Replace with Supabase query for bench reviews.
            callback.onSuccess(reviews);
        });
    }

    @Override
    public void addReview(Review review, ResultCallback<Void> callback) {
        executor.execute(() -> {
            if (review == null) {
                callback.onError("Review is null.");
                return;
            }
            // TODO: Send review insert to Supabase and update bench aggregates.
            localCache.add(review);
            callback.onSuccess(null);
        });
    }
}
