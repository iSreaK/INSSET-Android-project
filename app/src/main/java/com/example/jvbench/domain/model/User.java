package com.example.jvbench.domain.model;

public class User {
    private final String id;
    private final String email;
    private final String username;
    private final UserRole role;
    private final String createdAt;

    public User(String id, String email, String username, UserRole role, String createdAt) {
        this.id = id;
        this.email = email;
        this.username = username;
        this.role = role;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public String getEmail() { return email; }
    public String getUsername() { return username; }
    public UserRole getRole() { return role; }
    public String getCreatedAt() { return createdAt; }
}
