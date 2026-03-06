package com.example.jvbench.ui.benchdetail;

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
import com.example.jvbench.ui.main.AppViewModelFactory;

public class BenchDetailFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_bench_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        App app = (App) requireActivity().getApplication();
        BenchDetailViewModel viewModel = new ViewModelProvider(this, new AppViewModelFactory(app.getAppContainer())).get(BenchDetailViewModel.class);

        TextView nameText = view.findViewById(R.id.benchDetailNameText);
        TextView descriptionText = view.findViewById(R.id.benchDetailDescriptionText);
        TextView coordinatesText = view.findViewById(R.id.benchDetailCoordinatesText);
        TextView metaText = view.findViewById(R.id.benchDetailMetaText);

        String benchId = getArguments() != null ? getArguments().getString(NavConstants.ARG_BENCH_ID) : null;
        if (benchId == null || benchId.isBlank()) {
            nameText.setText(R.string.error_missing_bench_id);
            return;
        }

        view.findViewById(R.id.goReviewFormButton).setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putString(NavConstants.ARG_BENCH_ID, benchId);
            NavHostFragment.findNavController(this).navigate(R.id.action_benchDetailFragment_to_reviewFormFragment, args);
        });

        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            if (state == null) {
                return;
            }
            if (state.loading) {
                nameText.setText(R.string.loading);
                return;
            }
            if (state.error != null) {
                nameText.setText(state.error);
                return;
            }
            Bench bench = state.bench;
            if (bench == null) {
                nameText.setText(R.string.error_bench_not_found);
                return;
            }
            nameText.setText(bench.getName());
            descriptionText.setText(bench.getDescription());
            coordinatesText.setText(getString(R.string.coordinates_format, bench.getLatitude(), bench.getLongitude()));
            metaText.setText(getString(R.string.bench_meta_format, bench.getAverageRating(), bench.getReviewCount()));
        });

        viewModel.loadBench(benchId);
    }
}
