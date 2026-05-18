package com.example.jvbench.ui.main;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.jvbench.R;
import com.example.jvbench.di.App;

public class MainActivity extends AppCompatActivity {

    private static final String PREFS = "jvbench_main";
    private static final String KEY_LOCATION_ASKED = "location_permission_asked";

    private ActivityResultLauncher<String[]> locationPermissionLauncher;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        View root = findViewById(R.id.nav_host_fragment);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            // Only consume top inset (status bar) here; each fragment is in
            // charge of applying the BOTTOM inset on the right child (the
            // BottomNavigationView for screens that have one, or the
            // ScrollView / RecyclerView for the others). We return the
            // unmodified insets so children still see the bottom value.
            v.setPadding(bars.left, bars.top, bars.right, 0);
            return insets;
        });
        ViewCompat.requestApplyInsets(root);

        locationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    // We only need to remember that we asked, not the outcome.
                    // If user denied, we won't bug them again.
                }
        );

        App app = (App) getApplication();
        app.getAppContainer().authRepository.loadCurrentUser(new com.example.jvbench.core.common.ResultCallback<com.example.jvbench.domain.model.User>() {
            @Override
            public void onSuccess(com.example.jvbench.domain.model.User result) {
            }

            @Override
            public void onError(String errorMessage) {
            }
        });

        maybeRequestLocationPermission();
    }

    private void maybeRequestLocationPermission() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        boolean alreadyAsked = prefs.getBoolean(KEY_LOCATION_ASKED, false);
        boolean granted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        if (granted || alreadyAsked) {
            return;
        }
        prefs.edit().putBoolean(KEY_LOCATION_ASKED, true).apply();
        locationPermissionLauncher.launch(new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        });
    }
}
