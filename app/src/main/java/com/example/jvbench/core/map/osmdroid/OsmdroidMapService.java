package com.example.jvbench.core.map.osmdroid;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.jvbench.R;
import com.example.jvbench.core.map.MapMarker;
import com.example.jvbench.core.map.MapService;
import com.example.jvbench.domain.model.GeoPoint;

import org.osmdroid.api.IGeoPoint;
import org.osmdroid.bonuspack.clustering.RadiusMarkerClusterer;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.List;

/**
 * OpenStreetMap implementation of {@link MapService}, backed by osmdroid +
 * osmbonuspack.
 *
 * <p>All osmdroid-specific types live inside this class. Anything leaking
 * out (positions, markers, callbacks) goes through the provider-agnostic
 * types of {@code com.example.jvbench.core.map}.</p>
 *
 * <p>Notes on lifecycle: {@link MyLocationNewOverlay} keeps a strong
 * reference to the underlying {@link MapView}; if we forward stale instances
 * across {@code onCreateView}/{@code onDestroyView} cycles we get a crash
 * the next time the Fragment is re-created. We therefore null both out on
 * {@link #onDestroyView()} and rebuild them lazily in {@link #createMapView(Context)}.</p>
 */
public class OsmdroidMapService implements MapService {

    private static final long LONG_PRESS_TIMEOUT_MS = 1000L;
    private static final int CLUSTER_RADIUS_DP = 120;
    private static final int CLUSTER_ICON_PX = 96;
    /** On-screen size of an individual bench pin (in pixels at xxhdpi-ish density). */
    private static final int BENCH_MARKER_WIDTH_PX = 64;
    private static final int BENCH_MARKER_HEIGHT_PX = 90;
    /** Size of the "you are here" dot drawn by MyLocationNewOverlay. */
    private static final int USER_DOT_PX = 56;

    @Nullable
    private MapView mapView;
    @Nullable
    private RadiusMarkerClusterer markerClusterer;
    @Nullable
    private MyLocationNewOverlay myLocationOverlay;

    private final Handler longPressHandler = new Handler(Looper.getMainLooper());
    @Nullable
    private Runnable longPressRunnable;
    private float downX;
    private float downY;
    private int touchSlop;

    @Nullable
    private OnMarkerClickListener markerListener;
    @Nullable
    private OnLongPressListener longPressListener;

    @NonNull
    @Override
    public View createMapView(@NonNull Context context) {
        Configuration.getInstance().setUserAgentValue(context.getPackageName());

        MapView created = new MapView(context);
        created.setTileSource(TileSourceFactory.MAPNIK);
        created.setMultiTouchControls(true);

        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        attachTouchHandler(created);
        this.mapView = created;
        return created;
    }

    @Override
    public void onResume() {
        if (mapView != null) {
            mapView.onResume();
        }
    }

    @Override
    public void onPause() {
        if (mapView != null) {
            mapView.onPause();
        }
        if (myLocationOverlay != null) {
            try {
                myLocationOverlay.disableMyLocation();
            } catch (Exception ignored) {
                // Overlay already detached — safe to ignore.
            }
        }
        cancelLongPress();
    }

    @Override
    public void onDestroyView() {
        if (myLocationOverlay != null) {
            try {
                myLocationOverlay.disableMyLocation();
            } catch (Exception ignored) {
                // ignored
            }
            myLocationOverlay = null;
        }
        markerClusterer = null;
        mapView = null;
    }

    @Override
    public void setCenter(@NonNull GeoPoint center, double zoom) {
        if (mapView == null) return;
        mapView.getController().setZoom(zoom);
        mapView.getController().setCenter(toOsm(center));
    }

    @Override
    public void animateTo(@NonNull GeoPoint target, double zoom, long durationMs) {
        if (mapView == null) return;
        mapView.getController().animateTo(toOsm(target), zoom, durationMs);
    }

    @Override
    public void enableUserLocationOverlay() {
        if (mapView == null) return;
        if (!hasLocationPermission(mapView.getContext())) return;
        if (myLocationOverlay == null) {
            myLocationOverlay = new MyLocationNewOverlay(
                    new GpsMyLocationProvider(mapView.getContext()), mapView);
            // Replace osmdroid's default "person" sprite by our brand-coloured
            // dot. The directional arrow (used when the device has a compass
            // bearing) is replaced by the same dot so the user sees a
            // consistent visual whether or not the bearing is known.
            Bitmap dot = drawableToBitmap(mapView.getContext(),
                    R.drawable.ic_user_location_dot, USER_DOT_PX, USER_DOT_PX);
            if (dot != null) {
                myLocationOverlay.setPersonIcon(dot);
                myLocationOverlay.setDirectionIcon(dot);
                // Anchor the dot on its own centre rather than the default
                // bottom-left of the person sprite, otherwise the marker is
                // visually offset from the real GPS position.
                myLocationOverlay.setPersonAnchor(0.5f, 0.5f);
                myLocationOverlay.setDirectionAnchor(0.5f, 0.5f);
            }
            myLocationOverlay.enableMyLocation();
            mapView.getOverlays().add(myLocationOverlay);
        }
    }

    @Override
    public void disableUserLocationOverlay() {
        if (myLocationOverlay == null) return;
        try {
            myLocationOverlay.disableMyLocation();
        } catch (Exception ignored) {
            // ignored
        }
        if (mapView != null) {
            mapView.getOverlays().remove(myLocationOverlay);
        }
        myLocationOverlay = null;
    }

