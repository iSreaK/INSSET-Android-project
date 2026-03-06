package com.example.jvbench.ui.account;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.jvbench.core.common.ResultCallback;
import com.example.jvbench.domain.model.User;
import com.example.jvbench.domain.repository.AuthRepository;

public class AccountViewModel extends ViewModel {
    public static class UiState {
        public final boolean loading;
        public final User user;
        public final String message;

        public UiState(boolean loading, User user, String message) {
            this.loading = loading;
            this.user = user;
            this.message = message;
        }
    }

    private final AuthRepository authRepository;
    private final MutableLiveData<UiState> uiState = new MutableLiveData<>(new UiState(true, null, null));

    public AccountViewModel(AuthRepository authRepository) {
        this.authRepository = authRepository;
    }

    public LiveData<UiState> getUiState() {
        return uiState;
    }

    public void loadAccount() {
        uiState.postValue(new UiState(true, null, null));
        authRepository.loadCurrentUser(new ResultCallback<User>() {
            @Override
            public void onSuccess(User result) {
                uiState.postValue(new UiState(false, result, null));
            }

            @Override
            public void onError(String errorMessage) {
                uiState.postValue(new UiState(false, null, errorMessage));
            }
        });
    }

    public void signOut(ResultCallback<Void> callback) {
        authRepository.signOut(callback);
    }
}
