package com.example.jvbench.ui.map;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.example.jvbench.R;
import com.example.jvbench.core.navigation.NavConstants;
import com.example.jvbench.di.App;
import com.example.jvbench.domain.model.Bench;
import com.example.jvbench.domain.model.User;
import com.example.jvbench.ui.main.AppViewModelFactory;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.osmdroid.bonuspack.clustering.RadiusMarkerClusterer;
import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;

public class MapFragment extends Fragment {
    private MapView mapView;
    private RadiusMarkerClusterer markerClusterer;
    private boolean loggedIn;

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

        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.getController().setZoom(14.0);
        mapView.getController().setCenter(new GeoPoint(49.8941, 2.2958));
        mapView.setMultiTouchControls(true);

        View addBenchButton = view.findViewById(R.id.goBenchFormButton);
        User currentUser = app.getAppContainer().authRepository.getCurrentUser();
        loggedIn = currentUser != null;
        addBenchButton.setVisibility(loggedIn ? View.VISIBLE : View.GONE);

        addBenchButton.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(R.id.action_mapFragment_to_benchFormFragment));

        // Long-press on map -> open bench form pre-filled with the coordinates
        MapEventsOverlay eventsOverlay = new MapEventsOverlay(new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                return false;
            }

            @Override
            public boolean longPressHelper(GeoPoint p) {
                if (!loggedIn) {
                    Toast.makeText(requireContext(), R.string.error_guest_action_blocked, Toast.LENGTH_SHORT).show();
                    return true;
                }
                Bundle args = new Bundle();
                args.putFloat(NavConstants.ARG_PREFILL_LAT, (float) p.getLatitude());
                args.putFloat(NavConstants.ARG_PREFILL_LNG, (float) p.getLongitude());
                NavHostFragment.findNavController(MapFragment.this)
                        .navigate(R.id.action_mapFragment_to_benchFormFragment, args);
                return true;
            }
        });
        mapView.getOverlays().add(0, eventsOverlay);

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

    private void showBenchMarkers(java.util.List<Bench> benches) {
        // Remove any previous clusterer
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
                Bundle args = new Bundle();
                args.putString(NavConstants.ARG_BENCH_ID, bench.getId());
                NavHostFragment.findNavController(this)
                        .navigate(R.id.action_mapFragment_to_benchDetailFragment, args);
                return true;
            });
            markerClusterer.add(marker);
        }
        mapView.getOverlays().add(markerClusterer);
        mapView.invalidate();
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
        super.onPause();
    }
}
