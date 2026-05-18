package com.example.jvbench.ui.benchdetail;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.jvbench.core.common.ResultCallback;
import com.example.jvbench.domain.model.Bench;
import com.example.jvbench.domain.repository.BenchImageRepository;
import com.example.jvbench.domain.repository.BenchRepository;

public class BenchDetailViewModel extends ViewModel {
    public static class UiState {
        public final boolean loading;
        public final Bench bench;
        public final String error;
        public final boolean deleted;

        public UiState(boolean loading, Bench bench, String error, boolean deleted) {
            this.loading = loading;
            this.bench = bench;
            this.error = error;
            this.deleted = deleted;
        }
    }

    private final BenchRepository benchRepository;
    private final BenchImageRepository benchImageRepository;
    private final MutableLiveData<UiState> uiState = new MutableLiveData<>(new UiState(true, null, null, false));

    public BenchDetailViewModel(BenchRepository benchRepository, BenchImageRepository benchImageRepository) {
        this.benchRepository = benchRepository;
        this.benchImageRepository = benchImageRepository;
    }

    public LiveData<UiState> getUiState() {
        return uiState;
    }

    public void loadBench(String benchId) {
        uiState.postValue(new UiState(true, null, null, false));
        benchRepository.getBenchById(benchId, new ResultCallback<Bench>() {
            @Override
            public void onSuccess(Bench result) {
                uiState.postValue(new UiState(false, result, null, false));
            }

            @Override
            public void onError(String errorMessage) {
                uiState.postValue(new UiState(false, null, errorMessage, false));
            }
        });
    }

    public void deleteBench() {
        UiState current = uiState.getValue();
        if (current == null || current.bench == null) {
            uiState.postValue(new UiState(false, null, "Banc introuvable.", false));
            return;
        }
        Bench bench = current.bench;
        uiState.postValue(new UiState(true, bench, null, false));

        benchRepository.deleteBench(bench.getId(), new ResultCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                // best-effort image cleanup: try common extensions
                benchImageRepository.deleteBenchImage(bench.getId(), "jpg", new ResultCallback<Void>() {
                    @Override
                    public void onSuccess(Void r) { /* ignore */ }

                    @Override
                    public void onError(String errorMessage) { /* ignore */ }
                });
                uiState.postValue(new UiState(false, bench, null, true));
            }

            @Override
            public void onError(String errorMessage) {
                uiState.postValue(new UiState(false, bench, errorMessage, false));
            }
        });
    }
}
