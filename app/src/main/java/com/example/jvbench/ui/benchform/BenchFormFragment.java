package com.example.jvbench.ui.benchform;

import android.content.ContentResolver;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.example.jvbench.R;
import com.example.jvbench.core.navigation.NavConstants;
import com.example.jvbench.di.App;
import com.example.jvbench.ui.main.AppViewModelFactory;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class BenchFormFragment extends Fragment {

    private BenchFormViewModel viewModel;
    private ActivityResultLauncher<PickVisualMediaRequest> pickImageLauncher;

    private Uri pickedImageUri;
    private String pickedImageMime;

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
        viewModel = new ViewModelProvider(this, new AppViewModelFactory(app.getAppContainer())).get(BenchFormViewModel.class);

        TextView titleText = view.findViewById(R.id.benchFormTitle);
        EditText nameInput = view.findViewById(R.id.benchNameInput);
        EditText descriptionInput = view.findViewById(R.id.benchDescriptionInput);
        EditText latitudeInput = view.findViewById(R.id.benchLatitudeInput);
        EditText longitudeInput = view.findViewById(R.id.benchLongitudeInput);
        TextView statusText = view.findViewById(R.id.benchFormStatusText);
        Button saveButton = view.findViewById(R.id.saveBenchButton);
        Button pickImageButton = view.findViewById(R.id.pickImageButton);
        ImageView imagePreview = view.findViewById(R.id.benchImagePreview);

        String benchId = getArguments() != null ? getArguments().getString(NavConstants.ARG_BENCH_ID) : null;
        boolean editMode = benchId != null && !benchId.isBlank();

        titleText.setText(editMode ? R.string.edit_bench_title : R.string.create_bench_title);
        saveButton.setText(editMode ? R.string.action_edit : R.string.save_bench);

        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.PickVisualMedia(),
                uri -> {
                    if (uri != null) {
                        pickedImageUri = uri;
                        ContentResolver resolver = requireContext().getContentResolver();
                        pickedImageMime = resolver.getType(uri);
                        imagePreview.setImageURI(uri);
                        imagePreview.setVisibility(View.VISIBLE);
                    }
                }
        );

        pickImageButton.setOnClickListener(v -> pickImageLauncher.launch(
                new PickVisualMediaRequest.Builder()
                        .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                        .build()
        ));

        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            if (state == null) return;
            if (state.loading) {
                statusText.setText(R.string.loading);
                saveButton.setEnabled(false);
                return;
            }
            saveButton.setEnabled(true);
            if (state.prefill != null && nameInput.getText().toString().isEmpty()) {
                nameInput.setText(state.prefill.getName());
                descriptionInput.setText(state.prefill.getDescription());
                latitudeInput.setText(String.valueOf(state.prefill.getLatitude()));
                longitudeInput.setText(String.valueOf(state.prefill.getLongitude()));
                if (state.prefill.getImageUrl() != null && !state.prefill.getImageUrl().isBlank()) {
                    imagePreview.setVisibility(View.VISIBLE);
                }
            }
            if (state.message != null) {
                statusText.setText(state.message);
            }
        });

        if (editMode) {
            viewModel.loadBench(benchId);
        }

        saveButton.setOnClickListener(v -> {
            String name = nameInput.getText().toString().trim();
            String description = descriptionInput.getText().toString().trim();
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

            byte[] imageBytes = null;
            String mime = null;
            if (pickedImageUri != null) {
                imageBytes = readUriBytes(pickedImageUri);
                mime = pickedImageMime != null ? pickedImageMime : "image/jpeg";
                if (imageBytes == null) {
                    statusText.setText(R.string.error_image_read);
                    return;
                }
            }

            viewModel.submit(name, description, lat, lng, imageBytes, mime, () -> {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> NavHostFragment.findNavController(this).navigateUp());
                }
            });
        });
    }

    @Nullable
    private byte[] readUriBytes(@NonNull Uri uri) {
        try (InputStream in = requireContext().getContentResolver().openInputStream(uri)) {
            if (in == null) return null;
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
            byte[] raw = out.toByteArray();
            // sanity: decode bounds to confirm it's an image; ignore the result, just validate.
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(raw, 0, raw.length, opts);
            if (opts.outWidth <= 0) return null;
            return raw;
        } catch (Exception e) {
            return null;
        }
    }
}
