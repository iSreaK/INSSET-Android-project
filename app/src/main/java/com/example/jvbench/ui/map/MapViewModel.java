package com.example.jvbench.ui.map;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.jvbench.core.location.LocationProvider;
import com.example.jvbench.domain.model.Bench;
import com.example.jvbench.domain.model.GeoPoint;
import com.example.jvbench.domain.repository.BenchRepository;

import java.util.ArrayList;
import java.util.List;

public class MapViewModel extends ViewModel {
    public static class UiState {
        public final boolean loading;
        public final List<Bench> benches;
        public final GeoPoint center;
        public final String error;

        public UiState(boolean loading, List<Bench> benches, GeoPoint center, String error) {
            this.loading = loading;
            this.benches = benches;
            this.center = center;
            this.error = error;
        }
    }

    private final BenchRepository benchRepository;
    private final LocationProvider locationProvider;
    private final MutableLiveData<UiState> uiState = new MutableLiveData<>(
            new UiState(true, new ArrayList<>(), new GeoPoint(49.8941, 2.2958), null)
    );

    public MapViewModel(BenchRepository benchRepository, LocationProvider locationProvider) {
        this.benchRepository = benchRepository;
        this.locationProvider = locationProvider;
    }

    public LiveData<UiState> getUiState() {
        return uiState;
    }

    public void loadMapData() {
        UiState current = uiState.getValue();
        uiState.postValue(new UiState(true, current != null ? current.benches : new ArrayList<>(),
                current != null ? current.center : new GeoPoint(49.8941, 2.2958), null));

        locationProvider.getLastKnownLocation(new com.example.jvbench.core.common.ResultCallback<GeoPoint>() {
            @Override
            public void onSuccess(GeoPoint point) {
                loadBenches(point);
            }

            @Override
            public void onError(String errorMessage) {
                // TODO: refine location fallback and permission UX.
                loadBenches(current != null ? current.center : new GeoPoint(49.8941, 2.2958));
            }
        });
    }

    private void loadBenches(GeoPoint center) {
        benchRepository.getBenches(new com.example.jvbench.core.common.ResultCallback<List<Bench>>() {
            @Override
            public void onSuccess(List<Bench> result) {
                uiState.postValue(new UiState(false, result, center, null));
            }

            @Override
            public void onError(String errorMessage) {
                uiState.postValue(new UiState(false, new ArrayList<>(), center, errorMessage));
            }
        });
    }
}