    @Nullable
    @Override
    public GeoPoint getUserLocation() {
        if (myLocationOverlay == null) return null;
        org.osmdroid.util.GeoPoint loc = myLocationOverlay.getMyLocation();
        if (loc == null) return null;
        return new GeoPoint(loc.getLatitude(), loc.getLongitude());
    }

    @Override
    public void setMarkers(@NonNull List<MapMarker> markers) {
        if (mapView == null) return;
        if (markerClusterer != null) {
            mapView.getOverlays().remove(markerClusterer);
        }
        RadiusMarkerClusterer clusterer = new RadiusMarkerClusterer(mapView.getContext());
        Bitmap clusterIcon = drawableToBitmap(mapView.getContext(), R.drawable.marker_cluster,
                CLUSTER_ICON_PX, CLUSTER_ICON_PX);
        if (clusterIcon != null) {
            clusterer.setIcon(clusterIcon);
        }
        clusterer.setRadius(CLUSTER_RADIUS_DP);
        clusterer.getTextPaint().setTextSize(36f);
        clusterer.getTextPaint().setColor(0xFFFFFFFF);

        // Build the bench-pin drawable once and reuse it on every marker;
        // osmdroid's default green-hand sprite is replaced by our brand pin.
        Drawable benchPin = ContextCompat.getDrawable(mapView.getContext(), R.drawable.ic_bench_marker);
        if (benchPin != null) {
            benchPin.setBounds(0, 0, BENCH_MARKER_WIDTH_PX, BENCH_MARKER_HEIGHT_PX);
        }

        for (MapMarker model : markers) {
            Marker marker = new Marker(mapView);
            marker.setPosition(toOsm(model.getPosition()));
            marker.setTitle(model.getTitle());
            if (model.getSnippet() != null) {
                marker.setSnippet(model.getSnippet());
            }
            if (benchPin != null) {
                marker.setIcon(benchPin);
                // Anchor on the bottom-centre tip of the pin so the visual
                // point lines up with the actual coordinates.
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            }
            marker.setOnMarkerClickListener((clicked, mv) -> {
                if (markerListener != null) {
                    markerListener.onMarkerClick(model);
                }
                return true;
            });
            clusterer.add(marker);
        }
        mapView.getOverlays().add(clusterer);
        markerClusterer = clusterer;
        mapView.invalidate();
    }

    @Override
    public void clearMarkers() {
        if (mapView == null || markerClusterer == null) return;
        mapView.getOverlays().remove(markerClusterer);
        markerClusterer = null;
        mapView.invalidate();
    }

    @Override
    public void setOnMarkerClickListener(@Nullable OnMarkerClickListener listener) {
        this.markerListener = listener;
    }

    @Override
    public void setOnLongPressListener(@Nullable OnLongPressListener listener) {
        this.longPressListener = listener;
    }

    // --- Helpers ------------------------------------------------------------

    private void attachTouchHandler(@NonNull MapView target) {
        // Custom 1s long-press detection — MapEventsOverlay was too eager (~300ms)
        // and conflicted with pan gestures.
        target.setOnTouchListener((v, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    downX = event.getX();
                    downY = event.getY();
                    cancelLongPress();
                    final float capturedX = downX;
                    final float capturedY = downY;
                    longPressRunnable = () -> firePendingLongPress(capturedX, capturedY);
                    longPressHandler.postDelayed(longPressRunnable, LONG_PRESS_TIMEOUT_MS);
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (Math.abs(event.getX() - downX) > touchSlop
                            || Math.abs(event.getY() - downY) > touchSlop) {
                        cancelLongPress();
                    }
                    break;
                case MotionEvent.ACTION_POINTER_DOWN:
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    cancelLongPress();
                    break;
            }
            // Returning false lets osmdroid keep handling pan / zoom gestures.
            return false;
        });
    }

    private void firePendingLongPress(float x, float y) {
        longPressRunnable = null;
        if (mapView == null || longPressListener == null) return;
        IGeoPoint geoPoint = mapView.getProjection().fromPixels((int) x, (int) y);
        longPressListener.onLongPress(new GeoPoint(geoPoint.getLatitude(), geoPoint.getLongitude()));
    }

    private void cancelLongPress() {
        if (longPressRunnable != null) {
            longPressHandler.removeCallbacks(longPressRunnable);
            longPressRunnable = null;
        }
    }

    private static boolean hasLocationPermission(@NonNull Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private static org.osmdroid.util.GeoPoint toOsm(@NonNull GeoPoint domain) {
        return new org.osmdroid.util.GeoPoint(domain.getLatitude(), domain.getLongitude());
    }

    @Nullable
    private static Bitmap drawableToBitmap(@NonNull Context context, int drawableRes, int widthPx, int heightPx) {
        Drawable drawable = ContextCompat.getDrawable(context, drawableRes);
        if (drawable == null) return null;
        Bitmap bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, widthPx, heightPx);
        drawable.draw(canvas);
        return bitmap;
    }

    /**
     * Convenience hook for callers that have to build views outside a normal
     * {@code Activity} (kept private here — exposed only because the original
     * code used a {@link LayoutInflater} reference indirectly).
     */
    @SuppressWarnings("unused")
    private static LayoutInflater inflater(@NonNull Context context) {
        return LayoutInflater.from(context);
    }
}
