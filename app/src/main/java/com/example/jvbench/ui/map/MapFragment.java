package com.example.jvbench.ui.map;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.bumptech.glide.Glide;
import com.example.jvbench.R;
import com.example.jvbench.core.navigation.NavConstants;
import com.example.jvbench.di.App;
import com.example.jvbench.domain.model.Bench;
import com.example.jvbench.domain.model.User;
import com.example.jvbench.ui.main.AppViewModelFactory;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;

import org.osmdroid.api.IGeoPoint;
import org.osmdroid.bonuspack.clustering.RadiusMarkerClusterer;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

public class MapFragment extends Fragment {
    private static final long LONG_PRESS_TIMEOUT_MS = 1000L;
    private static final long VIBRATION_MS = 60L;

    private MapView mapView;
    private RadiusMarkerClusterer markerClusterer;
    private boolean loggedIn;

    private final Handler longPressHandler = new Handler(Looper.getMainLooper());
    @Nullable
    private Runnable longPressRunnable;
    private float downX;
    private float downY;
    private int touchSlop;

    // Popup card state
    private MaterialCardView popupCard;
    private ImageView popupImage;
    private TextView popupName;
    private TextView popupMeta;
    @Nullable
    private Bench selectedBench;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_map, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        App app = (App) requireActivity().getApplication();
        MapViewModel viewModel = new ViewModelProvider(this, new AppViewModelFactory(app.getAppContainer())).get(MapViewModel.class);

        Configuration.getInstance().setUserAgentValue(requireContext().getPackageName());
        mapView = view.findViewById(R.id.mapView);
        TextView statusText = view.findViewById(R.id.mapStatusText);

        touchSlop = ViewConfiguration.get(requireContext()).getScaledTouchSlop();

        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.getController().setZoom(14.0);
        mapView.getController().setCenter(new GeoPoint(49.8941, 2.2958));
        mapView.setMultiTouchControls(true);

        // Popup wiring
        popupCard = view.findViewById(R.id.markerPopupCard);
        popupImage = view.findViewById(R.id.popupImage);
        popupName = view.findViewById(R.id.popupName);
        popupMeta = view.findViewById(R.id.popupMeta);
        View popupClose = view.findViewById(R.id.popupCloseButton);
        View popupVoirPlus = view.findViewById(R.id.popupVoirPlusButton);

        popupClose.setOnClickListener(v -> hidePopup());
        popupVoirPlus.setOnClickListener(v -> {
            if (selectedBench == null) return;
            Bundle args = new Bundle();
            args.putString(NavConstants.ARG_BENCH_ID, selectedBench.getId());
            NavHostFragment.findNavController(this)
                    .navigate(R.id.action_mapFragment_to_benchDetailFragment, args);
        });

        View addBenchButton = view.findViewById(R.id.goBenchFormButton);
        User currentUser = app.getAppContainer().authRepository.getCurrentUser();
        loggedIn = currentUser != null;
        addBenchButton.setVisibility(loggedIn ? View.VISIBLE : View.GONE);

