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
        bottomNavigationView.setSelectedItemId(R.id.navSettingsItem);
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
            return false;
        });
    }
}
