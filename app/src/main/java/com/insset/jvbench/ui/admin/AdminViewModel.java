package com.insset.jvbench.ui.admin;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.insset.jvbench.core.common.ResultCallback;
import com.insset.jvbench.domain.model.User;
import com.insset.jvbench.domain.model.UserRole;
import com.insset.jvbench.domain.repository.AdminRepository;

import java.util.Collections;
import java.util.List;

public class AdminViewModel extends ViewModel {
    public static class UiState {
        public final boolean loading;
        public final List<User> users;
        public final String error;
        public final String message;

        public UiState(boolean loading, List<User> users, String error, String message) {
            this.loading = loading;
            this.users = users;
            this.error = error;
            this.message = message;
        }
    }

    private final AdminRepository adminRepository;
    private final MutableLiveData<UiState> state = new MutableLiveData<>(new UiState(false, Collections.emptyList(), null, null));

    public AdminViewModel(AdminRepository adminRepository) {
        this.adminRepository = adminRepository;
    }

    public LiveData<UiState> getState() {
        return state;
    }

    public void load() {
        state.postValue(new UiState(true, current(), null, null));
        adminRepository.listUsers(new ResultCallback<List<User>>() {
            @Override
            public void onSuccess(List<User> result) {
                state.postValue(new UiState(false, result, null, null));
            }

            @Override
            public void onError(String errorMessage) {
                state.postValue(new UiState(false, current(), errorMessage, null));
            }
        });
    }

    public void changeRole(String userId, UserRole newRole) {
        state.postValue(new UiState(true, current(), null, null));
        adminRepository.changeUserRole(userId, newRole, new ResultCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                load();
            }

            @Override
            public void onError(String errorMessage) {
                state.postValue(new UiState(false, current(), errorMessage, null));
            }
        });
    }

    public void deleteUser(String userId) {
        state.postValue(new UiState(true, current(), null, null));
        adminRepository.deleteUser(userId, new ResultCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                load();
            }

            @Override
            public void onError(String errorMessage) {
                state.postValue(new UiState(false, current(), errorMessage, null));
            }
        });
    }

    private List<User> current() {
        UiState s = state.getValue();
        return s != null && s.users != null ? s.users : Collections.emptyList();
    }
}
