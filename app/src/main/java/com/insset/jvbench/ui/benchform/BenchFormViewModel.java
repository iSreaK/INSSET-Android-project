package com.insset.jvbench.ui.benchform;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.insset.jvbench.core.common.ResultCallback;
import com.insset.jvbench.domain.model.Bench;
import com.insset.jvbench.domain.model.User;
import com.insset.jvbench.domain.repository.AuthRepository;
import com.insset.jvbench.domain.repository.BenchImageRepository;
import com.insset.jvbench.domain.repository.BenchRepository;

import java.util.UUID;

public class BenchFormViewModel extends ViewModel {
    public static class UiState {
        public final boolean loading;
        public final String message;
        public final Bench prefill;

        public UiState(boolean loading, String message, Bench prefill) {
            this.loading = loading;
            this.message = message;
            this.prefill = prefill;
        }
    }

    private final BenchRepository benchRepository;
    private final BenchImageRepository benchImageRepository;
    private final AuthRepository authRepository;
    private final MutableLiveData<UiState> uiState = new MutableLiveData<>(new UiState(false, null, null));

    private Bench currentBench;

    public BenchFormViewModel(BenchRepository benchRepository,
                              BenchImageRepository benchImageRepository,
                              AuthRepository authRepository) {
        this.benchRepository = benchRepository;
        this.benchImageRepository = benchImageRepository;
        this.authRepository = authRepository;
    }

    public LiveData<UiState> getUiState() {
        return uiState;
    }

    public boolean isEditMode() {
        return currentBench != null;
    }

    public void loadBench(String benchId) {
        if (benchId == null || benchId.isBlank()) {
            return;
        }
        uiState.postValue(new UiState(true, null, null));
        benchRepository.getBenchById(benchId, new ResultCallback<Bench>() {
            @Override
            public void onSuccess(Bench result) {
                currentBench = result;
                uiState.postValue(new UiState(false, null, result));
            }

            @Override
            public void onError(String errorMessage) {
                uiState.postValue(new UiState(false, errorMessage, null));
            }
        });
    }

    public void submit(String name, String description, double latitude, double longitude,
                       byte[] imageBytes, String imageMimeType, Runnable onSuccess) {
        uiState.postValue(new UiState(true, null, currentBench));

        User user = authRepository.getCurrentUser();
        if (user == null) {
            uiState.postValue(new UiState(false, "Action reservee aux utilisateurs connectes.", currentBench));
            return;
        }

        boolean editing = currentBench != null;
        String benchId = editing ? currentBench.getId() : UUID.randomUUID().toString();
        String authorId = editing ? currentBench.getAuthorId() : user.getId();
        String existingImageUrl = editing ? currentBench.getImageUrl() : null;
        double previousAvg = editing ? currentBench.getAverageRating() : 0.0;
        int previousCount = editing ? currentBench.getReviewCount() : 0;
        long createdAt = editing ? currentBench.getCreatedAt() : System.currentTimeMillis();

        Runnable persist = () -> {
            // re-read latest imageUrl from currentBench (mutated below after upload)
            String finalImageUrl = currentBench != null ? currentBench.getImageUrl() : existingImageUrl;
            Bench bench = new Bench(
                    benchId,
                    name,
                    description,
                    latitude,
                    longitude,
                    finalImageUrl,
                    authorId,
                    createdAt,
                    previousAvg,
                    previousCount
            );

            ResultCallback<Void> persistCallback = new ResultCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    uiState.postValue(new UiState(false, editing ? "Banc modifie." : "Banc cree.", bench));
                    if (onSuccess != null) {
                        onSuccess.run();
                    }
                }

                @Override
                public void onError(String errorMessage) {
                    uiState.postValue(new UiState(false, errorMessage, currentBench));
                }
            };

            if (editing) {
                benchRepository.updateBench(bench, persistCallback);
            } else {
                benchRepository.createBench(bench, persistCallback);
            }
        };

        if (imageBytes != null && imageBytes.length > 0) {
            benchImageRepository.uploadBenchImage(benchId, imageBytes, imageMimeType, new ResultCallback<String>() {
                @Override
                public void onSuccess(String publicUrl) {
                    // Stash uploaded URL onto a temporary bench so persist sees it.
                    currentBench = new Bench(
                            benchId, name, description, latitude, longitude,
                            publicUrl, authorId, createdAt, previousAvg, previousCount
                    );
                    persist.run();
                }

                @Override
                public void onError(String errorMessage) {
                    uiState.postValue(new UiState(false, "Erreur upload image: " + errorMessage, currentBench));
                }
            });
        } else {
            persist.run();
        }
    }
}