        addBenchButton.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(R.id.action_mapFragment_to_benchFormFragment));

        // Custom 1s long-press detection on the map (replaces MapEventsOverlay)
        mapView.setOnTouchListener((v, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    downX = event.getX();
                    downY = event.getY();
                    cancelLongPress();
                    final float capturedX = downX;
                    final float capturedY = downY;
                    longPressRunnable = () -> handleLongPress(capturedX, capturedY);
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
            return false; // let the map handle pan/zoom
        });

        BottomNavigationView bottomNavigationView = view.findViewById(R.id.mapBottomNav);
        bottomNavigationView.setSelectedItemId(R.id.navMapItem);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.navMapItem) {
                return true;
            }
            if (id == R.id.navAccountItem) {
                NavHostFragment.findNavController(this).navigate(R.id.action_mapFragment_to_accountFragment);
                return true;
            }
            if (id == R.id.navSettingsItem) {
                NavHostFragment.findNavController(this).navigate(R.id.action_mapFragment_to_settingsFragment);
                return true;
            }
            return false;
        });

        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            if (state == null) {
                return;
            }
            if (state.loading) {
                statusText.setText(R.string.loading);
                return;
            }
            if (state.error != null) {
                statusText.setText(state.error);
            } else {
                statusText.setText(getString(R.string.map_ready, state.benches.size()));
            }

            mapView.getController().setCenter(new GeoPoint(state.center.getLatitude(), state.center.getLongitude()));
            showBenchMarkers(state.benches);
        });

        viewModel.loadMapData();
    }

    private void cancelLongPress() {
        if (longPressRunnable != null) {
            longPressHandler.removeCallbacks(longPressRunnable);
            longPressRunnable = null;
        }
    }

    private void handleLongPress(float x, float y) {
        longPressRunnable = null;
        if (!isAdded()) return;
        if (!loggedIn) {
            Toast.makeText(requireContext(), R.string.error_guest_action_blocked, Toast.LENGTH_SHORT).show();
            return;
        }
        IGeoPoint geoPoint = mapView.getProjection().fromPixels((int) x, (int) y);
        triggerHaptic();
        Bundle args = new Bundle();
        args.putFloat(NavConstants.ARG_PREFILL_LAT, (float) geoPoint.getLatitude());
        args.putFloat(NavConstants.ARG_PREFILL_LNG, (float) geoPoint.getLongitude());
        NavHostFragment.findNavController(this)
                .navigate(R.id.action_mapFragment_to_benchFormFragment, args);
    }

    private void triggerHaptic() {
        Vibrator vibrator;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            VibratorManager vm = (VibratorManager) requireContext().getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
            vibrator = vm == null ? null : vm.getDefaultVibrator();
        } else {
            //noinspection deprecation
            vibrator = (Vibrator) requireContext().getSystemService(Context.VIBRATOR_SERVICE);
        }
        if (vibrator == null || !vibrator.hasVibrator()) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(VIBRATION_MS, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            //noinspection deprecation
            vibrator.vibrate(VIBRATION_MS);
        }
    }

    private void showBenchMarkers(java.util.List<Bench> benches) {
        if (markerClusterer != null) {
            mapView.getOverlays().remove(markerClusterer);
        }
        markerClusterer = new RadiusMarkerClusterer(requireContext());
        Bitmap clusterIcon = drawableToBitmap(R.drawable.marker_cluster, 96, 96);
        if (clusterIcon != null) {
            markerClusterer.setIcon(clusterIcon);
        }
        markerClusterer.setRadius(120);
        markerClusterer.getTextPaint().setTextSize(36f);
        markerClusterer.getTextPaint().setColor(0xFFFFFFFF);

        for (Bench bench : benches) {
            Marker marker = new Marker(mapView);
            marker.setPosition(new GeoPoint(bench.getLatitude(), bench.getLongitude()));
            marker.setTitle(bench.getName());
            marker.setSnippet(bench.getDescription());
            marker.setOnMarkerClickListener((clickedMarker, mv) -> {
                showPopup(bench);
                return true;
            });
            markerClusterer.add(marker);
        }
        mapView.getOverlays().add(markerClusterer);
        mapView.invalidate();
    }

    private void showPopup(Bench bench) {
        selectedBench = bench;
        popupName.setText(bench.getName());
        popupMeta.setText(getString(R.string.bench_meta_format, bench.getAverageRating(), bench.getReviewCount()));
        String imageUrl = bench.getImageUrl();
        if (imageUrl != null && !imageUrl.isBlank()) {
            popupImage.setVisibility(View.VISIBLE);
            Glide.with(this).load(imageUrl).centerCrop().into(popupImage);
        } else {
            popupImage.setVisibility(View.GONE);
        }
        popupCard.setVisibility(View.VISIBLE);
    }

    private void hidePopup() {
        popupCard.setVisibility(View.GONE);
        selectedBench = null;
    }

    @Nullable
    private Bitmap drawableToBitmap(int drawableRes, int widthPx, int heightPx) {
        Drawable drawable = ContextCompat.getDrawable(requireContext(), drawableRes);
        if (drawable == null) return null;
        Bitmap bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, widthPx, heightPx);
        drawable.draw(canvas);
        return bitmap;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null) {
            mapView.onResume();
        }
    }

    @Override
    public void onPause() {
        if (mapView != null) {
            mapView.onPause();
        }
        cancelLongPress();
        super.onPause();
    }
}
