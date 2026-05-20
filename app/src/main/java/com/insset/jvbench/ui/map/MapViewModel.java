package com.insset.jvbench.ui.map;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.insset.jvbench.core.location.LocationProvider;
import com.insset.jvbench.domain.model.Bench;
import com.insset.jvbench.domain.model.GeoPoint;
import com.insset.jvbench.domain.repository.BenchRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * ViewModel driving {@link MapFragment}.
 *
 * <p>Centering strategy:</p>
 * <ul>
 *   <li>Default: geographic center of metropolitan France, zoomed out enough to
 *       show the whole country. Used while we don't have a position fix.</li>
 *   <li>As soon as the {@link LocationProvider} returns a position, the state
 *       is updated with {@code userLocationKnown=true}. The Fragment then
 *       animates to the user's coordinates.</li>
 * </ul>
 */
public class MapViewModel extends ViewModel {
    /** Approximate geographic center of metropolitan France. */
    public static final GeoPoint FRANCE_CENTER = new GeoPoint(46.227638, 2.213749);
    /** Zoom level that fits all of metropolitan France comfortably on most screens. */
    public static final double FRANCE_ZOOM = 5.5;
    /** Zoom level used once we know where the user is. */
    public static final double USER_ZOOM = 14.0;

    public static class UiState {
        public final boolean loading;
        public final List<Bench> benches;
        public final GeoPoint center;
        public final String error;
        /** {@code true} when {@link #center} is the user's actual position, {@code false} for the France-wide fallback. */
        public final boolean userLocationKnown;

        public UiState(boolean loading, List<Bench> benches, GeoPoint center, String error, boolean userLocationKnown) {
            this.loading = loading;
            this.benches = benches;
            this.center = center;
            this.error = error;
            this.userLocationKnown = userLocationKnown;
        }
    }

    private final BenchRepository benchRepository;
    private final LocationProvider locationProvider;
    private final MutableLiveData<UiState> uiState = new MutableLiveData<>(
            new UiState(true, new ArrayList<>(), FRANCE_CENTER, null, false)
    );

    /**
     * Persistent camera state — kept in the ViewModel rather than the
     * Fragment so that navigating to a bench detail and coming back does
     * not reset the map to its default France-wide view.
     */
    @androidx.annotation.Nullable
    private GeoPoint lastCameraCenter;
    private double lastCameraZoom = FRANCE_ZOOM;
    /** True once the camera has been animated onto the user's real position. */
    private boolean hasCenteredOnUser;

    public MapViewModel(BenchRepository benchRepository, LocationProvider locationProvider) {
        this.benchRepository = benchRepository;
        this.locationProvider = locationProvider;
    }

    public LiveData<UiState> getUiState() {
        return uiState;
    }

    @androidx.annotation.Nullable
    public GeoPoint getLastCameraCenter() {
        return lastCameraCenter;
    }

    public double getLastCameraZoom() {
        return lastCameraZoom;
    }

    public boolean hasCenteredOnUser() {
        return hasCenteredOnUser;
    }

    public void markCenteredOnUser() {
        this.hasCenteredOnUser = true;
    }

    /**
     * Called by the Fragment when its view is about to be destroyed so the
     * current camera state survives the trip to another screen.
     */
    public void saveCameraState(@androidx.annotation.Nullable GeoPoint center, double zoom) {
        if (center == null) return;
        this.lastCameraCenter = center;
        this.lastCameraZoom = zoom;
    }

    public void loadMapData() {
        UiState current = uiState.getValue();
        List<Bench> previousBenches = current != null ? current.benches : new ArrayList<>();
        GeoPoint previousCenter = current != null ? current.center : FRANCE_CENTER;
        boolean previousUserKnown = current != null && current.userLocationKnown;
        uiState.postValue(new UiState(true, previousBenches, previousCenter, null, previousUserKnown));

        locationProvider.getLastKnownLocation(new com.insset.jvbench.core.common.ResultCallback<GeoPoint>() {
            @Override
            public void onSuccess(GeoPoint point) {
                loadBenches(point, true);
            }

            @Override
            public void onError(String errorMessage) {
                // No fix available (permission denied, GPS off, cold start, ...).
                // Keep whatever we had centered before; if nothing, fall back to
                // the France-wide view. The Fragment will not animate as long as
                // userLocationKnown stays false.
                loadBenches(previousCenter, previousUserKnown);
            }
        });
    }

    private void loadBenches(GeoPoint center, boolean userLocationKnown) {
        benchRepository.getBenches(new com.insset.jvbench.core.common.ResultCallback<List<Bench>>() {
            @Override
            public void onSuccess(List<Bench> result) {
                uiState.postValue(new UiState(false, result, center, null, userLocationKnown));
            }

            @Override
            public void onError(String errorMessage) {
                uiState.postValue(new UiState(false, new ArrayList<>(), center, errorMessage, userLocationKnown));
            }
        });
    }
}
