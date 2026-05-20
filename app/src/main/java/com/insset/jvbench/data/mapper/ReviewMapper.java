package com.insset.jvbench.data.mapper;

import com.insset.jvbench.data.remote.supabase.dto.ReviewDto;
import com.insset.jvbench.domain.model.Review;

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
        JSONObject author = json.optJSONObject("author");
        if (author != null) {
            dto.authorUsername = author.optString("username", null);
        }
        return dto;
    }

    public static Review toDomain(ReviewDto dto) {
        return new Review(
                dto.id,
                dto.benchId,
                dto.userId,
                dto.rating,
                dto.comment,
                dto.createdAt,
                dto.authorUsername
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
