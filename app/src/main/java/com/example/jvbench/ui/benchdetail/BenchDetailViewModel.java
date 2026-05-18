package com.example.jvbench.ui.benchdetail;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.jvbench.core.common.ResultCallback;
import com.example.jvbench.domain.model.Bench;
import com.example.jvbench.domain.model.Review;
import com.example.jvbench.domain.repository.BenchRepository;
import com.example.jvbench.domain.repository.ReviewRepository;

import java.util.Collections;
import java.util.List;

public class BenchDetailViewModel extends ViewModel {
    public static class UiState {
        public final boolean loading;
        public final Bench bench;
        public final List<Review> reviews;
        public final String error;

        public UiState(boolean loading, Bench bench, List<Review> reviews, String error) {
            this.loading = loading;
            this.bench = bench;
            this.reviews = reviews != null ? reviews : Collections.emptyList();
            this.error = error;
        }
    }

    private final BenchRepository benchRepository;
    private final ReviewRepository reviewRepository;
    private final MutableLiveData<UiState> uiState = new MutableLiveData<>(new UiState(true, null, null, null));

    public BenchDetailViewModel(BenchRepository benchRepository, ReviewRepository reviewRepository) {
        this.benchRepository = benchRepository;
        this.reviewRepository = reviewRepository;
    }

    public LiveData<UiState> getUiState() {
        return uiState;
    }

    public void loadBench(String benchId) {
        uiState.postValue(new UiState(true, null, null, null));
        benchRepository.getBenchById(benchId, new ResultCallback<Bench>() {
            @Override
            public void onSuccess(Bench bench) {
                reviewRepository.getReviewsForBench(benchId, new ResultCallback<List<Review>>() {
                    @Override
                    public void onSuccess(List<Review> reviews) {
                        uiState.postValue(new UiState(false, bench, reviews, null));
                    }

                    @Override
                    public void onError(String errorMessage) {
                        uiState.postValue(new UiState(false, bench, Collections.emptyList(), null));
                    }
                });
            }

            @Override
            public void onError(String errorMessage) {
                uiState.postValue(new UiState(false, null, null, errorMessage));
            }
        });
    }
}
