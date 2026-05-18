package com.example.jvbench.ui.benchdetail;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.jvbench.core.common.ResultCallback;
import com.example.jvbench.domain.model.Bench;
import com.example.jvbench.domain.model.Review;
import com.example.jvbench.domain.model.User;
import com.example.jvbench.domain.repository.AuthRepository;
import com.example.jvbench.domain.repository.BenchImageRepository;
import com.example.jvbench.domain.repository.BenchRepository;
import com.example.jvbench.domain.repository.ReviewRepository;

import java.util.Collections;
import java.util.List;

public class BenchDetailViewModel extends ViewModel {
    public static class UiState {
        public final boolean loading;
        public final Bench bench;
        public final String error;
        public final boolean deleted;
        public final String authorUsername;

        public UiState(boolean loading, Bench bench, String error, boolean deleted, String authorUsername) {
            this.loading = loading;
            this.bench = bench;
            this.error = error;
            this.deleted = deleted;
            this.authorUsername = authorUsername;
        }
    }

    private final BenchRepository benchRepository;
    private final BenchImageRepository benchImageRepository;
    private final ReviewRepository reviewRepository;
    private final AuthRepository authRepository;
    private final MutableLiveData<UiState> uiState = new MutableLiveData<>(new UiState(true, null, null, false, null));
    private final MutableLiveData<List<Review>> reviews = new MutableLiveData<>(Collections.emptyList());

    public BenchDetailViewModel(BenchRepository benchRepository,
                                BenchImageRepository benchImageRepository,
                                ReviewRepository reviewRepository,
                                AuthRepository authRepository) {
        this.benchRepository = benchRepository;
        this.benchImageRepository = benchImageRepository;
        this.reviewRepository = reviewRepository;
        this.authRepository = authRepository;
    }

    public LiveData<UiState> getUiState() {
        return uiState;
    }

    public LiveData<List<Review>> getReviews() {
        return reviews;
    }

    public void loadBench(String benchId) {
        uiState.postValue(new UiState(true, null, null, false, null));
        benchRepository.getBenchById(benchId, new ResultCallback<Bench>() {
            @Override
            public void onSuccess(Bench result) {
                uiState.postValue(new UiState(false, result, null, false, null));
                loadAuthor(result);
                loadReviews(benchId);
            }

            @Override
            public void onError(String errorMessage) {
                uiState.postValue(new UiState(false, null, errorMessage, false, null));
            }
        });
    }

    private void loadAuthor(Bench bench) {
        if (bench == null || bench.getAuthorId() == null) return;
        authRepository.getUserById(bench.getAuthorId(), new ResultCallback<User>() {
            @Override
            public void onSuccess(User result) {
                if (result == null) return;
                UiState current = uiState.getValue();
                if (current == null || current.bench == null) return;
                uiState.postValue(new UiState(false, current.bench, current.error, current.deleted, result.getUsername()));
            }

            @Override
            public void onError(String errorMessage) {
                // silently ignore: author label is optional
            }
        });
    }

    public void loadReviews(String benchId) {
        reviewRepository.getReviewsForBench(benchId, new ResultCallback<List<Review>>() {
            @Override
            public void onSuccess(List<Review> result) {
                reviews.postValue(result);
            }

            @Override
            public void onError(String errorMessage) {
                reviews.postValue(Collections.emptyList());
            }
        });
    }

    public void deleteBench() {
        UiState current = uiState.getValue();
        if (current == null || current.bench == null) {
            uiState.postValue(new UiState(false, null, "Banc introuvable.", false, null));
            return;
        }
        Bench bench = current.bench;
        uiState.postValue(new UiState(true, bench, null, false, current.authorUsername));

        benchRepository.deleteBench(bench.getId(), new ResultCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                benchImageRepository.deleteBenchImage(bench.getId(), "jpg", new ResultCallback<Void>() {
                    @Override
                    public void onSuccess(Void r) { /* ignore */ }

                    @Override
                    public void onError(String errorMessage) { /* ignore */ }
                });
                uiState.postValue(new UiState(false, bench, null, true, current.authorUsername));
            }

            @Override
            public void onError(String errorMessage) {
                uiState.postValue(new UiState(false, bench, errorMessage, false, current.authorUsername));
            }
        });
    }
}
