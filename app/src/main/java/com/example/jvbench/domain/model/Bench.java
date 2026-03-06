package com.example.jvbench.domain.model;

public class Bench {
    private final String id;
    private final String name;
    private final String description;
    private final double latitude;
    private final double longitude;
    private final String imageUrl;
    private final String authorId;
    private final long createdAt;
    private final double averageRating;
    private final int reviewCount;

    public Bench(String id, String name, String description, double latitude, double longitude,
                 String imageUrl, String authorId, long createdAt, double averageRating, int reviewCount) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.latitude = latitude;
        this.longitude = longitude;
        this.imageUrl = imageUrl;
        this.authorId = authorId;
        this.createdAt = createdAt;
        this.averageRating = averageRating;
        this.reviewCount = reviewCount;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public String getImageUrl() { return imageUrl; }
    public String getAuthorId() { return authorId; }
    public long getCreatedAt() { return createdAt; }
    public double getAverageRating() { return averageRating; }
    public int getReviewCount() { return reviewCount; }
}
