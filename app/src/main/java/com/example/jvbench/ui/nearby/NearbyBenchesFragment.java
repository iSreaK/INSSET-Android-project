package com.example.jvbench.ui.nearby;

import android.Manifest;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.jvbench.R;
import com.example.jvbench.core.navigation.BenchNavigationLauncher;
import com.example.jvbench.core.navigation.NavConstants;
import com.example.jvbench.core.theme.WindowInsetsHelper;
import com.example.jvbench.di.App;
import com.example.jvbench.domain.model.Bench;
import com.example.jvbench.ui.main.AppViewModelFactory;
import com.google.android.material.slider.Slider;

import java.util.ArrayList;
import java.util.List;

/**
 * Lists benches within a user-selected radius around the current device
 * location, sorted by distance. Each row hands off either to the bench
 * detail screen or to an external navigation app via
 * {@link ExternalNavigation#openDirections(android.content.Context, double, double, String)}.
 */
public class NearbyBenchesFragment extends Fragment {

    private NearbyBenchesViewModel viewModel;
    private NearbyBenchesAdapter adapter;
    private TextView radiusLabel;
    private TextView emptyText;
    private RecyclerView recyclerView;
    /** Bench the user is about to navigate to once permissions are sorted out. */
    @Nullable
    private Bench pendingNavigationBench;
    @Nullable
    private ActivityResultLauncher<String[]> permissionLauncher;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_nearby_benches, container, false);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    // Whatever the user picked, proceed with the launch — the
                    // launcher does its own permission check and silently
                    // skips the geofence/service branch when something is
                    // missing.
                    if (pendingNavigationBench != null) {
                        BenchNavigationLauncher.start(requireContext(), pendingNavigationBench);
                        pendingNavigationBench = null;
                    }
                });
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        App app = (App) requireActivity().getApplication();
        viewModel = new ViewModelProvider(this, new AppViewModelFactory(app.getAppContainer()))
                .get(NearbyBenchesViewModel.class);

        View root = view.findViewById(R.id.nearbyRoot);
        if (root != null) WindowInsetsHelper.addBottomSystemInset(root);

        View backButton = view.findViewById(R.id.nearbyBackButton);
        backButton.setOnClickListener(v -> NavHostFragment.findNavController(this).navigateUp());

        radiusLabel = view.findViewById(R.id.nearbyRadiusLabel);
        emptyText = view.findViewById(R.id.nearbyEmptyText);
        recyclerView = view.findViewById(R.id.nearbyRecycler);
        Slider radiusSlider = view.findViewById(R.id.nearbyRadiusSlider);

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new NearbyBenchesAdapter(new NearbyBenchesAdapter.Callbacks() {
            @Override
            public void onOpenDetail(@NonNull com.example.jvbench.domain.model.Bench bench) {
                Bundle args = new Bundle();
                args.putString(NavConstants.ARG_BENCH_ID, bench.getId());
                NavHostFragment.findNavController(NearbyBenchesFragment.this)
                        .navigate(R.id.action_nearbyBenchesFragment_to_benchDetailFragment, args);
            }

            @Override
            public void onNavigate(@NonNull com.example.jvbench.domain.model.Bench bench) {
                requestPermissionsAndLaunch(bench);
            }
        });
        recyclerView.setAdapter(adapter);

        radiusSlider.setValueFrom(NearbyBenchesViewModel.RADIUS_MIN);
        radiusSlider.setValueTo(NearbyBenchesViewModel.RADIUS_MAX);
        radiusSlider.setStepSize(NearbyBenchesViewModel.RADIUS_STEP);
        radiusSlider.setValue(viewModel.getCurrentRadius());
        radiusSlider.addOnChangeListener((slider, value, fromUser) -> {
            if (fromUser) {
                viewModel.setRadiusMeters((int) value);
            }
        });

        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            if (state == null) return;
            radiusLabel.setText(getString(R.string.nearby_radius_label, state.radiusMeters));

            if (state.loading) {
                emptyText.setVisibility(View.GONE);
                recyclerView.setVisibility(View.GONE);
                return;
            }
            if (!state.userLocationKnown) {
                emptyText.setText(R.string.nearby_no_fix);
                emptyText.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
                return;
            }
            if (state.items.isEmpty()) {
                emptyText.setText(R.string.nearby_empty);
                emptyText.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            } else {
                emptyText.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
                adapter.submit(state.items);
            }
        });

        viewModel.load();
    }

    /**
     * Checks the runtime permissions needed for background geofencing and
     * notification posting; requests whichever ones are missing, then
     * delegates to {@link BenchNavigationLauncher}. If everything is already
     * granted (or running on an old Android), launches immediately.
     */
    private void requestPermissionsAndLaunch(@NonNull Bench bench) {
        if (BenchNavigationLauncher.hasRequiredPermissions(requireContext())) {
            BenchNavigationLauncher.start(requireContext(), bench);
            return;
        }
        List<String> toRequest = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            toRequest.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            toRequest.add(Manifest.permission.POST_NOTIFICATIONS);
        }
        if (toRequest.isEmpty() || permissionLauncher == null) {
            // Nothing to ask for on this OS; just launch (geofence branch
            // will be skipped if FINE_LOCATION itself is missing).
            BenchNavigationLauncher.start(requireContext(), bench);
            return;
        }
        pendingNavigationBench = bench;
        permissionLauncher.launch(toRequest.toArray(new String[0]));
    }
}
