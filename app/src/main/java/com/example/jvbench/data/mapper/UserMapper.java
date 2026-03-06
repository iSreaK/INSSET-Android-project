package com.example.jvbench.data.mapper;

import com.example.jvbench.domain.model.User;
import com.example.jvbench.domain.model.UserRole;

import org.json.JSONException;
import org.json.JSONObject;

public final class UserMapper {
    private UserMapper() {
    }

    public static User fromProfileJson(JSONObject json) throws JSONException {
        String id = json.getString("id");
        String email = json.getString("email");
        String username = json.optString("username", "");
        String roleRaw = json.optString("role", "USER");
        String createdAt = json.optString("created_at", null);
        UserRole role = "ADMIN".equalsIgnoreCase(roleRaw) ? UserRole.ADMIN : UserRole.USER;
        return new User(id, email, username, role, createdAt);
    }
}
