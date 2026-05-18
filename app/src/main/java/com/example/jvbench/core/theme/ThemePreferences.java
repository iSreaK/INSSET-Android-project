package com.example.jvbench.core.theme;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

public class ThemePreferences {
    private static final String PREFS_NAME = "jvbench_theme";
    private static final String KEY_MODE = "night_mode";

    private final SharedPreferences prefs;

    public ThemePreferences(Context context) {
        this.prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Returns the saved night mode, defaulting to MODE_NIGHT_YES (dark) since the app
     * was originally designed for dark UI.
     */
    public int getNightMode() {
        return prefs.getInt(KEY_MODE, AppCompatDelegate.MODE_NIGHT_YES);
    }

    public boolean isDarkMode() {
        return getNightMode() == AppCompatDelegate.MODE_NIGHT_YES;
    }

    public void setDarkMode(boolean dark) {
        int mode = dark ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO;
        prefs.edit().putInt(KEY_MODE, mode).apply();
        AppCompatDelegate.setDefaultNightMode(mode);
    }

    public void apply() {
        AppCompatDelegate.setDefaultNightMode(getNightMode());
    }
}
