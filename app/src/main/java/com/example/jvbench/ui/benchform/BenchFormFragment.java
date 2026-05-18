package com.example.jvbench.ui.benchform;

import android.Manifest;
import android.content.ContentResolver;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.example.jvbench.data.remote.supabase.SupabaseResponse;
import com.example.jvbench.di.App;
import com.example.jvbench.di.AppContainer;
import com.example.jvbench.ui.main.AppViewModelFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BenchFormFragment extends Fragment {

    private AppContainer appContainer;
    private EditText latitudeInput;
    private EditText longitudeInput;
    private TextView statusText;
    private ImageView benchImagePreview;
    private View pickImageButton;
    private View saveBenchButton;

    private String selectedImageUrl = null;
    private boolean uploadInProgress = false;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final ActivityResultLauncher<String> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), this::onPhotoPicked);

    private final ActivityResultLauncher<String> requestMediaLocationPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted ->
                    pickImageLauncher.launch("image/*"));

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
        appContainer = app.getAppContainer();
        BenchFormViewModel viewModel = new ViewModelProvider(this, new AppViewModelFactory(appContainer))
                .get(BenchFormViewModel.class);

        EditText nameInput = view.findViewById(R.id.benchNameInput);
        EditText descriptionInput = view.findViewById(R.id.benchDescriptionInput);
        latitudeInput = view.findViewById(R.id.benchLatitudeInput);
        longitudeInput = view.findViewById(R.id.benchLongitudeInput);
        statusText = view.findViewById(R.id.benchFormStatusText);
        benchImagePreview = view.findViewById(R.id.benchImagePreview);
        pickImageButton = view.findViewById(R.id.pickImageButton);
        saveBenchButton = view.findViewById(R.id.saveBenchButton);

        view.findViewById(R.id.backButton).setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigateUp());
        pickImageButton.setOnClickListener(v -> launchImagePicker());

        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            if (state != null && state.loading) {
                statusText.setText(R.string.loading);
            } else if (state != null && state.message != null) {
                statusText.setText(state.message);
            }
        });

        saveBenchButton.setOnClickListener(v -> {
            if (uploadInProgress) {
                setStatus("Upload en cours, veuillez patienter...", false);
                return;
            }
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

            viewModel.createBench(name, description, selectedImageUrl != null ? selectedImageUrl : "", lat, lng, () -> {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> NavHostFragment.findNavController(this).navigateUp());
                }
            });
        });
    }

    private void launchImagePicker() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            boolean granted = ContextCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.ACCESS_MEDIA_LOCATION) == PackageManager.PERMISSION_GRANTED;
            if (!granted) {
                requestMediaLocationPermission.launch(Manifest.permission.ACCESS_MEDIA_LOCATION);
                return;
            }
        }
        pickImageLauncher.launch("image/*");
    }

    private void onPhotoPicked(Uri uri) {
        if (uri == null) return;

        ContentResolver resolver = requireContext().getContentResolver();
        String mimeType = resolver.getType(uri);

        benchImagePreview.setVisibility(View.VISIBLE);
        Glide.with(this).load(uri).centerCrop().into(benchImagePreview);
        ((com.google.android.material.button.MaterialButton) pickImageButton).setText("Changer la photo");
        uploadInProgress = true;
        saveBenchButton.setEnabled(false);
        setStatus("Traitement en cours...", false);

        executor.execute(() -> {
            try {
                // 1. Lecture EXIF avec URI originale (GPS non supprimé) si permission accordée
                Uri exifUri = uri;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                        ContextCompat.checkSelfPermission(requireContext(),
                                Manifest.permission.ACCESS_MEDIA_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    exifUri = android.provider.MediaStore.setRequireOriginal(uri);
                }

                try (InputStream exifStream = resolver.openInputStream(exifUri)) {
                    if (exifStream != null) {
                        ExifInterface exif = new ExifInterface(exifStream);
                        double[] latLong = exif.getLatLong();
                        if (latLong != null) {
                            double lat = latLong[0];
                            double lng = latLong[1];
                            if (isAdded()) {
                                requireActivity().runOnUiThread(() -> {
                                    latitudeInput.setText(String.valueOf(lat));
                                    longitudeInput.setText(String.valueOf(lng));
                                    setStatus("Coordonnées lues depuis la photo. Upload en cours...", true);
                                });
                            }
                        } else {
                            if (isAdded()) {
                                requireActivity().runOnUiThread(() ->
                                        setStatus("La récupération automatique des métadonnées a échoué. Upload en cours...", false));
                            }
                        }
                    }
                }

                // 2. Lecture des bytes pour l'upload
                byte[] imageBytes;
                try (InputStream uploadStream = resolver.openInputStream(uri)) {
                    if (uploadStream == null) {
                        showUploadError("Impossible de lire la photo.");
                        return;
                    }
                    imageBytes = readAllBytes(uploadStream);
                }

                // 3. Upload vers Supabase Storage
                String extension = "image/png".equals(mimeType) ? "png" : "jpg";
                String filename = UUID.randomUUID() + "." + extension;
                String uploadUrl = appContainer.supabaseClientProvider.getStorageBaseUrl()
                        + "/object/bench-images/" + filename;

                SupabaseResponse response = appContainer.supabaseApiClient.uploadImage(uploadUrl, imageBytes, mimeType);

                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    uploadInProgress = false;
                    saveBenchButton.setEnabled(true);
                    if (response.isSuccessful()) {
                        selectedImageUrl = appContainer.supabaseClientProvider.getStorageBaseUrl()
                                + "/object/public/bench-images/" + filename;
                        setStatus("Photo ajoutée avec succès.", true);
                    } else {
                        setStatus("Erreur lors de l'upload : " + response.getError(), false);
                    }
                });

            } catch (Exception e) {
                showUploadError("Erreur inattendue : " + e.getMessage());
            }
        });
    }

    private void setStatus(String message, boolean success) {
        statusText.setTextColor(requireContext().getColor(
                success ? R.color.jv_success_green : R.color.jv_error_red));
        statusText.setText(message);
    }

    private void showUploadError(String message) {
        if (!isAdded()) return;
        requireActivity().runOnUiThread(() -> {
            uploadInProgress = false;
            saveBenchButton.setEnabled(true);
            setStatus(message, false);
        });
    }

    private static byte[] readAllBytes(InputStream stream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        int bytesRead;
        while ((bytesRead = stream.read(chunk)) != -1) {
            buffer.write(chunk, 0, bytesRead);
        }
        return buffer.toByteArray();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
