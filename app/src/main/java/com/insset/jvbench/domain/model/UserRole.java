package com.insset.jvbench.domain.model;

public enum UserRole {
    USER,
    MODERATOR,
    ADMINISTRATOR;

    public boolean canModerate() {
        return this == MODERATOR || this == ADMINISTRATOR;
    }

    public boolean isAdmin() {
        return this == ADMINISTRATOR;
    }
}
