package com.example.jvbench.ui.auth;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.jvbench.domain.repository.AuthRepository;

public class RegisterViewModel extends ViewModel {
    public static class UiState {
        public final boolean loading;
        public final String message;

        public UiState(boolean loading, String message) {
            this.loading = loading;
            this.message = message;
        }
    }

    private final AuthRepository authRepository;
    private final MutableLiveData<UiState> uiState = new MutableLiveData<>(new UiState(false, null));

    public RegisterViewModel(AuthRepository authRepository) {
        this.authRepository = authRepository;
    }

    public LiveData<UiState> getUiState() {
        return uiState;
    }

    public void register(String email, String password, Runnable onSuccess) {
        uiState.postValue(new UiState(true, null));
        authRepository.signUp(email, password, new com.example.jvbench.core.common.AuthCallback() {
            @Override
            public void onSuccess(com.example.jvbench.domain.model.User result) {
                uiState.postValue(new UiState(false, "Account created."));
                if (onSuccess != null) {
                    onSuccess.run();
                }
            }

            @Override
            public void onError(String errorMessage) {
                uiState.postValue(new UiState(false, errorMessage));
            }
        });
    }
}
