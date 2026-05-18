package com.example.jvbench.core.map;

import android.content.Context;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.jvbench.domain.model.GeoPoint;

import java.util.List;

/**
 * Abstraction over the map rendering engine.
 *
 * <p>The UI layer ({@link com.example.jvbench.ui.map.MapFragment}) talks
 * exclusively to this interface — it never imports {@code org.osmdroid.*}
 * (or {@code com.google.android.gms.maps.*}, if we ever swap providers).
 * Switching engines therefore boils down to writing a second
 * {@link MapService} implementation and updating one line of dependency
 * injection in {@code AppContainer}.</p>
 *
 * <p>This satisfies the Option-3 PDF requirement: <em>"Possibilité de
 * remplacer Google Maps ↔ OpenStreetMap sans modifier la couche UI"</em>.</p>
 *
 * <p>The interface is intentionally synchronous and main-thread only. Map
 * providers all expose synchronous read APIs over their main thread; making
 * the abstraction async would force every caller to handle callbacks that
 * never have a non-trivial latency anyway.</p>
 */
public interface MapService {

    /** Callback invoked when the user taps a marker. */
    interface OnMarkerClickListener {
        void onMarkerClick(@NonNull MapMarker marker);
    }

    /** Callback invoked on a sustained press anywhere on the map (not on a marker). */
    interface OnLongPressListener {
        void onLongPress(@NonNull GeoPoint point);
    }

    // --- Lifecycle ----------------------------------------------------------

    /**
     * Builds and returns the native map view to attach to the host layout.
     * Called once from {@code Fragment.onCreateView}.
     */
    @NonNull
    View createMapView(@NonNull Context context);

    /** Forward from {@code Fragment.onResume()}. */
    void onResume();

    /** Forward from {@code Fragment.onPause()}. */
    void onPause();

    /** Forward from {@code Fragment.onDestroyView()}. Releases native resources. */
    void onDestroyView();

    // --- Camera -------------------------------------------------------------

    /** Snaps to a position/zoom without animation. */
    void setCenter(@NonNull GeoPoint center, double zoom);

    /** Animates to a position/zoom over the given duration. */
    void animateTo(@NonNull GeoPoint target, double zoom, long durationMs);

    // --- User location ------------------------------------------------------

    /** Enables the small "blue dot" overlay tracking the user's position. Requires location permission. */
    void enableUserLocationOverlay();

    /** Hides the user-location overlay and releases its provider. */
    void disableUserLocationOverlay();

    /** Best-effort current position. {@code null} if no fix is available yet. */
    @Nullable
    GeoPoint getUserLocation();

    // --- Markers ------------------------------------------------------------

    /** Replaces the current marker set. Implementations are free to cluster/style internally. */
    void setMarkers(@NonNull List<MapMarker> markers);

    /** Removes all markers from the map. */
    void clearMarkers();

    // --- Input --------------------------------------------------------------

    void setOnMarkerClickListener(@Nullable OnMarkerClickListener listener);

    void setOnLongPressListener(@Nullable OnLongPressListener listener);
}
