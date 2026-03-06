package com.example.jvbench.domain.model;

public class Review {
    private final String id;
    private final String benchId;
    private final String userId;
    private final int rating;
    private final String comment;
    private final long createdAt;

    public Review(String id, String benchId, String userId, int rating, String comment, long createdAt) {
        this.id = id;
        this.benchId = benchId;
        this.userId = userId;
        this.rating = rating;
        this.comment = comment;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public String getBenchId() { return benchId; }
    public String getUserId() { return userId; }
    public int getRating() { return rating; }
    public String getComment() { return comment; }
    public long getCreatedAt() { return createdAt; }
}
