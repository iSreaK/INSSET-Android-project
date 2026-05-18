package com.example.jvbench.ui.reviewform;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.jvbench.core.common.ResultCallback;
import com.example.jvbench.domain.model.Review;
import com.example.jvbench.domain.model.User;
import com.example.jvbench.domain.repository.AuthRepository;
import com.example.jvbench.domain.repository.ReviewRepository;

import java.util.UUID;

public class ReviewFormViewModel extends ViewModel {
    public static class UiState {
        public final boolean loading;
        public final String message;
        public final Review prefill;
        public final boolean deleted;

        public UiState(boolean loading, String message, Review prefill, boolean deleted) {
            this.loading = loading;
            this.message = message;
            this.prefill = prefill;
            this.deleted = deleted;
        }
    }

    private final ReviewRepository reviewRepository;
    private final AuthRepository authRepository;
    private final MutableLiveData<UiState> uiState = new MutableLiveData<>(new UiState(false, null, null, false));

    private Review existingReview;

    public ReviewFormViewModel(ReviewRepository reviewRepository, AuthRepository authRepository) {
        this.reviewRepository = reviewRepository;
        this.authRepository = authRepository;
    }

    public LiveData<UiState> getUiState() {
        return uiState;
    }

    public boolean hasExistingReview() {
        return existingReview != null;
    }

    public void loadExisting(String benchId) {
        User user = authRepository.getCurrentUser();
        if (user == null) {
            uiState.postValue(new UiState(false, "Action reservee aux utilisateurs connectes.", null, false));
            return;
        }
        uiState.postValue(new UiState(true, null, null, false));
        reviewRepository.getReviewByUserAndBench(user.getId(), benchId, new ResultCallback<Review>() {
            @Override
            public void onSuccess(Review result) {
                existingReview = result;
                uiState.postValue(new UiState(false, null, result, false));
            }

            @Override
            public void onError(String errorMessage) {
                uiState.postValue(new UiState(false, errorMessage, null, false));
            }
        });
    }

    public void submit(String benchId, int rating, String comment, Runnable onSuccess) {
        uiState.postValue(new UiState(true, null, existingReview, false));

        User user = authRepository.getCurrentUser();
        if (user == null) {
            uiState.postValue(new UiState(false, "Action reservee aux utilisateurs connectes.", existingReview, false));
            return;
        }

        if (existingReview != null) {
            Review updated = new Review(
                    existingReview.getId(),
                    existingReview.getBenchId(),
                    existingReview.getUserId(),
                    rating,
                    comment,
                    existingReview.getCreatedAt()
            );
            reviewRepository.updateReview(updated, new ResultCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    existingReview = updated;
                    uiState.postValue(new UiState(false, "Avis modifie.", updated, false));
                    if (onSuccess != null) onSuccess.run();
                }

                @Override
                public void onError(String errorMessage) {
                    uiState.postValue(new UiState(false, errorMessage, existingReview, false));
                }
            });
        } else {
            Review review = new Review(
                    UUID.randomUUID().toString(),
                    benchId,
                    user.getId(),
                    rating,
                    comment,
                    System.currentTimeMillis()
            );
            reviewRepository.addReview(review, new ResultCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    existingReview = review;
                    uiState.postValue(new UiState(false, "Avis ajoute.", review, false));
                    if (onSuccess != null) onSuccess.run();
                }

                @Override
                public void onError(String errorMessage) {
                    uiState.postValue(new UiState(false, errorMessage, null, false));
                }
            });
        }
    }

    public void deleteExisting(Runnable onSuccess) {
        if (existingReview == null) {
            return;
        }
        uiState.postValue(new UiState(true, null, existingReview, false));
        String id = existingReview.getId();
        reviewRepository.deleteReview(id, new ResultCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                existingReview = null;
                uiState.postValue(new UiState(false, "Avis supprime.", null, true));
                if (onSuccess != null) onSuccess.run();
            }

            @Override
            public void onError(String errorMessage) {
                uiState.postValue(new UiState(false, errorMessage, existingReview, false));
            }
        });
    }
}
