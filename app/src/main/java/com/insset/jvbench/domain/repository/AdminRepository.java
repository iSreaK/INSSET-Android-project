package com.insset.jvbench.domain.repository;

import com.insset.jvbench.core.common.ResultCallback;
import com.insset.jvbench.domain.model.User;
import com.insset.jvbench.domain.model.UserRole;

import java.util.List;

public interface AdminRepository {
    void listUsers(ResultCallback<List<User>> callback);

    void changeUserRole(String userId, UserRole newRole, ResultCallback<Void> callback);

    void deleteUser(String userId, ResultCallback<Void> callback);
}
