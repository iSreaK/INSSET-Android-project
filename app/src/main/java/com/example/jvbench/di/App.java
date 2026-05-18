package com.example.jvbench.di;

import android.app.Application;

import com.example.jvbench.core.theme.ThemePreferences;

public class App extends Application {
    private AppContainer appContainer;
    private ThemePreferences themePreferences;

    @Override
    public void onCreate() {
        super.onCreate();
        themePreferences = new ThemePreferences(this);
        themePreferences.apply();
        appContainer = new AppContainer(this);
        // Start the connectivity broadcast receiver for the whole process
        // lifetime. The monitor is then queried/observed by UI screens.
        appContainer.networkMonitor.start();
    }

    public AppContainer getAppContainer() {
        return appContainer;
    }

    public ThemePreferences getThemePreferences() {
        return themePreferences;
    }
}
