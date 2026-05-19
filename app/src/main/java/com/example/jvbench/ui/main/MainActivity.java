package com.example.jvbench.ui.main;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;

import com.example.jvbench.R;
import com.example.jvbench.core.common.ResultCallback;
import com.example.jvbench.di.App;
import com.example.jvbench.domain.model.User;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private static final String PREFS = "jvbench_main";
    private static final String KEY_LOCATION_ASKED = "location_permission_asked";

    private static final Set<Integer> TOP_LEVEL_DESTINATIONS = new HashSet<>(Arrays.asList(
            R.id.mapFragment,
            R.id.accountFragment,
            R.id.settingsFragment,
            R.id.adminFragment
    ));

    private ActivityResultLauncher<String[]> locationPermissionLauncher;
    private BottomNavigationView bottomNav;
    private NavController navController;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        View root = findViewById(R.id.nav_host_fragment);
        bottomNav = findViewById(R.id.bottom_nav);

        // Top inset on the fragment container; bottom inset on the persistent BottomNav.
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, 0);
            return insets;
        });
        ViewCompat.requestApplyInsets(root);

        final int baseBottomPadding = bottomNav.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(bottomNav, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(),
                    baseBottomPadding + bars.bottom);
            return insets;
        });
        ViewCompat.requestApplyInsets(bottomNav);

        NavHostFragment navHostFragment = (NavHostFragment)
                getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        navController = navHostFragment.getNavController();

        setupBottomNav();
        refreshAdminTab();

        locationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> { }
        );

        App app = (App) getApplication();
        app.getAppContainer().authRepository.loadCurrentUser(new ResultCallback<User>() {
            @Override
            public void onSuccess(User result) {
                runOnUiThread(() -> refreshAdminTab());
            }

            @Override
            public void onError(String errorMessage) { }
        });

        maybeRequestLocationPermission();
    }

    @Override
    protected void onResume() {
        super.onResume();
        App app = (App) getApplication();
        app.getAppContainer().authRepository.loadCurrentUser(new ResultCallback<User>() {
            @Override
            public void onSuccess(User result) {
                runOnUiThread(() -> refreshAdminTab());
            }

            @Override
            public void onError(String errorMessage) { }
        });
    }

    private void setupBottomNav() {
        bottomNav.setOnItemSelectedListener(item -> {
            int target = destinationForMenuItem(item.getItemId());
            if (target == 0) return false;
            NavDestination current = navController.getCurrentDestination();
            if (current != null && current.getId() == target) {
                return true; // already on this tab
            }
            NavOptions opts = new NavOptions.Builder()
                    .setLaunchSingleTop(true)
                    .setPopUpTo(R.id.mapFragment, false)
                    .build();
            navController.navigate(target, null, opts);
            return true;
        });

        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            if (TOP_LEVEL_DESTINATIONS.contains(destination.getId())) {
                bottomNav.setVisibility(View.VISIBLE);
                syncBottomNavSelection(destination.getId());
                refreshAdminTab();
            } else {
                bottomNav.setVisibility(View.GONE);
            }
        });
    }

    /**
     * Called by fragments when they know the auth state has changed (e.g. login/logout),
     * so the admin tab is updated immediately without waiting for a navigation event.
     */
    public void setAdminTabVisible(boolean visible) {
        MenuItem adminItem = bottomNav.getMenu().findItem(R.id.navAdminItem);
        if (adminItem.isVisible() == visible) return;
        adminItem.setVisible(visible);
        // Re-assert selection after setVisible() rebuilds menu items.
        bottomNav.setSelectedItemId(bottomNav.getSelectedItemId());
        bottomNav.jumpDrawablesToCurrentState();
    }

    private void refreshAdminTab() {
        User user = ((App) getApplication()).getAppContainer().authRepository.getCurrentUser();
        setAdminTabVisible(user != null && user.getRole().isAdmin());
    }

    private void syncBottomNavSelection(int destinationId) {
        int menuItemId;
        if (destinationId == R.id.mapFragment) menuItemId = R.id.navMapItem;
        else if (destinationId == R.id.accountFragment) menuItemId = R.id.navAccountItem;
        else if (destinationId == R.id.settingsFragment) menuItemId = R.id.navSettingsItem;
        else if (destinationId == R.id.adminFragment) menuItemId = R.id.navAdminItem;
        else return;
        if (bottomNav.getSelectedItemId() != menuItemId) {
            bottomNav.setSelectedItemId(menuItemId);
            bottomNav.jumpDrawablesToCurrentState();
        }
    }

    private static int destinationForMenuItem(int menuItemId) {
        if (menuItemId == R.id.navMapItem) return R.id.mapFragment;
        if (menuItemId == R.id.navAccountItem) return R.id.accountFragment;
        if (menuItemId == R.id.navSettingsItem) return R.id.settingsFragment;
        if (menuItemId == R.id.navAdminItem) return R.id.adminFragment;
        return 0;
    }

    private void maybeRequestLocationPermission() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        boolean alreadyAsked = prefs.getBoolean(KEY_LOCATION_ASKED, false);
        boolean granted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        if (granted || alreadyAsked) return;
        prefs.edit().putBoolean(KEY_LOCATION_ASKED, true).apply();
        locationPermissionLauncher.launch(new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        });
    }
}
