package com.example.jvbench.domain.repository;

import com.example.jvbench.core.common.AuthCallback;
import com.example.jvbench.core.common.ResultCallback;
import com.example.jvbench.domain.model.User;

public interface AuthRepository {
    void signIn(String email, String password, AuthCallback callback);

    void signUp(String email, String username, String password, AuthCallback callback);

    void signOut(ResultCallback<Void> callback);

    User getCurrentUser();

    void loadCurrentUser(ResultCallback<User> callback);

    void updateUsername(String username, ResultCallback<User> callback);
}
