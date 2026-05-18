package com.example.jvbench.ui.map;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
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
import com.example.jvbench.core.map.MapMarker;
import com.example.jvbench.core.map.MapService;
import com.example.jvbench.core.navigation.NavConstants;
import com.example.jvbench.core.network.NetworkMonitor;
import com.example.jvbench.core.theme.WindowInsetsHelper;
import com.example.jvbench.data.remote.supabase.SupabaseRealtimeClient;
import com.example.jvbench.di.App;
import com.example.jvbench.domain.model.Bench;
import com.example.jvbench.domain.model.GeoPoint;
import com.example.jvbench.domain.model.User;
import com.example.jvbench.ui.main.AppViewModelFactory;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

/**
 * Single map screen.
 *
 * <p>The Fragment owns the overall UI composition (bottom nav, popup card,
 * action buttons, offline banner). Everything map-related — rendering,
 * marker clustering, long-press detection, user-location overlay,
 * camera animations — is delegated to a {@link MapService} obtained from
 * {@link com.example.jvbench.di.AppContainer#mapServiceFactory}, so this
 * Fragment contains zero references to {@code osmdroid.*} or any other
 * provider-specific type.</p>
 */
public class MapFragment extends Fragment {
    private static final long VIBRATION_MS = 60L;
    private static final long USER_RECENTER_ANIM_MS = 900L;
    private static final double LOCATE_ME_ZOOM = 16.0;

    private MapService mapService;
    private MapViewModel viewModel;
    private boolean loggedIn;
    /** Becomes true the first time we animate the camera onto the user's real position. */
    private boolean hasCenteredOnUser;

    @Nullable
    private BottomNavigationView bottomNavCache;
    @Nullable
    private App appCache;
    @Nullable
    private SupabaseRealtimeClient realtimeClient;
    @Nullable
    private Object benchesRealtimeSub;
    @Nullable
    private ImageButton locateMeButton;
    @Nullable
    private TextView offlineBanner;
    @Nullable
    private View addBenchButtonRef;
    @Nullable
    private FloatingActionButton nearbyFab;
    @Nullable
    private NetworkMonitor networkMonitor;
    @Nullable
    private NetworkMonitor.Listener networkListener;

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
        appCache = app;
        viewModel = new ViewModelProvider(this, new AppViewModelFactory(app.getAppContainer())).get(MapViewModel.class);
        realtimeClient = app.getAppContainer().supabaseRealtimeClient;
        networkMonitor = app.getAppContainer().networkMonitor;

