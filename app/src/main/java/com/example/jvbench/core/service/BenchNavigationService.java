package com.example.jvbench.core.service;

import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import com.example.jvbench.core.geofence.GeofenceConstants;
import com.example.jvbench.core.notification.AppNotifications;
import com.example.jvbench.di.App;

/**
 * Foreground service that keeps the "navigation to a bench" session alive in
 * the background.
 *
 * <p>What it does:</p>
 * <ul>
 *   <li>Posts a persistent notification so the OS allows the process to keep
 *       running while the user walks.</li>
 *   <li>Registers a 15m geofence around the target bench (via
 *       {@code AppContainer.geofenceManager}). The
 *       {@code GeofenceTransitionReceiver} fires the "you arrived"
 *       notification.</li>
 *   <li>Auto-stops when the geofence is consumed (the receiver re-broadcasts
 *       an {@link #ACTION_STOP} intent) or when the user dismisses the
 *       persistent notification.</li>
 * </ul>
 *
 * <p>The service exists so that Android lets the geofence keep monitoring
 * even when the launcher app (Maps / Waze) is in the foreground and our
 * own UI has been hidden — without it, modern Android would aggressively
 * kill the process and silently drop the geofence.</p>
 */
public class BenchNavigationService extends Service {
    private static final String TAG = "BenchNavService";

    public static final String ACTION_START = "com.example.jvbench.service.NAV_START";
    public static final String ACTION_STOP = "com.example.jvbench.service.NAV_STOP";

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            stopSelf();
            return START_NOT_STICKY;
        }
        String action = intent.getAction();
        if (ACTION_STOP.equals(action)) {
            stopForeground(STOP_FOREGROUND_REMOVE);
            stopSelf(startId);
            return START_NOT_STICKY;
        }
        if (!ACTION_START.equals(action)) {
            stopSelf(startId);
            return START_NOT_STICKY;
        }

        String benchId = intent.getStringExtra(GeofenceConstants.EXTRA_BENCH_ID);
        String benchName = intent.getStringExtra(GeofenceConstants.EXTRA_BENCH_NAME);
        double latitude = intent.getDoubleExtra(GeofenceConstants.EXTRA_BENCH_LAT, Double.NaN);
        double longitude = intent.getDoubleExtra(GeofenceConstants.EXTRA_BENCH_LNG, Double.NaN);

        if (benchId == null || Double.isNaN(latitude) || Double.isNaN(longitude)) {
            Log.w(TAG, "Missing extras, stopping immediately");
            stopSelf(startId);
            return START_NOT_STICKY;
        }
        if (benchName == null) benchName = "";

        // Promote to foreground BEFORE doing any long work so the platform
        // doesn't kill the service for not calling startForeground in time.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(AppNotifications.NOTIF_ID_NAVIGATION,
                    AppNotifications.buildNavigationNotification(this, benchId, benchName),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
        } else {
            startForeground(AppNotifications.NOTIF_ID_NAVIGATION,
                    AppNotifications.buildNavigationNotification(this, benchId, benchName));
        }

        Object appObj = getApplicationContext();
        if (appObj instanceof App) {
            ((App) appObj).getAppContainer().geofenceManager
                    .registerArrivalGeofence(benchId, benchName, latitude, longitude);
        }
        return START_REDELIVER_INTENT;
    }

    @Override
    public void onDestroy() {
        // Defensive cleanup if the OS kills the service without going through
        // ACTION_STOP — leave no dangling geofence.
        Object appObj = getApplicationContext();
        if (appObj instanceof App) {
            ((App) appObj).getAppContainer().geofenceManager.removeArrivalGeofence();
        }
        super.onDestroy();
    }

    /**
     * Helper for callers: builds the ACTION_START intent with the right extras.
     */
    public static Intent buildStartIntent(android.content.Context context,
                                          String benchId, String benchName,
                                          double latitude, double longitude) {
        Intent intent = new Intent(context, BenchNavigationService.class);
        intent.setAction(ACTION_START);
        intent.putExtra(GeofenceConstants.EXTRA_BENCH_ID, benchId);
        intent.putExtra(GeofenceConstants.EXTRA_BENCH_NAME, benchName);
        intent.putExtra(GeofenceConstants.EXTRA_BENCH_LAT, latitude);
        intent.putExtra(GeofenceConstants.EXTRA_BENCH_LNG, longitude);
        return intent;
    }
}
