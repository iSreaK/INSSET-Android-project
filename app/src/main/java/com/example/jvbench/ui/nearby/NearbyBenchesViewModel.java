package com.example.jvbench.ui.nearby;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.jvbench.core.common.ResultCallback;
import com.example.jvbench.core.location.DistanceUtils;
import com.example.jvbench.core.location.LocationProvider;
import com.example.jvbench.domain.model.Bench;
import com.example.jvbench.domain.model.GeoPoint;
import com.example.jvbench.domain.repository.BenchRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * ViewModel for the "Bancs autour de vous" screen.
 *
 * <p>Strategy:</p>
 * <ol>
 *   <li>Read the last known user location once.</li>
 *   <li>Pull the full bench list from {@link BenchRepository} (Supabase
 *       already caches network calls, so reloading from disk is cheap).</li>
 *   <li>Compute the Haversine distance for each bench and keep only those
 *       within the currently-selected radius.</li>
 *   <li>Sort by distance ascending.</li>
 * </ol>
 *
 * <p>The radius can be changed via {@link #setRadiusMeters(int)} which simply
 * re-applies the in-memory filter — no network round-trip.</p>
 */
public class NearbyBenchesViewModel extends ViewModel {

    /** Minimum radius (meters) exposed by the slider. */
    public static final int RADIUS_MIN = 50;
    /** Maximum radius (meters) exposed by the slider. */
    public static final int RADIUS_MAX = 1000;
    /** Default radius when the screen opens. */
    public static final int RADIUS_DEFAULT = 200;
    /** Slider step (meters); kept low so users feel the slider is continuous. */
    public static final int RADIUS_STEP = 50;

    public static final class NearbyBench {
        public final Bench bench;
        public final double distanceMeters;

        public NearbyBench(Bench bench, double distanceMeters) {
            this.bench = bench;
            this.distanceMeters = distanceMeters;
        }
    }

    public static final class UiState {
        public final boolean loading;
        public final List<NearbyBench> items;
        public final int radiusMeters;
        public final boolean userLocationKnown;
        public final String error;

        public UiState(boolean loading, List<NearbyBench> items, int radiusMeters,
                       boolean userLocationKnown, String error) {
            this.loading = loading;
            this.items = items;
            this.radiusMeters = radiusMeters;
            this.userLocationKnown = userLocationKnown;
            this.error = error;
        }
    }

    private final BenchRepository benchRepository;
    private final LocationProvider locationProvider;
    private final MutableLiveData<UiState> uiState = new MutableLiveData<>(
            new UiState(true, Collections.emptyList(), RADIUS_DEFAULT, false, null)
    );

    /** Last loaded location (null until a fix is acquired). */
    private GeoPoint userLocation;
    /** Last loaded bench list, kept in memory so radius changes don't refetch. */
    private List<Bench> cachedBenches = Collections.emptyList();
    private int currentRadius = RADIUS_DEFAULT;

    public NearbyBenchesViewModel(BenchRepository benchRepository, LocationProvider locationProvider) {
        this.benchRepository = benchRepository;
        this.locationProvider = locationProvider;
    }

    public LiveData<UiState> getUiState() {
        return uiState;
    }

    public int getCurrentRadius() {
        return currentRadius;
    }

    /** Triggers the initial load. Safe to call repeatedly (e.g. swipe-to-refresh). */
    public void load() {
        uiState.postValue(new UiState(true, Collections.emptyList(), currentRadius, userLocation != null, null));

        locationProvider.getLastKnownLocation(new ResultCallback<GeoPoint>() {
            @Override
            public void onSuccess(GeoPoint point) {
                userLocation = point;
                fetchBenches();
            }

            @Override
            public void onError(String errorMessage) {
                userLocation = null;
                uiState.postValue(new UiState(false, Collections.emptyList(), currentRadius, false, "no_fix"));
            }
        });
    }

    /** Updates the radius and refreshes the filtered list without a network call. */
    public void setRadiusMeters(int radius) {
        int clamped = Math.max(RADIUS_MIN, Math.min(RADIUS_MAX, radius));
        if (clamped == currentRadius && uiState.getValue() != null && !uiState.getValue().loading) {
            return;
        }
        currentRadius = clamped;
        applyFilter();
    }

    private void fetchBenches() {
        benchRepository.getBenches(new ResultCallback<List<Bench>>() {
            @Override
            public void onSuccess(List<Bench> result) {
                cachedBenches = result != null ? result : Collections.emptyList();
                applyFilter();
            }

            @Override
            public void onError(String errorMessage) {
                cachedBenches = Collections.emptyList();
                uiState.postValue(new UiState(false, Collections.emptyList(), currentRadius,
                        userLocation != null, errorMessage));
            }
        });
    }

    private void applyFilter() {
        if (userLocation == null) {
            uiState.postValue(new UiState(false, Collections.emptyList(), currentRadius, false, "no_fix"));
            return;
        }
        List<NearbyBench> filtered = new ArrayList<>();
        for (Bench bench : cachedBenches) {
            double distance = DistanceUtils.haversineMeters(
                    userLocation.getLatitude(), userLocation.getLongitude(),
                    bench.getLatitude(), bench.getLongitude());
            if (distance <= currentRadius) {
                filtered.add(new NearbyBench(bench, distance));
            }
        }
        Collections.sort(filtered, new Comparator<NearbyBench>() {
            @Override
            public int compare(NearbyBench a, NearbyBench b) {
                return Double.compare(a.distanceMeters, b.distanceMeters);
            }
        });
        uiState.postValue(new UiState(false, filtered, currentRadius, true, null));
    }
}
