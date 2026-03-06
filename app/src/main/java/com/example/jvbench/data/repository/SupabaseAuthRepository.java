package com.example.jvbench.data.repository;

import com.example.jvbench.core.common.AuthCallback;
import com.example.jvbench.core.common.ResultCallback;
import com.example.jvbench.data.remote.supabase.SupabaseClientProvider;
import com.example.jvbench.domain.model.User;
import com.example.jvbench.domain.model.UserRole;
import com.example.jvbench.domain.repository.AuthRepository;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SupabaseAuthRepository implements AuthRepository {
    private final SupabaseClientProvider clientProvider;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private User currentUser;

    public SupabaseAuthRepository(SupabaseClientProvider clientProvider) {
        this.clientProvider = clientProvider;
    }

    @Override
    public void signIn(String email, String password, AuthCallback callback) {
        executor.execute(() -> {
            if (email == null || email.isBlank() || password == null || password.isBlank()) {
                callback.onError("Email and password are required.");
                return;
            }
            // TODO: Connect to Supabase Auth sign in endpoint.
            currentUser = new User(UUID.randomUUID().toString(), email, UserRole.USER);
            callback.onSuccess(currentUser);
        });
    }

    @Override
    public void signUp(String email, String password, AuthCallback callback) {
        executor.execute(() -> {
            if (email == null || email.isBlank() || password == null || password.length() < 6) {
                callback.onError("Invalid sign up payload.");
                return;
            }
            // TODO: Connect to Supabase Auth sign up endpoint.
            currentUser = new User(UUID.randomUUID().toString(), email, UserRole.USER);
            callback.onSuccess(currentUser);
        });
    }

    @Override
    public void signOut(ResultCallback<Void> callback) {
        executor.execute(() -> {
            // TODO: Connect to Supabase sign out endpoint.
            currentUser = null;
            callback.onSuccess(null);
        });
    }

    @Override
    public User getCurrentUser() {
        return currentUser;
    }
}
