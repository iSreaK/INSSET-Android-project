package com.example.jvbench.data.mapper;

import com.example.jvbench.data.remote.supabase.dto.BenchDto;
import com.example.jvbench.domain.model.Bench;

import org.json.JSONException;
import org.json.JSONObject;

public final class BenchMapper {
    private BenchMapper() {
    }

    public static BenchDto fromJson(JSONObject json) throws JSONException {
        BenchDto dto = new BenchDto();
        dto.id = json.getString("id");
        dto.name = json.getString("name");
        dto.description = json.optString("description", "");
        dto.latitude = json.getDouble("latitude");
        dto.longitude = json.getDouble("longitude");
        dto.imageUrl = json.optString("image_url", "");
        dto.authorId = json.getString("author_id");
        dto.averageRating = json.optDouble("average_rating", 0.0);
        dto.reviewCount = json.optInt("review_count", 0);
        dto.createdAt = json.optLong("created_at", System.currentTimeMillis());
        return dto;
    }

    public static Bench toDomain(BenchDto dto) {
        return new Bench(
                dto.id,
                dto.name,
                dto.description,
                dto.latitude,
                dto.longitude,
                dto.imageUrl,
                dto.authorId,
                dto.createdAt,
                dto.averageRating,
                dto.reviewCount
        );
    }

    public static JSONObject toInsertJson(Bench bench) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("name", bench.getName());
        json.put("description", bench.getDescription());
        json.put("latitude", bench.getLatitude());
        json.put("longitude", bench.getLongitude());
        json.put("image_url", bench.getImageUrl());
        json.put("author_id", bench.getAuthorId());
        return json;
    }
}
