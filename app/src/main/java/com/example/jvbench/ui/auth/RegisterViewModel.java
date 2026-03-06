package com.example.jvbench.ui.auth;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.jvbench.domain.repository.AuthRepository;

public class RegisterViewModel extends ViewModel {
    public enum Status {
        IDLE,
        LOADING,
        SUCCESS,
        ERROR
    }

    public static class UiState {
        public final Status status;
        public final String message;

        public UiState(Status status, String message) {
            this.status = status;
            this.message = message;
        }
    }

    private final AuthRepository authRepository;
    private final MutableLiveData<UiState> uiState = new MutableLiveData<>(new UiState(Status.IDLE, null));

    public RegisterViewModel(AuthRepository authRepository) {
        this.authRepository = authRepository;
    }

    public LiveData<UiState> getUiState() {
        return uiState;
    }

    public void register(String email, String username, String password, Runnable onSuccess) {
        uiState.postValue(new UiState(Status.LOADING, null));
        authRepository.signUp(email, username, password, new com.example.jvbench.core.common.AuthCallback() {
            @Override
            public void onSuccess(com.example.jvbench.domain.model.User result) {
                uiState.postValue(new UiState(Status.SUCCESS, "Compte cree avec succes."));
                if (onSuccess != null) {
                    onSuccess.run();
                }
            }

            @Override
            public void onError(String errorMessage) {
                uiState.postValue(new UiState(Status.ERROR, errorMessage));
            }
        });
    }
}
