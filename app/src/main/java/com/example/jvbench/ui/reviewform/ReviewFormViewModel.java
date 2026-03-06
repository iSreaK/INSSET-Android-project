package com.example.jvbench.ui.reviewform;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.jvbench.domain.model.Review;
import com.example.jvbench.domain.model.User;
import com.example.jvbench.domain.repository.AuthRepository;
import com.example.jvbench.domain.repository.ReviewRepository;

import java.util.UUID;

public class ReviewFormViewModel extends ViewModel {
    public static class UiState {
        public final boolean loading;
        public final String message;

        public UiState(boolean loading, String message) {
            this.loading = loading;
            this.message = message;
        }
    }

    private final ReviewRepository reviewRepository;
    private final AuthRepository authRepository;
    private final MutableLiveData<UiState> uiState = new MutableLiveData<>(new UiState(false, null));

    public ReviewFormViewModel(ReviewRepository reviewRepository, AuthRepository authRepository) {
        this.reviewRepository = reviewRepository;
        this.authRepository = authRepository;
    }

    public LiveData<UiState> getUiState() {
        return uiState;
    }

    public void addReview(String benchId, int rating, String comment, Runnable onSuccess) {
        uiState.postValue(new UiState(true, null));

        User user = authRepository.getCurrentUser();
        if (user == null) {
            uiState.postValue(new UiState(false, "Action reservee aux utilisateurs connectes."));
            return;
        }
        String userId = user.getId();

        Review review = new Review(
                UUID.randomUUID().toString(),
                benchId,
                userId,
                rating,
                comment,
                System.currentTimeMillis()
        );

        reviewRepository.addReview(review, new com.example.jvbench.core.common.ResultCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                uiState.postValue(new UiState(false, "Review added."));
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
