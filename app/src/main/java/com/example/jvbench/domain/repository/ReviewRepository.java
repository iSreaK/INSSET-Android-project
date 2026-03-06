package com.example.jvbench.domain.repository;

import com.example.jvbench.core.common.ResultCallback;
import com.example.jvbench.domain.model.Review;

import java.util.List;

public interface ReviewRepository {
    void getReviewsForBench(String benchId, ResultCallback<List<Review>> callback);

    void addReview(Review review, ResultCallback<Void> callback);
}