        // Build the map view through the abstraction so we don't depend on
        // any particular provider here.
        mapService = app.getAppContainer().mapServiceFactory.create(requireContext());
        FrameLayout mapContainer = view.findViewById(R.id.mapContainer);
        mapContainer.addView(mapService.createMapView(requireContext()),
                new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT));
        mapService.setCenter(MapViewModel.FRANCE_CENTER, MapViewModel.FRANCE_ZOOM);
        mapService.setOnMarkerClickListener(marker -> {
            Object payload = marker.getPayload();
            if (payload instanceof Bench) {
                showPopup((Bench) payload);
            }
        });
        mapService.setOnLongPressListener(this::handleLongPress);

        TextView statusText = view.findViewById(R.id.mapStatusText);

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
        addBenchButtonRef = addBenchButton;
        User currentUser = app.getAppContainer().authRepository.getCurrentUser();
        loggedIn = currentUser != null;
        addBenchButton.setVisibility(loggedIn ? View.VISIBLE : View.GONE);

        addBenchButton.setOnClickListener(v -> {
            if (networkMonitor != null && !networkMonitor.isOnline()) {
                Toast.makeText(requireContext(), R.string.offline_action_blocked, Toast.LENGTH_SHORT).show();
                return;
            }
            NavHostFragment.findNavController(this).navigate(R.id.action_mapFragment_to_benchFormFragment);
        });

        offlineBanner = view.findViewById(R.id.offlineBanner);

        nearbyFab = view.findViewById(R.id.nearbyFab);
        nearbyFab.setOnClickListener(v -> {
            if (networkMonitor != null && !networkMonitor.isOnline()) {
                Toast.makeText(requireContext(), R.string.nearby_button_disabled_offline, Toast.LENGTH_SHORT).show();
                return;
            }
            if (!hasLocationPermission()) {
                Toast.makeText(requireContext(), R.string.nearby_button_disabled_no_perm, Toast.LENGTH_SHORT).show();
                return;
            }
            NavHostFragment.findNavController(this)
                    .navigate(R.id.action_mapFragment_to_nearbyBenchesFragment);
        });

        BottomNavigationView bottomNavigationView = view.findViewById(R.id.mapBottomNav);
        bottomNavCache = bottomNavigationView;
        WindowInsetsHelper.addBottomSystemInset(bottomNavigationView);
        boolean isAdminUser = currentUser != null && currentUser.getRole().isAdmin();
        bottomNavigationView.getMenu().findItem(R.id.navAdminItem).setVisible(isAdminUser);
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
            if (id == R.id.navAdminItem) {
                NavHostFragment.findNavController(this).navigate(R.id.action_mapFragment_to_adminFragment);
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
                statusText.setVisibility(View.VISIBLE);
                return;
            }
            if (state.error != null) {
                statusText.setText(R.string.error_map_load);
                statusText.setVisibility(View.VISIBLE);
            } else {
                statusText.setVisibility(View.GONE);
            }

            // Only animate to the user's coordinates the *first* time we get
            // a real fix. Subsequent updates (realtime bench refresh) must
            // not snap the camera back from wherever the user has panned.
            if (state.userLocationKnown && !hasCenteredOnUser) {
                mapService.animateTo(state.center, MapViewModel.USER_ZOOM, USER_RECENTER_ANIM_MS);
                hasCenteredOnUser = true;
            }
            renderMarkers(state.benches);
        });

        viewModel.loadMapData();

        locateMeButton = view.findViewById(R.id.locateMeButton);
        setupLocationOverlay();
    }

    private void renderMarkers(@NonNull List<Bench> benches) {
        List<MapMarker> markers = new ArrayList<>(benches.size());
        for (Bench bench : benches) {
            markers.add(new MapMarker(
                    bench.getId(),
                    new GeoPoint(bench.getLatitude(), bench.getLongitude()),
                    bench.getName() != null ? bench.getName() : "",
                    bench.getDescription(),
                    bench));
        }
        mapService.setMarkers(markers);
    }

    private void setupLocationOverlay() {
        if (locateMeButton == null) return;
        boolean hasPermission = hasLocationPermission();
        locateMeButton.setEnabled(hasPermission);
        locateMeButton.setImageResource(hasPermission
                ? R.drawable.ic_compass
                : R.drawable.ic_compass_disabled);

        if (!hasPermission) {
            locateMeButton.setOnClickListener(v ->
                    Toast.makeText(requireContext(), R.string.location_disabled_hint, Toast.LENGTH_SHORT).show());
            return;
        }

        mapService.enableUserLocationOverlay();

        locateMeButton.setOnClickListener(v -> {
            GeoPoint loc = mapService.getUserLocation();
            if (loc != null) {
                mapService.animateTo(loc, LOCATE_ME_ZOOM, 700L);
                hasCenteredOnUser = true;
            } else {
                Toast.makeText(requireContext(), R.string.locate_me_waiting, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void handleLongPress(@NonNull GeoPoint point) {
        if (!isAdded()) return;
        if (!loggedIn) {
            Toast.makeText(requireContext(), R.string.error_guest_action_blocked, Toast.LENGTH_SHORT).show();
            return;
        }
        triggerHaptic();
        Bundle args = new Bundle();
        args.putFloat(NavConstants.ARG_PREFILL_LAT, (float) point.getLatitude());
        args.putFloat(NavConstants.ARG_PREFILL_LNG, (float) point.getLongitude());
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

    /**
     * Called by {@link NetworkMonitor} (on the main thread) when connectivity
     * flips. Shows or hides the offline banner and visually grays out the
     * actions that require the Supabase backend.
     */
    private void applyOnlineState(boolean online) {
        if (offlineBanner != null) {
            offlineBanner.setVisibility(online ? View.GONE : View.VISIBLE);
        }
        if (addBenchButtonRef != null && loggedIn) {
            addBenchButtonRef.setEnabled(online);
            addBenchButtonRef.setAlpha(online ? 1f : 0.5f);
        }
        if (nearbyFab != null) {
            nearbyFab.setEnabled(online);
            nearbyFab.setAlpha(online ? 1f : 0.5f);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mapService != null) {
            mapService.onResume();
        }
        if (realtimeClient != null && benchesRealtimeSub == null) {
            benchesRealtimeSub = realtimeClient.subscribeTable("public", "benches", (eventType, record) -> {
                if (viewModel != null) {
                    viewModel.loadMapData();
                }
            });
        }

        if (networkMonitor != null && networkListener == null) {
            networkListener = this::applyOnlineState;
            networkMonitor.addListener(networkListener);
        }

        // Cold-start case: getCurrentUser() may have returned a USER-role
        // fallback while loadCurrentUser was still in-flight. Refresh the
        // admin tab visibility once the real profile lands.
        if (appCache != null) {
            appCache.getAppContainer().authRepository.loadCurrentUser(new com.example.jvbench.core.common.ResultCallback<com.example.jvbench.domain.model.User>() {
                @Override
                public void onSuccess(com.example.jvbench.domain.model.User result) {
                    if (!isAdded() || bottomNavCache == null || result == null) return;
                    requireActivity().runOnUiThread(() ->
                            bottomNavCache.getMenu().findItem(R.id.navAdminItem)
                                    .setVisible(result.getRole().isAdmin()));
                }

                @Override
                public void onError(String errorMessage) { /* keep last known */ }
            });
        }
    }

    @Override
    public void onPause() {
        if (mapService != null) {
            mapService.onPause();
        }
        if (realtimeClient != null && benchesRealtimeSub != null) {
            realtimeClient.unsubscribe(benchesRealtimeSub);
            benchesRealtimeSub = null;
        }
        if (networkMonitor != null && networkListener != null) {
            networkMonitor.removeListener(networkListener);
            networkListener = null;
        }
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        if (mapService != null) {
            mapService.onDestroyView();
        }
        offlineBanner = null;
        addBenchButtonRef = null;
        nearbyFab = null;
        super.onDestroyView();
    }
}
