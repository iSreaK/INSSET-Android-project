package com.insset.jvbench.ui.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.insset.jvbench.R;
import com.insset.jvbench.core.navigation.BottomNavBinder;
import com.insset.jvbench.core.theme.ThemePreferences;
import com.insset.jvbench.core.theme.WindowInsetsHelper;
import com.insset.jvbench.di.App;
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
        com.insset.jvbench.domain.model.User u = app.getAppContainer().authRepository.getCurrentUser();
        boolean isAdmin = u != null && u.getRole().isAdmin();
        BottomNavBinder.bind(bottomNavigationView, this, R.id.navSettingsItem, isAdmin);
    }
}
