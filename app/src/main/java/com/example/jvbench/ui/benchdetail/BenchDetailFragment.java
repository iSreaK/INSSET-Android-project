package com.example.jvbench.ui.benchdetail;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.jvbench.R;
import com.example.jvbench.core.navigation.NavConstants;
import com.example.jvbench.di.App;
import com.example.jvbench.domain.model.Bench;
import com.example.jvbench.domain.model.User;
import com.example.jvbench.domain.model.UserRole;
import com.example.jvbench.ui.main.AppViewModelFactory;

public class BenchDetailFragment extends Fragment {

    private static final int RATING_CIRCLES = 10;

    private BenchDetailViewModel viewModel;
    private LinearLayout ratingCirclesRow;

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
        viewModel = new ViewModelProvider(this, new AppViewModelFactory(app.getAppContainer())).get(BenchDetailViewModel.class);

        TextView nameText = view.findViewById(R.id.benchDetailNameText);
        TextView descriptionText = view.findViewById(R.id.benchDetailDescriptionText);
        TextView authorText = view.findViewById(R.id.benchDetailAuthorText);
        TextView coordinatesText = view.findViewById(R.id.benchDetailCoordinatesText);
        TextView ratingBigText = view.findViewById(R.id.benchDetailRatingBig);
        ratingCirclesRow = view.findViewById(R.id.ratingCirclesRow);
        ImageView imageView = view.findViewById(R.id.benchDetailImage);
        View addReviewButton = view.findViewById(R.id.goReviewFormButton);
        View editButton = view.findViewById(R.id.editBenchButton);
        View deleteButton = view.findViewById(R.id.deleteBenchButton);
        View backButton = view.findViewById(R.id.backButton);
        TextView reviewsEmptyText = view.findViewById(R.id.reviewsEmptyText);
        RecyclerView reviewsRecycler = view.findViewById(R.id.reviewsRecycler);

        ReviewsAdapter reviewsAdapter = new ReviewsAdapter();
        reviewsRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        reviewsRecycler.setAdapter(reviewsAdapter);

        buildRatingCircles();

        backButton.setOnClickListener(v -> NavHostFragment.findNavController(this).navigateUp());

        User currentUser = app.getAppContainer().authRepository.getCurrentUser();
        addReviewButton.setVisibility(currentUser == null ? View.GONE : View.VISIBLE);
        editButton.setVisibility(View.GONE);
        deleteButton.setVisibility(View.GONE);

        String benchId = getArguments() != null ? getArguments().getString(NavConstants.ARG_BENCH_ID) : null;
        if (benchId == null || benchId.isBlank()) {
            nameText.setText(R.string.error_missing_bench_id);
            return;
        }

        addReviewButton.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putString(NavConstants.ARG_BENCH_ID, benchId);
            NavHostFragment.findNavController(this).navigate(R.id.action_benchDetailFragment_to_reviewFormFragment, args);
        });

        editButton.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putString(NavConstants.ARG_BENCH_ID, benchId);
            NavHostFragment.findNavController(this).navigate(R.id.action_benchDetailFragment_to_benchFormFragment, args);
        });

        deleteButton.setOnClickListener(v -> new AlertDialog.Builder(requireContext())
                .setTitle(R.string.confirm_delete_title)
                .setMessage(R.string.confirm_delete_message)
                .setPositiveButton(R.string.action_delete, (dialog, which) -> viewModel.deleteBench())
                .setNegativeButton(android.R.string.cancel, null)
                .show());

        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            if (state == null) return;
            if (state.deleted) {
                NavHostFragment.findNavController(this).navigateUp();
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
            ratingBigText.setText(getString(R.string.bench_rating_big_format, bench.getAverageRating()));
            paintRatingCircles((int) Math.round(bench.getAverageRating()));

            if (state.authorUsername != null && !state.authorUsername.isBlank()) {
                authorText.setText(getString(R.string.bench_author_format, state.authorUsername));
                authorText.setVisibility(View.VISIBLE);
            } else {
                authorText.setVisibility(View.GONE);
            }

            String imageUrl = bench.getImageUrl();
            if (imageUrl != null && !imageUrl.isBlank()) {
                Glide.with(this).load(imageUrl).centerCrop().into(imageView);
            }

            boolean canEdit = currentUser != null
                    && (currentUser.getId().equals(bench.getAuthorId()) || currentUser.getRole() == UserRole.ADMIN);
            editButton.setVisibility(canEdit ? View.VISIBLE : View.GONE);
            deleteButton.setVisibility(canEdit ? View.VISIBLE : View.GONE);
        });

        viewModel.getReviews().observe(getViewLifecycleOwner(), list -> {
            if (list == null || list.isEmpty()) {
                reviewsEmptyText.setVisibility(View.VISIBLE);
                reviewsRecycler.setVisibility(View.GONE);
            } else {
                reviewsEmptyText.setVisibility(View.GONE);
                reviewsRecycler.setVisibility(View.VISIBLE);
                reviewsAdapter.submit(list);
            }
        });

        viewModel.loadBench(benchId);
    }

    private void buildRatingCircles() {
        ratingCirclesRow.removeAllViews();
        int size = (int) (16 * getResources().getDisplayMetrics().density);
        int margin = (int) (4 * getResources().getDisplayMetrics().density);
        for (int i = 0; i < RATING_CIRCLES; i++) {
            View dot = new View(requireContext());
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
            if (i > 0) lp.leftMargin = margin;
            dot.setLayoutParams(lp);
            dot.setBackgroundResource(R.drawable.circle_rating_empty);
            ratingCirclesRow.addView(dot);
        }
    }

    private void paintRatingCircles(int filledCount) {
        int total = ratingCirclesRow.getChildCount();
        for (int i = 0; i < total; i++) {
            View dot = ratingCirclesRow.getChildAt(i);
            dot.setBackgroundResource(i < filledCount
                    ? R.drawable.circle_rating_filled
                    : R.drawable.circle_rating_empty);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (viewModel != null) {
            String benchId = getArguments() != null ? getArguments().getString(NavConstants.ARG_BENCH_ID) : null;
            if (benchId != null && !benchId.isBlank()) {
                viewModel.loadReviews(benchId);
            }
        }
    }
}
