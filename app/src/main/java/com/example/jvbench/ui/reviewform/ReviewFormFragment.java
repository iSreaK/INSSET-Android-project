package com.example.jvbench.ui.reviewform;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.example.jvbench.R;
import com.example.jvbench.core.navigation.NavConstants;
import com.example.jvbench.di.App;
import com.example.jvbench.ui.main.AppViewModelFactory;

public class ReviewFormFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_review_form, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        String benchId = getArguments() != null ? getArguments().getString(NavConstants.ARG_BENCH_ID) : null;
        if (benchId == null || benchId.isBlank()) {
            ((TextView) view.findViewById(R.id.reviewFormStatusText)).setText(R.string.error_missing_bench_id);
            return;
        }

        App app = (App) requireActivity().getApplication();
        ReviewFormViewModel viewModel = new ViewModelProvider(this, new AppViewModelFactory(app.getAppContainer())).get(ReviewFormViewModel.class);

        EditText ratingInput = view.findViewById(R.id.reviewRatingInput);
        EditText commentInput = view.findViewById(R.id.reviewCommentInput);
        TextView statusText = view.findViewById(R.id.reviewFormStatusText);

        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            if (state != null && state.loading) {
                statusText.setText(R.string.loading);
            } else if (state != null && state.message != null) {
                statusText.setText(state.message);
            }
        });

        view.findViewById(R.id.saveReviewButton).setOnClickListener(v -> {
            String ratingRaw = ratingInput.getText().toString().trim();
            String comment = commentInput.getText().toString().trim();
            if (TextUtils.isEmpty(ratingRaw)) {
                statusText.setText(R.string.error_invalid_rating);
                return;
            }

            int rating;
            try {
                rating = Integer.parseInt(ratingRaw);
            } catch (NumberFormatException ex) {
                statusText.setText(R.string.error_invalid_rating);
                return;
            }

            if (rating < 0 || rating > 10) {
                statusText.setText(R.string.error_invalid_rating);
                return;
            }

            viewModel.addReview(benchId, rating, comment, () -> {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> NavHostFragment.findNavController(this).navigateUp());
                }
            });
        });

        // TODO: Add richer validation and display backend errors per field.
    }
}
