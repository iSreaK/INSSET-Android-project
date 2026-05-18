package com.example.jvbench.ui.benchform;

import android.Manifest;
import android.content.ContentResolver;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.exifinterface.media.ExifInterface;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.bumptech.glide.Glide;
import com.example.jvbench.R;
import com.example.jvbench.core.navigation.NavConstants;
import com.example.jvbench.core.theme.WindowInsetsHelper;
import com.example.jvbench.di.App;
import com.example.jvbench.ui.main.AppViewModelFactory;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class BenchFormFragment extends Fragment {

    private static final String TAG = "BenchForm";

    private BenchFormViewModel viewModel;
    private ActivityResultLauncher<String> pickImageLauncher;
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

        View content = view.findViewById(R.id.benchFormContent);
        if (content != null) WindowInsetsHelper.addBottomSystemInset(content);

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

        // GetContent gives a MediaStore URI on which MediaStore.setRequireOriginal()
        // actually works; PickVisualMedia returns a sandboxed copy and strips EXIF.
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri == null) return;
                    pickedImageUri = uri;
                    ContentResolver resolver = requireContext().getContentResolver();
                    pickedImageMime = resolver.getType(uri);
                    // Glide downsamples + caches, otherwise ImageView crashes
                    // on full-res photos (e.g. 12Mpx -> 200MB bitmap).
                    Glide.with(this).load(uri).centerCrop().into(imagePreview);
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
                imageBytes = readUriBytesDownscaled(pickedImageUri);
                // Force JPEG since we re-encode; the stored extension stays
                // consistent with what SupabaseBenchImageRepository expects.
                mime = "image/jpeg";
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
        pickImageLauncher.launch("image/*");
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
        boolean hasMediaLocation = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
                || ContextCompat.checkSelfPermission(requireContext(),
                        Manifest.permission.ACCESS_MEDIA_LOCATION) == PackageManager.PERMISSION_GRANTED;
        Log.d(TAG, "EXIF: uri=" + uri + " mediaLocPermission=" + hasMediaLocation);

        Uri exifUri = uri;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && hasMediaLocation) {
            try {
                exifUri = MediaStore.setRequireOriginal(uri);
                Log.d(TAG, "EXIF: setRequireOriginal -> " + exifUri);
            } catch (Exception e) {
                Log.w(TAG, "EXIF: setRequireOriginal failed: " + e.getMessage());
                exifUri = uri;
            }
        }
        try (InputStream in = resolver.openInputStream(exifUri)) {
            if (in == null) {
                Log.w(TAG, "EXIF: openInputStream returned null");
                return null;
            }
            ExifInterface exif = new ExifInterface(in);
            double[] latLng = exif.getLatLong();
            if (latLng == null) {
                Log.d(TAG, "EXIF: no GPS tags in the image");
            } else {
                Log.d(TAG, "EXIF: lat=" + latLng[0] + " lng=" + latLng[1]);
            }
            return latLng;
        } catch (Exception e) {
            Log.w(TAG, "EXIF: read failed: " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * Read the picked image, downscale it so its longest side is at most
     * {@value #MAX_UPLOAD_SIZE_PX}px, and re-encode as JPEG. Without this a
     * 12-30 Mpx phone photo would be uploaded raw (tens of MB) and would
     * also crash any ImageView that tried to draw it (200+ MB bitmap).
     */
    private static final int MAX_UPLOAD_SIZE_PX = 1600;
    private static final int JPEG_QUALITY = 85;

    @Nullable
    private byte[] readUriBytesDownscaled(@NonNull Uri uri) {
        ContentResolver resolver = requireContext().getContentResolver();
        try {
            // 1st pass: read bounds only to compute an efficient inSampleSize.
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            try (InputStream in = resolver.openInputStream(uri)) {
                if (in == null) return null;
                BitmapFactory.decodeStream(in, null, bounds);
            }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null;

            int longest = Math.max(bounds.outWidth, bounds.outHeight);
            int sample = 1;
            while (longest / (sample * 2) >= MAX_UPLOAD_SIZE_PX) {
                sample *= 2;
            }

            // 2nd pass: actually decode with subsampling.
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = sample;
            opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
            Bitmap decoded;
            try (InputStream in = resolver.openInputStream(uri)) {
                if (in == null) return null;
                decoded = BitmapFactory.decodeStream(in, null, opts);
            }
            if (decoded == null) return null;

            // Final fine-grained scale to make sure we hit the target size
            // exactly even when inSampleSize only halves.
            Bitmap scaled;
            int w = decoded.getWidth();
            int h = decoded.getHeight();
            int longestActual = Math.max(w, h);
            if (longestActual > MAX_UPLOAD_SIZE_PX) {
                float ratio = MAX_UPLOAD_SIZE_PX / (float) longestActual;
                scaled = Bitmap.createScaledBitmap(decoded, Math.round(w * ratio), Math.round(h * ratio), true);
                if (scaled != decoded) decoded.recycle();
            } else {
                scaled = decoded;
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out);
            scaled.recycle();
            return out.toByteArray();
        } catch (Exception e) {
            return null;
        }
    }
}
