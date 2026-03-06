package com.example.jvbench.data.mapper;

import com.example.jvbench.data.remote.supabase.dto.ReviewDto;
import com.example.jvbench.domain.model.Review;

import org.json.JSONException;
import org.json.JSONObject;

public final class ReviewMapper {
    private ReviewMapper() {
    }

    public static ReviewDto fromJson(JSONObject json) throws JSONException {
        ReviewDto dto = new ReviewDto();
        dto.id = json.getString("id");
        dto.benchId = json.getString("bench_id");
        dto.userId = json.getString("user_id");
        dto.rating = json.getInt("rating");
        dto.comment = json.optString("comment", "");
        dto.createdAt = json.optLong("created_at", System.currentTimeMillis());
        return dto;
    }

    public static Review toDomain(ReviewDto dto) {
        return new Review(
                dto.id,
                dto.benchId,
                dto.userId,
                dto.rating,
                dto.comment,
                dto.createdAt
        );
    }

    public static JSONObject toInsertJson(Review review) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("bench_id", review.getBenchId());
        json.put("user_id", review.getUserId());
        json.put("rating", review.getRating());
        json.put("comment", review.getComment());
        return json;
    }
}
