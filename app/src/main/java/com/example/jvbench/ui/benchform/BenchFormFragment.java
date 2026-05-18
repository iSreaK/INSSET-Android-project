package com.example.jvbench.ui.benchform;

import android.Manifest;
import android.content.ContentResolver;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
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
import androidx.core.content.ContextCompat;
import androidx.exifinterface.media.ExifInterface;
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
    private ActivityResultLauncher<String> requestMediaLocationLauncher;

    private Uri pickedImageUri;
    private String pickedImageMime;

    /** Last GPS coords extracted from EXIF on the picked image, null if none. */
    @Nullable
    private double[] lastExifLatLng;

    private EditText latitudeInput;
    private EditText longitudeInput;
    private TextView statusText;
    private Button useExifCoordsButton;

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
        latitudeInput = view.findViewById(R.id.benchLatitudeInput);
        longitudeInput = view.findViewById(R.id.benchLongitudeInput);
        statusText = view.findViewById(R.id.benchFormStatusText);
        Button saveButton = view.findViewById(R.id.saveBenchButton);
        Button pickImageButton = view.findViewById(R.id.pickImageButton);
        ImageView imagePreview = view.findViewById(R.id.benchImagePreview);
        useExifCoordsButton = view.findViewById(R.id.useExifCoordsButton);
        View backButton = view.findViewById(R.id.backButton);

        backButton.setOnClickListener(v -> NavHostFragment.findNavController(this).navigateUp());

        Bundle args = getArguments();
        String benchId = args != null ? args.getString(NavConstants.ARG_BENCH_ID) : null;
        boolean editMode = benchId != null && !benchId.isBlank();

        titleText.setText(editMode ? R.string.edit_bench_title : R.string.create_bench_title);
        saveButton.setText(editMode ? R.string.action_edit : R.string.save_bench);

        if (!editMode && args != null) {
            float prefilLat = args.getFloat(NavConstants.ARG_PREFILL_LAT, 0f);
            float prefilLng = args.getFloat(NavConstants.ARG_PREFILL_LNG, 0f);
            if (prefilLat != 0f || prefilLng != 0f) {
                latitudeInput.setText(String.valueOf(prefilLat));
                longitudeInput.setText(String.valueOf(prefilLng));
            }
        }

        // ACCESS_MEDIA_LOCATION request flow (Android 10+). If user denies, we
        // still launch the picker — they just won't get auto-fill from EXIF.
        requestMediaLocationLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> launchPicker()
        );

        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.PickVisualMedia(),
                uri -> {
                    if (uri == null) return;
                    pickedImageUri = uri;
                    ContentResolver resolver = requireContext().getContentResolver();
                    pickedImageMime = resolver.getType(uri);
                    imagePreview.setImageURI(uri);
                    imagePreview.setVisibility(View.VISIBLE);
                    handleExifFromPickedImage(uri);
                }
        );

        pickImageButton.setOnClickListener(v -> requestMediaLocationThenPick());

        useExifCoordsButton.setOnClickListener(v -> {
            if (lastExifLatLng == null) return;
            latitudeInput.setText(String.valueOf(lastExifLatLng[0]));
            longitudeInput.setText(String.valueOf(lastExifLatLng[1]));
            useExifCoordsButton.setVisibility(View.GONE);
            statusText.setText(R.string.exif_coords_filled);
        });

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

            if (TextUtils.isEmpty(name)
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

    /**
     * On Android 10+ we need ACCESS_MEDIA_LOCATION to read the EXIF GPS tags
     * out of a photo. We request it transparently before launching the picker;
     * the user can deny, in which case we just won't auto-fill.
     */
    private void requestMediaLocationThenPick() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            boolean granted = ContextCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.ACCESS_MEDIA_LOCATION) == PackageManager.PERMISSION_GRANTED;
            if (!granted) {
                requestMediaLocationLauncher.launch(Manifest.permission.ACCESS_MEDIA_LOCATION);
                return;
            }
        }
        launchPicker();
    }

    private void launchPicker() {
        pickImageLauncher.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build());
    }

    /**
     * Try to read GPS EXIF tags from the picked image. Behavior:
     *  - GPS found AND lat/lng fields empty → auto-fill them.
     *  - GPS found AND lat/lng already have user-entered values → leave the
     *    fields untouched but reveal a "Utiliser les coordonnees de la photo"
     *    button so the user can opt in.
     *  - No GPS in EXIF → do nothing.
     */
    private void handleExifFromPickedImage(@NonNull Uri uri) {
        lastExifLatLng = null;
        useExifCoordsButton.setVisibility(View.GONE);

        double[] latLng = readExifLatLng(uri);
        if (latLng == null) return;

        lastExifLatLng = latLng;
        boolean coordsAlreadyFilled =
                !latitudeInput.getText().toString().trim().isEmpty()
                || !longitudeInput.getText().toString().trim().isEmpty();
        if (coordsAlreadyFilled) {
            useExifCoordsButton.setVisibility(View.VISIBLE);
        } else {
            latitudeInput.setText(String.valueOf(latLng[0]));
            longitudeInput.setText(String.valueOf(latLng[1]));
            statusText.setText(R.string.exif_coords_filled);
        }
    }

    @Nullable
    private double[] readExifLatLng(@NonNull Uri uri) {
        ContentResolver resolver = requireContext().getContentResolver();
        // setRequireOriginal() on Android Q+ gives us the un-stripped EXIF if
        // we have ACCESS_MEDIA_LOCATION. On older Android it's a no-op via
        // try/catch fallback to the raw URI.
        Uri exifUri = uri;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                && ContextCompat.checkSelfPermission(requireContext(),
                        Manifest.permission.ACCESS_MEDIA_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            try {
                exifUri = MediaStore.setRequireOriginal(uri);
            } catch (Exception ignored) {
                exifUri = uri;
            }
        }
        try (InputStream in = resolver.openInputStream(exifUri)) {
            if (in == null) return null;
            ExifInterface exif = new ExifInterface(in);
            return exif.getLatLong();
        } catch (Exception e) {
            return null;
        }
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
