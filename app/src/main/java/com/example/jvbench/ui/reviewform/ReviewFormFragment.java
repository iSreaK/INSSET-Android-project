package com.example.jvbench.ui.reviewform;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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

    private ReviewFormViewModel viewModel;
    private boolean prefilled = false;

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
        TextView statusText = view.findViewById(R.id.reviewFormStatusText);
        if (benchId == null || benchId.isBlank()) {
            statusText.setText(R.string.error_missing_bench_id);
            return;
        }

        App app = (App) requireActivity().getApplication();
        viewModel = new ViewModelProvider(this, new AppViewModelFactory(app.getAppContainer())).get(ReviewFormViewModel.class);

        TextView titleText = view.findViewById(R.id.reviewFormTitle);
        EditText ratingInput = view.findViewById(R.id.reviewRatingInput);
        EditText commentInput = view.findViewById(R.id.reviewCommentInput);
        Button saveButton = view.findViewById(R.id.saveReviewButton);
        Button deleteButton = view.findViewById(R.id.deleteReviewButton);
        View backButton = view.findViewById(R.id.backButton);

        backButton.setOnClickListener(v -> NavHostFragment.findNavController(this).navigateUp());

        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            if (state == null) return;
            saveButton.setEnabled(!state.loading);
            deleteButton.setEnabled(!state.loading);

            if (state.loading) {
                statusText.setText(R.string.loading);
            } else if (state.message != null) {
                statusText.setText(state.message);
            } else {
                statusText.setText("");
            }

            if (state.deleted) {
                NavHostFragment.findNavController(this).navigateUp();
                return;
            }

            if (state.prefill != null && !prefilled) {
                prefilled = true;
                titleText.setText(R.string.edit_review_title);
                saveButton.setText(R.string.action_edit);
                deleteButton.setVisibility(View.VISIBLE);
                ratingInput.setText(String.valueOf(state.prefill.getRating()));
                commentInput.setText(state.prefill.getComment());
            } else if (state.prefill == null && prefilled) {
                // After delete: back to create mode (shouldn't normally happen because we navigateUp)
                prefilled = false;
                titleText.setText(R.string.add_review);
                saveButton.setText(R.string.save_review);
                deleteButton.setVisibility(View.GONE);
            }
        });

        saveButton.setOnClickListener(v -> {
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
            viewModel.submit(benchId, rating, comment, () -> {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> NavHostFragment.findNavController(this).navigateUp());
                }
            });
        });

        deleteButton.setOnClickListener(v -> new AlertDialog.Builder(requireContext())
                .setTitle(R.string.confirm_delete_review_title)
                .setMessage(R.string.confirm_delete_message)
                .setPositiveButton(R.string.action_delete, (dialog, which) -> viewModel.deleteExisting(null))
                .setNegativeButton(android.R.string.cancel, null)
                .show());

        viewModel.loadExisting(benchId);
    }
}
