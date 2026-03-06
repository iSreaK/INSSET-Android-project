package com.example.jvbench.domain.model;

public class User {
    private final String id;
    private final String email;
    private final UserRole role;

    public User(String id, String email, UserRole role) {
        this.id = id;
        this.email = email;
        this.role = role;
    }

    public String getId() { return id; }
    public String getEmail() { return email; }
    public UserRole getRole() { return role; }
}
