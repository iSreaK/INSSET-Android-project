package com.example.jvbench.ui.auth;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.jvbench.domain.repository.AuthRepository;

public class LoginViewModel extends ViewModel {
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

    public LoginViewModel(AuthRepository authRepository) {
        this.authRepository = authRepository;
    }

    public LiveData<UiState> getUiState() {
        return uiState;
    }

    public void restoreSession(Runnable onSuccess) {
        uiState.postValue(new UiState(Status.LOADING, null));
        authRepository.loadCurrentUser(new com.example.jvbench.core.common.ResultCallback<com.example.jvbench.domain.model.User>() {
            @Override
            public void onSuccess(com.example.jvbench.domain.model.User result) {
                uiState.postValue(new UiState(Status.SUCCESS, "Bon retour " + result.getEmail()));
                if (onSuccess != null) {
                    onSuccess.run();
                }
            }

            @Override
            public void onError(String errorMessage) {
                uiState.postValue(new UiState(Status.IDLE, null));
            }
        });
    }

    public void login(String email, String password, Runnable onSuccess) {
        uiState.postValue(new UiState(Status.LOADING, null));
        authRepository.signIn(email, password, new com.example.jvbench.core.common.AuthCallback() {
            @Override
            public void onSuccess(com.example.jvbench.domain.model.User result) {
                uiState.postValue(new UiState(Status.SUCCESS, "Bienvenue " + result.getEmail()));
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
