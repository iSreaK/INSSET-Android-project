package com.insset.jvbench.domain.repository;

import com.insset.jvbench.core.common.AuthCallback;
import com.insset.jvbench.core.common.ResultCallback;
import com.insset.jvbench.domain.model.User;

public interface AuthRepository {
    void signIn(String email, String password, AuthCallback callback);

    void signUp(String email, String username, String password, AuthCallback callback);

    void signOut(ResultCallback<Void> callback);

    User getCurrentUser();

    void loadCurrentUser(ResultCallback<User> callback);

    void updateUsername(String username, ResultCallback<User> callback);

    void getUserById(String userId, ResultCallback<User> callback);
}
