package com.example.jvbench.ui.benchdetail;

import android.os.Bundle;
import android.util.TypedValue;
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

import com.bumptech.glide.Glide;
import com.example.jvbench.R;
import com.example.jvbench.core.navigation.NavConstants;
import com.example.jvbench.di.App;
import com.example.jvbench.domain.model.Bench;
import com.example.jvbench.domain.model.Review;
import com.example.jvbench.domain.model.User;
import com.example.jvbench.ui.main.AppViewModelFactory;

import java.util.List;

public class BenchDetailFragment extends Fragment {

    private BenchDetailViewModel viewModel;
    private String benchId;

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

        ImageView benchImage = view.findViewById(R.id.benchDetailImage);
        TextView nameText = view.findViewById(R.id.benchDetailNameText);
        TextView descriptionText = view.findViewById(R.id.benchDetailDescriptionText);
        TextView coordinatesText = view.findViewById(R.id.benchDetailCoordinatesText);
        LinearLayout starsRow = view.findViewById(R.id.starsRow);
        TextView reviewCountText = view.findViewById(R.id.reviewCountText);
        TextView reviewsHeaderLabel = view.findViewById(R.id.reviewsHeaderLabel);
        TextView reviewsToggleIcon = view.findViewById(R.id.reviewsToggleIcon);
        LinearLayout reviewsHeader = view.findViewById(R.id.reviewsHeader);
        LinearLayout reviewsContainer = view.findViewById(R.id.reviewsContainer);
        View addReviewButton = view.findViewById(R.id.goReviewFormButton);

        User currentUser = app.getAppContainer().authRepository.getCurrentUser();
        addReviewButton.setVisibility(currentUser == null ? View.GONE : View.VISIBLE);

        view.findViewById(R.id.backButton).setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigateUp());

        benchId = getArguments() != null ? getArguments().getString(NavConstants.ARG_BENCH_ID) : null;
        if (benchId == null || benchId.isBlank()) {
            nameText.setText(R.string.error_missing_bench_id);
            return;
        }

        addReviewButton.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putString(NavConstants.ARG_BENCH_ID, benchId);
            NavHostFragment.findNavController(this).navigate(R.id.action_benchDetailFragment_to_reviewFormFragment, args);
        });

        reviewsHeader.setOnClickListener(v -> {
            boolean expanded = reviewsContainer.getVisibility() == View.VISIBLE;
            reviewsContainer.setVisibility(expanded ? View.GONE : View.VISIBLE);
            reviewsToggleIcon.setText(expanded ? "▼" : "▲");
        });

        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            if (state == null) return;
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

            renderStars(starsRow, bench.getAverageRating(), 20);

            int count = bench.getReviewCount();
            if (count == 0) {
                reviewCountText.setText("Aucun avis");
            } else {
                reviewCountText.setText(String.format("%.1f / 10  ·  %d avis", bench.getAverageRating(), count));
            }

            reviewsHeaderLabel.setText(String.format("Avis (%d)", count));
            renderReviews(reviewsContainer, state.reviews);

            // Déployer automatiquement si des avis existent
            if (!state.reviews.isEmpty()) {
                reviewsContainer.setVisibility(View.VISIBLE);
                reviewsToggleIcon.setText("▲");
            }

            String imageUrl = bench.getImageUrl();
            if (imageUrl != null && !imageUrl.isBlank()) {
                benchImage.setVisibility(View.VISIBLE);
                Glide.with(this).load(imageUrl).centerCrop().into(benchImage);
            } else {
                benchImage.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (viewModel != null && benchId != null && !benchId.isBlank()) {
            viewModel.loadBench(benchId);
        }
    }

    private void renderStars(LinearLayout container, double rating, int sizeSp) {
        container.removeAllViews();
        int filled = (int) Math.round(rating);
        for (int i = 0; i < 10; i++) {
            TextView star = new TextView(requireContext());
            star.setText(i < filled ? "★" : "☆");
            star.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp);
            star.setTextColor(requireContext().getColor(
                    i < filled ? R.color.jv_orange : R.color.jv_input_hint));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMarginEnd(dp(1));
            star.setLayoutParams(params);
            container.addView(star);
        }
    }

    private void renderReviews(LinearLayout container, List<Review> reviews) {
        container.removeAllViews();
        if (reviews.isEmpty()) {
            TextView empty = new TextView(requireContext());
            empty.setText("Aucun avis pour l'instant.");
            empty.setTextColor(requireContext().getColor(R.color.jv_input_hint));
            empty.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            p.topMargin = dp(12);
            empty.setLayoutParams(p);
            container.addView(empty);
            return;
        }

        for (int i = 0; i < reviews.size(); i++) {
            Review review = reviews.get(i);

            LinearLayout item = new LinearLayout(requireContext());
            item.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams itemParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            itemParams.topMargin = dp(12);
            item.setLayoutParams(itemParams);

            TextView usernameText = new TextView(requireContext());
            usernameText.setText(review.getUsername());
            usernameText.setTextColor(requireContext().getColor(R.color.jv_orange));
            usernameText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            usernameText.setTypeface(null, android.graphics.Typeface.BOLD);
            LinearLayout.LayoutParams up = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            up.bottomMargin = dp(4);
            usernameText.setLayoutParams(up);
            item.addView(usernameText);

            LinearLayout miniStars = new LinearLayout(requireContext());
            miniStars.setOrientation(LinearLayout.HORIZONTAL);
            renderStars(miniStars, review.getRating(), 14);
            item.addView(miniStars);

            String comment = review.getComment();
            if (comment != null && !comment.isBlank()) {
                TextView commentText = new TextView(requireContext());
                commentText.setText(comment);
                commentText.setTextColor(requireContext().getColor(R.color.jv_text_on_dark));
                commentText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
                commentText.setLineSpacing(0, 1.3f);
                LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                cp.topMargin = dp(4);
                commentText.setLayoutParams(cp);
                item.addView(commentText);
            }

            container.addView(item);

            if (i < reviews.size() - 1) {
                View divider = new View(requireContext());
                LinearLayout.LayoutParams dp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 1);
                dp.topMargin = this.dp(12);
                divider.setLayoutParams(dp);
                divider.setBackgroundColor(0x22FFFFFF);
                container.addView(divider);
            }
        }
    }

    private int dp(int value) {
        return Math.round(value * requireContext().getResources().getDisplayMetrics().density);
    }
}
