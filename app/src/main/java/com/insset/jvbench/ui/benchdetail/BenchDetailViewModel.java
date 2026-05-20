package com.insset.jvbench.ui.benchdetail;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.insset.jvbench.core.common.ResultCallback;
import com.insset.jvbench.core.location.DistanceUtils;
import com.insset.jvbench.core.location.LocationProvider;
import com.insset.jvbench.domain.model.Bench;
import com.insset.jvbench.domain.model.GeoPoint;
import com.insset.jvbench.domain.model.Review;
import com.insset.jvbench.domain.model.User;
import com.insset.jvbench.domain.repository.AuthRepository;
import com.insset.jvbench.domain.repository.BenchImageRepository;
import com.insset.jvbench.domain.repository.BenchRepository;
import com.insset.jvbench.domain.repository.ReviewRepository;

import java.util.Collections;
import java.util.List;

public class BenchDetailViewModel extends ViewModel {
    public static class UiState {
        public final boolean loading;
        public final Bench bench;
        public final String error;
        public final boolean deleted;
        public final String authorUsername;
        /** Distance in meters from the device to {@link #bench}; {@link Double#NaN} when unknown. */
        public final double distanceMeters;

        public UiState(boolean loading, Bench bench, String error, boolean deleted,
                       String authorUsername, double distanceMeters) {
            this.loading = loading;
            this.bench = bench;
            this.error = error;
            this.deleted = deleted;
            this.authorUsername = authorUsername;
            this.distanceMeters = distanceMeters;
        }
    }

    private final BenchRepository benchRepository;
    private final BenchImageRepository benchImageRepository;
    private final ReviewRepository reviewRepository;
    private final AuthRepository authRepository;
    private final LocationProvider locationProvider;
    private final MutableLiveData<UiState> uiState = new MutableLiveData<>(
            new UiState(true, null, null, false, null, Double.NaN));
    private final MutableLiveData<List<Review>> reviews = new MutableLiveData<>(Collections.emptyList());

    public BenchDetailViewModel(BenchRepository benchRepository,
                                BenchImageRepository benchImageRepository,
                                ReviewRepository reviewRepository,
                                AuthRepository authRepository,
                                LocationProvider locationProvider) {
        this.benchRepository = benchRepository;
        this.benchImageRepository = benchImageRepository;
        this.reviewRepository = reviewRepository;
        this.authRepository = authRepository;
        this.locationProvider = locationProvider;
    }

    public LiveData<UiState> getUiState() {
        return uiState;
    }

    public LiveData<List<Review>> getReviews() {
        return reviews;
    }

    public void loadBench(String benchId) {
        uiState.postValue(new UiState(true, null, null, false, null, Double.NaN));
        benchRepository.getBenchById(benchId, new ResultCallback<Bench>() {
            @Override
            public void onSuccess(Bench result) {
                uiState.postValue(new UiState(false, result, null, false, null, Double.NaN));
                loadAuthor(result);
                loadDistance(result);
                loadReviews(benchId);
            }

            @Override
            public void onError(String errorMessage) {
                uiState.postValue(new UiState(false, null, errorMessage, false, null, Double.NaN));
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
                uiState.postValue(new UiState(false, current.bench, current.error, current.deleted,
                        result.getUsername(), current.distanceMeters));
            }

            @Override
            public void onError(String errorMessage) {
                // silently ignore: author label is optional
            }
        });
    }

    /**
     * Reads the last known device location and publishes the Haversine
     * distance to the current bench. Treated like the author label: if it
     * fails (no fix, permission missing), we simply don't show the distance.
     */
    private void loadDistance(Bench bench) {
        if (bench == null) return;
        locationProvider.getLastKnownLocation(new ResultCallback<GeoPoint>() {
            @Override
            public void onSuccess(GeoPoint point) {
                UiState current = uiState.getValue();
                if (current == null || current.bench == null) return;
                double meters = DistanceUtils.haversineMeters(
                        point.getLatitude(), point.getLongitude(),
                        current.bench.getLatitude(), current.bench.getLongitude());
                uiState.postValue(new UiState(false, current.bench, current.error, current.deleted,
                        current.authorUsername, meters));
            }

            @Override
            public void onError(String errorMessage) {
                // No fix available; leave distanceMeters as NaN and the UI hides the row.
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

    public void deleteReview(String reviewId, String benchId) {
        reviewRepository.deleteReview(reviewId, new ResultCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                loadReviews(benchId);
                loadBench(benchId); // refresh average rating
            }

            @Override
            public void onError(String errorMessage) {
                // ignore; user can retry
            }
        });
    }

    public void deleteBench() {
        UiState current = uiState.getValue();
        if (current == null || current.bench == null) {
            uiState.postValue(new UiState(false, null, "Banc introuvable.", false, null, Double.NaN));
            return;
        }
        Bench bench = current.bench;
        double previousDistance = current.distanceMeters;
        uiState.postValue(new UiState(true, bench, null, false, current.authorUsername, previousDistance));

        benchRepository.deleteBench(bench.getId(), new ResultCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                benchImageRepository.deleteBenchImage(bench.getId(), "jpg", new ResultCallback<Void>() {
                    @Override
                    public void onSuccess(Void r) { /* ignore */ }

                    @Override
                    public void onError(String errorMessage) { /* ignore */ }
                });
                uiState.postValue(new UiState(false, bench, null, true, current.authorUsername, previousDistance));
            }

            @Override
            public void onError(String errorMessage) {
                uiState.postValue(new UiState(false, bench, errorMessage, false,
                        current.authorUsername, previousDistance));
            }
        });
    }
}
