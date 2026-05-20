package com.insset.jvbench.domain.model;

public class Review {
    private final String id;
    private final String benchId;
    private final String userId;
    private final int rating;
    private final String comment;
    private final long createdAt;
    private final String authorUsername;

    public Review(String id, String benchId, String userId, int rating, String comment, long createdAt) {
        this(id, benchId, userId, rating, comment, createdAt, null);
    }

    public Review(String id, String benchId, String userId, int rating, String comment, long createdAt, String authorUsername) {
        this.id = id;
        this.benchId = benchId;
        this.userId = userId;
        this.rating = rating;
        this.comment = comment;
        this.createdAt = createdAt;
        this.authorUsername = authorUsername;
    }

    public String getId() { return id; }
    public String getBenchId() { return benchId; }
    public String getUserId() { return userId; }
    public int getRating() { return rating; }
    public String getComment() { return comment; }
    public long getCreatedAt() { return createdAt; }
    public String getAuthorUsername() { return authorUsername; }
}
