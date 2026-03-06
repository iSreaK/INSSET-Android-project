package com.example.jvbench.ui.benchdetail;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.jvbench.domain.model.Bench;
import com.example.jvbench.domain.repository.BenchRepository;

public class BenchDetailViewModel extends ViewModel {
    public static class UiState {
        public final boolean loading;
        public final Bench bench;
        public final String error;

        public UiState(boolean loading, Bench bench, String error) {
            this.loading = loading;
            this.bench = bench;
            this.error = error;
        }
    }

    private final BenchRepository benchRepository;
    private final MutableLiveData<UiState> uiState = new MutableLiveData<>(new UiState(true, null, null));

    public BenchDetailViewModel(BenchRepository benchRepository) {
        this.benchRepository = benchRepository;
    }

    public LiveData<UiState> getUiState() {
        return uiState;
    }

    public void loadBench(String benchId) {
        uiState.postValue(new UiState(true, null, null));
        benchRepository.getBenchById(benchId, new com.example.jvbench.core.common.ResultCallback<Bench>() {
            @Override
            public void onSuccess(Bench result) {
                uiState.postValue(new UiState(false, result, null));
            }

            @Override
            public void onError(String errorMessage) {
                uiState.postValue(new UiState(false, null, errorMessage));
            }
        });
    }
}
