package com.example.jvbench.ui.benchform;

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
import com.example.jvbench.di.App;
import com.example.jvbench.ui.main.AppViewModelFactory;

public class BenchFormFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_bench_form, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        App app = (App) requireActivity().getApplication();
        BenchFormViewModel viewModel = new ViewModelProvider(this, new AppViewModelFactory(app.getAppContainer())).get(BenchFormViewModel.class);

        EditText nameInput = view.findViewById(R.id.benchNameInput);
        EditText descriptionInput = view.findViewById(R.id.benchDescriptionInput);
        EditText imageUrlInput = view.findViewById(R.id.benchImageUrlInput);
        EditText latitudeInput = view.findViewById(R.id.benchLatitudeInput);
        EditText longitudeInput = view.findViewById(R.id.benchLongitudeInput);
        TextView statusText = view.findViewById(R.id.benchFormStatusText);

        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            if (state != null && state.loading) {
                statusText.setText(R.string.loading);
            } else if (state != null && state.message != null) {
                statusText.setText(state.message);
            }
        });

        view.findViewById(R.id.saveBenchButton).setOnClickListener(v -> {
            String name = nameInput.getText().toString().trim();
            String description = descriptionInput.getText().toString().trim();
            String imageUrl = imageUrlInput.getText().toString().trim();
            String latRaw = latitudeInput.getText().toString().trim();
            String lngRaw = longitudeInput.getText().toString().trim();

            if (TextUtils.isEmpty(name) || TextUtils.isEmpty(description)
                    || TextUtils.isEmpty(latRaw) || TextUtils.isEmpty(lngRaw)) {
                statusText.setText(R.string.error_missing_bench_fields);
                return;
            }

            double lat;
            double lng;
            try {
                lat = Double.parseDouble(latRaw);
                lng = Double.parseDouble(lngRaw);
            } catch (NumberFormatException ex) {
                statusText.setText(R.string.error_invalid_coordinates);
                return;
            }

            viewModel.createBench(name, description, imageUrl, lat, lng, () -> {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> NavHostFragment.findNavController(this).navigateUp());
                }
            });
        });
    }
}
