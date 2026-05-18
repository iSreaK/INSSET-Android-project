package com.example.jvbench.ui.mybenches;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.jvbench.core.common.ResultCallback;
import com.example.jvbench.domain.model.Bench;
import com.example.jvbench.domain.model.User;
import com.example.jvbench.domain.repository.AuthRepository;
import com.example.jvbench.domain.repository.BenchRepository;

import java.util.Collections;
import java.util.List;

public class MyBenchesViewModel extends ViewModel {
    public static class UiState {
        public final boolean loading;
        public final List<Bench> benches;
        public final String error;
        public final boolean notLoggedIn;

        public UiState(boolean loading, List<Bench> benches, String error, boolean notLoggedIn) {
            this.loading = loading;
            this.benches = benches;
            this.error = error;
            this.notLoggedIn = notLoggedIn;
        }
    }

    private final BenchRepository benchRepository;
    private final AuthRepository authRepository;
    private final MutableLiveData<UiState> uiState = new MutableLiveData<>(new UiState(false, Collections.emptyList(), null, false));

    public MyBenchesViewModel(BenchRepository benchRepository, AuthRepository authRepository) {
        this.benchRepository = benchRepository;
        this.authRepository = authRepository;
    }

    public LiveData<UiState> getUiState() {
        return uiState;
    }

    public void load() {
        User user = authRepository.getCurrentUser();
        if (user == null) {
            uiState.postValue(new UiState(false, Collections.emptyList(), null, true));
            return;
        }
        uiState.postValue(new UiState(true, Collections.emptyList(), null, false));
        benchRepository.getBenchesByAuthor(user.getId(), new ResultCallback<List<Bench>>() {
            @Override
            public void onSuccess(List<Bench> result) {
                uiState.postValue(new UiState(false, result, null, false));
            }

            @Override
            public void onError(String errorMessage) {
                uiState.postValue(new UiState(false, Collections.emptyList(), errorMessage, false));
            }
        });
    }
}
