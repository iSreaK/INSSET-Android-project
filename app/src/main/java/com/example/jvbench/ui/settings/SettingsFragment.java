package com.example.jvbench.ui.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.jvbench.R;
import com.example.jvbench.core.theme.ThemePreferences;
import com.example.jvbench.core.theme.WindowInsetsHelper;
import com.example.jvbench.di.App;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.materialswitch.MaterialSwitch;

public class SettingsFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        App app = (App) requireActivity().getApplication();
        ThemePreferences themePrefs = app.getThemePreferences();

        MaterialSwitch darkModeSwitch = view.findViewById(R.id.darkModeSwitch);
        darkModeSwitch.setChecked(themePrefs.isDarkMode());
        darkModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (themePrefs.isDarkMode() == isChecked) {
                return;
            }
            themePrefs.setDarkMode(isChecked);
            // Activity will recreate via AppCompatDelegate
            requireActivity().recreate();
        });

        BottomNavigationView bottomNavigationView = view.findViewById(R.id.settingsBottomNav);
        WindowInsetsHelper.addBottomSystemInset(bottomNavigationView);
        com.example.jvbench.domain.model.User u = app.getAppContainer().authRepository.getCurrentUser();
        bottomNavigationView.getMenu().findItem(R.id.navAdminItem)
                .setVisible(u != null && u.getRole().isAdmin());
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.navSettingsItem) return true;
            if (id == R.id.navMapItem) {
                NavHostFragment.findNavController(this).navigate(R.id.action_settingsFragment_to_mapFragment);
                return true;
            }
            if (id == R.id.navAccountItem) {
                NavHostFragment.findNavController(this).navigate(R.id.action_settingsFragment_to_accountFragment);
                return true;
            }
            if (id == R.id.navAdminItem) {
                NavHostFragment.findNavController(this).navigate(R.id.action_settingsFragment_to_adminFragment);
                return true;
            }
            return false;
        });
        // Synchronous so the right tab is highlighted from the very first
        // frame; deferring with post() makes the previously-selected tab
        // visually flicker for one frame.
        bottomNavigationView.setSelectedItemId(R.id.navSettingsItem);
    }
}
