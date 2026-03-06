package com.example.jvbench.ui.map;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

public class MapFragment extends Fragment {
    private MapView mapView;

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
        mapView.getController().setCenter(new org.osmdroid.util.GeoPoint(49.8941, 2.2958));
        mapView.setMultiTouchControls(true);

        View addBenchButton = view.findViewById(R.id.goBenchFormButton);
        User currentUser = app.getAppContainer().authRepository.getCurrentUser();
        addBenchButton.setVisibility(currentUser == null ? View.GONE : View.VISIBLE);

        addBenchButton.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(R.id.action_mapFragment_to_benchFormFragment));

        BottomNavigationView bottomNavigationView = view.findViewById(R.id.mapBottomNav);
        bottomNavigationView.setSelectedItemId(R.id.navMapItem);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.navMapItem) {
                return true;
            }
            if (item.getItemId() == R.id.navAccountItem) {
                NavHostFragment.findNavController(this).navigate(R.id.action_mapFragment_to_accountFragment);
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

            mapView.getController().setCenter(new org.osmdroid.util.GeoPoint(state.center.getLatitude(), state.center.getLongitude()));
            showBenchMarkers(state.benches);
        });

        viewModel.loadMapData();
    }

    private void showBenchMarkers(java.util.List<Bench> benches) {
        mapView.getOverlays().clear();
        for (Bench bench : benches) {
            Marker marker = new Marker(mapView);
            marker.setPosition(new org.osmdroid.util.GeoPoint(bench.getLatitude(), bench.getLongitude()));
            marker.setTitle(bench.getName());
            marker.setSnippet(bench.getDescription());
            marker.setOnMarkerClickListener((clickedMarker, mapView1) -> {
                Bundle args = new Bundle();
                args.putString(NavConstants.ARG_BENCH_ID, bench.getId());
                NavHostFragment.findNavController(this)
                        .navigate(R.id.action_mapFragment_to_benchDetailFragment, args);
                return true;
            });
            mapView.getOverlays().add(marker);
        }
        mapView.invalidate();

        // TODO: Replace static marker refresh with reactive map layer updates.
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
