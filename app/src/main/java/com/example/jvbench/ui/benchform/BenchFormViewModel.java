package com.example.jvbench.ui.benchform;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.jvbench.domain.model.Bench;
import com.example.jvbench.domain.model.User;
import com.example.jvbench.domain.repository.AuthRepository;
import com.example.jvbench.domain.repository.BenchRepository;

import java.util.UUID;

public class BenchFormViewModel extends ViewModel {
    public static class UiState {
        public final boolean loading;
        public final String message;

        public UiState(boolean loading, String message) {
            this.loading = loading;
            this.message = message;
        }
    }

    private final BenchRepository benchRepository;
    private final AuthRepository authRepository;
    private final MutableLiveData<UiState> uiState = new MutableLiveData<>(new UiState(false, null));

    public BenchFormViewModel(BenchRepository benchRepository, AuthRepository authRepository) {
        this.benchRepository = benchRepository;
        this.authRepository = authRepository;
    }

    public LiveData<UiState> getUiState() {
        return uiState;
    }

    public void createBench(String name, String description, String imageUrl, double latitude, double longitude,
                            Runnable onSuccess) {
        uiState.postValue(new UiState(true, null));

        User user = authRepository.getCurrentUser();
        String authorId = user != null ? user.getId() : "anonymous";

        Bench bench = new Bench(
                UUID.randomUUID().toString(),
                name,
                description,
                latitude,
                longitude,
                imageUrl,
                authorId,
                System.currentTimeMillis(),
                0.0,
                0
        );

        benchRepository.createBench(bench, new com.example.jvbench.core.common.ResultCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                uiState.postValue(new UiState(false, "Bench created."));
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
