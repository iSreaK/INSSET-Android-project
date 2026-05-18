package com.example.jvbench.ui.benchdetail;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.example.jvbench.R;
import com.example.jvbench.core.navigation.NavConstants;
import com.example.jvbench.core.network.NetworkMonitor;
import com.example.jvbench.core.theme.WindowInsetsHelper;
import com.example.jvbench.data.remote.supabase.SupabaseRealtimeClient;
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
    private SupabaseRealtimeClient realtimeClient;
    @Nullable
    private Object reviewsSub;
    @Nullable
    private Object benchSub;
    @Nullable
    private NetworkMonitor networkMonitor;
    @Nullable
    private NetworkMonitor.Listener networkListener;
    @Nullable
    private Button addReviewButtonRef;
    @Nullable
    private View editButtonRef;
    @Nullable
    private View deleteButtonRef;

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
        realtimeClient = app.getAppContainer().supabaseRealtimeClient;
        networkMonitor = app.getAppContainer().networkMonitor;

        View content = view.findViewById(R.id.benchDetailContent);
        if (content != null) WindowInsetsHelper.addBottomSystemInset(content);

        TextView nameText = view.findViewById(R.id.benchDetailNameText);
        TextView descriptionText = view.findViewById(R.id.benchDetailDescriptionText);
        TextView authorText = view.findViewById(R.id.benchDetailAuthorText);
        TextView coordinatesText = view.findViewById(R.id.benchDetailCoordinatesText);
        TextView ratingBigText = view.findViewById(R.id.benchDetailRatingBig);
        ratingCirclesRow = view.findViewById(R.id.ratingCirclesRow);
        ImageView imageView = view.findViewById(R.id.benchDetailImage);
        Button addReviewButton = view.findViewById(R.id.goReviewFormButton);
        View editButton = view.findViewById(R.id.editBenchButton);
        View deleteButton = view.findViewById(R.id.deleteBenchButton);
        addReviewButtonRef = addReviewButton;
        editButtonRef = editButton;
        deleteButtonRef = deleteButton;
        View backButton = view.findViewById(R.id.backButton);
        TextView reviewsEmptyText = view.findViewById(R.id.reviewsEmptyText);
        RecyclerView reviewsRecycler = view.findViewById(R.id.reviewsRecycler);
        SwipeRefreshLayout swipeRefresh = view.findViewById(R.id.benchDetailSwipeRefresh);

        final String benchIdFinal = getArguments() != null ? getArguments().getString(NavConstants.ARG_BENCH_ID) : null;

        User currentUser = app.getAppContainer().authRepository.getCurrentUser();
        ReviewsAdapter reviewsAdapter = new ReviewsAdapter(review -> new AlertDialog.Builder(requireContext())
                .setTitle(R.string.confirm_delete_review_title)
                .setMessage(R.string.confirm_delete_message)
                .setPositiveButton(R.string.action_delete, (d, w) -> {
                    if (benchIdFinal != null) viewModel.deleteReview(review.getId(), benchIdFinal);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show());
        reviewsAdapter.setCurrentUserId(currentUser != null ? currentUser.getId() : null);
        reviewsAdapter.setCanModerate(currentUser != null && currentUser.getRole().canModerate());
        reviewsRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        reviewsRecycler.setAdapter(reviewsAdapter);

        if (swipeRefresh != null) {
            swipeRefresh.setOnRefreshListener(() -> {
                if (benchIdFinal != null) {
                    viewModel.loadBench(benchIdFinal);
                    viewModel.loadReviews(benchIdFinal);
                }
                swipeRefresh.setRefreshing(false);
            });
        }

        buildRatingCircles();

        backButton.setOnClickListener(v -> NavHostFragment.findNavController(this).navigateUp());

        addReviewButton.setVisibility(currentUser == null ? View.GONE : View.VISIBLE);
        editButton.setVisibility(View.GONE);
        deleteButton.setVisibility(View.GONE);

        String benchId = benchIdFinal;
        if (benchId == null || benchId.isBlank()) {
            nameText.setText(R.string.error_missing_bench_id);
            return;
        }

        addReviewButton.setOnClickListener(v -> {
            if (blockedWhenOffline()) return;
            Bundle args = new Bundle();
            args.putString(NavConstants.ARG_BENCH_ID, benchId);
            NavHostFragment.findNavController(this).navigate(R.id.action_benchDetailFragment_to_reviewFormFragment, args);
        });

        editButton.setOnClickListener(v -> {
            if (blockedWhenOffline()) return;
            Bundle args = new Bundle();
            args.putString(NavConstants.ARG_BENCH_ID, benchId);
            NavHostFragment.findNavController(this).navigate(R.id.action_benchDetailFragment_to_benchFormFragment, args);
        });

        deleteButton.setOnClickListener(v -> {
            if (blockedWhenOffline()) return;
            new AlertDialog.Builder(requireContext())
                    .setTitle(R.string.confirm_delete_title)
                    .setMessage(R.string.confirm_delete_message)
                    .setPositiveButton(R.string.action_delete, (dialog, which) -> viewModel.deleteBench())
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
        });

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

            boolean isOwner = currentUser != null && currentUser.getId().equals(bench.getAuthorId());
            boolean canModerate = currentUser != null && currentUser.getRole().canModerate();
            // Edit only owner or admin (moderators don't rewrite content).
            boolean canEdit = isOwner || (currentUser != null && currentUser.getRole().isAdmin());
            // Delete owner OR any moderator/admin.
            boolean canDelete = isOwner || canModerate;
            editButton.setVisibility(canEdit ? View.VISIBLE : View.GONE);
            deleteButton.setVisibility(canDelete ? View.VISIBLE : View.GONE);
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

            // Adapt the CTA: "Modifier votre avis" if user already posted one
            boolean userHasReview = false;
            if (currentUser != null && list != null) {
                for (com.example.jvbench.domain.model.Review r : list) {
                    if (currentUser.getId().equals(r.getUserId())) {
                        userHasReview = true;
                        break;
                    }
                }
            }
            addReviewButton.setText(userHasReview ? R.string.edit_my_review : R.string.add_review);
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
        if (viewModel == null) return;
        final String benchId = getArguments() != null ? getArguments().getString(NavConstants.ARG_BENCH_ID) : null;
        if (benchId == null || benchId.isBlank()) return;

        viewModel.loadReviews(benchId);

        if (realtimeClient != null && reviewsSub == null) {
            reviewsSub = realtimeClient.subscribeTable("public", "reviews", (eventType, record) -> {
                // Only react to events that mention this bench
                if (record == null || benchId.equals(record.optString("bench_id"))) {
                    viewModel.loadReviews(benchId);
                    viewModel.loadBench(benchId);
                }
            });
        }
        if (realtimeClient != null && benchSub == null) {
            benchSub = realtimeClient.subscribeTable("public", "benches", (eventType, record) -> {
                if (record == null || benchId.equals(record.optString("id"))) {
                    viewModel.loadBench(benchId);
                }
            });
        }

        if (networkMonitor != null && networkListener == null) {
            networkListener = this::applyOnlineState;
            networkMonitor.addListener(networkListener);
        }
    }

    @Override
    public void onPause() {
        if (realtimeClient != null) {
            if (reviewsSub != null) { realtimeClient.unsubscribe(reviewsSub); reviewsSub = null; }
            if (benchSub != null)   { realtimeClient.unsubscribe(benchSub);   benchSub = null; }
        }
        if (networkMonitor != null && networkListener != null) {
            networkMonitor.removeListener(networkListener);
            networkListener = null;
        }
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        addReviewButtonRef = null;
        editButtonRef = null;
        deleteButtonRef = null;
        super.onDestroyView();
    }

    /**
     * Returns true (and shows a toast) when the device is offline and the
     * action that triggered this call cannot proceed. Used as a guard inside
     * click handlers so the user gets a clear hint instead of a Supabase
     * timeout much later.
     */
    private boolean blockedWhenOffline() {
        if (networkMonitor == null || networkMonitor.isOnline()) return false;
        Toast.makeText(requireContext(), R.string.offline_action_blocked, Toast.LENGTH_SHORT).show();
        return true;
    }

    /** Updates the visual state of network-dependent buttons. */
    private void applyOnlineState(boolean online) {
        float alpha = online ? 1f : 0.5f;
        if (addReviewButtonRef != null) {
            addReviewButtonRef.setEnabled(online);
            addReviewButtonRef.setAlpha(alpha);
        }
        if (editButtonRef != null && editButtonRef.getVisibility() == View.VISIBLE) {
            editButtonRef.setEnabled(online);
            editButtonRef.setAlpha(alpha);
        }
        if (deleteButtonRef != null && deleteButtonRef.getVisibility() == View.VISIBLE) {
            deleteButtonRef.setEnabled(online);
            deleteButtonRef.setAlpha(alpha);
        }
    }
}
