package com.example.jvbench.di;

import android.app.Application;

import com.example.jvbench.core.notification.AppNotifications;
import com.example.jvbench.core.sync.BenchSyncScheduler;
import com.example.jvbench.core.theme.ThemePreferences;

public class App extends Application {
    private AppContainer appContainer;
    private ThemePreferences themePreferences;
    private BenchSyncScheduler benchSyncScheduler;

    @Override
    public void onCreate() {
        super.onCreate();
        themePreferences = new ThemePreferences(this);
        themePreferences.apply();
        appContainer = new AppContainer(this);
        // Start the connectivity broadcast receiver for the whole process
        // lifetime. The monitor is then queried/observed by UI screens.
        appContainer.networkMonitor.start();
        // Ensure notification channels exist before anything tries to post
        // a notification, and start the periodic in-process Supabase sync.
        AppNotifications.ensureChannels(this);
        benchSyncScheduler = new BenchSyncScheduler(appContainer.benchRepository);
        benchSyncScheduler.start();
    }

    public AppContainer getAppContainer() {
        return appContainer;
    }

    public ThemePreferences getThemePreferences() {
        return themePreferences;
    }
}
