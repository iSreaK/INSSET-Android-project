package com.insset.jvbench.core.geofence;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.insset.jvbench.core.notification.AppNotifications;
import com.insset.jvbench.core.service.BenchNavigationService;
import com.insset.jvbench.di.App;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

/**
 * BroadcastReceiver invoked by Play Services when the user crosses a
 * registered geofence boundary. We only ever register a single ENTER
 * geofence per navigation session, so the handler is straightforward:
 *
 * <ul>
 *   <li>Read the bench info from the intent extras (we stored them in
 *       the PendingIntent when registering the geofence).</li>
 *   <li>Show the "you've arrived" notification.</li>
 *   <li>Remove the geofence so it cannot fire a second time.</li>
 *   <li>Stop the foreground navigation service.</li>
 * </ul>
 */
public class GeofenceTransitionReceiver extends BroadcastReceiver {
    private static final String TAG = "GeofenceRx";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;

        GeofencingEvent event = GeofencingEvent.fromIntent(intent);
        if (event == null || event.hasError()) {
            Log.w(TAG, "Bad geofencing event: " + (event != null ? event.getErrorCode() : "null"));
            return;
        }
        // We only registered an ENTER trigger but be defensive in case the
        // INITIAL_TRIGGER_ENTER flag flips the type later on.
        if (event.getGeofenceTransition() != Geofence.GEOFENCE_TRANSITION_ENTER) {
            return;
        }

        String benchId = intent.getStringExtra(GeofenceConstants.EXTRA_BENCH_ID);
        String benchName = intent.getStringExtra(GeofenceConstants.EXTRA_BENCH_NAME);
        if (benchId == null || benchId.isEmpty()) return;
        if (benchName == null) benchName = "";

        NotificationManager nm = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.notify(AppNotifications.NOTIF_ID_ARRIVAL,
                    AppNotifications.buildArrivalNotification(context, benchId, benchName));
        }

        // Clean up: pull the geofence and stop the foreground service.
        Context appCtx = context.getApplicationContext();
        if (appCtx instanceof App) {
            ((App) appCtx).getAppContainer().geofenceManager.removeArrivalGeofence();
        }
        Intent stopServiceIntent = new Intent(appCtx, BenchNavigationService.class);
        stopServiceIntent.setAction(BenchNavigationService.ACTION_STOP);
        // startForegroundService is fine here even for a stop intent: the
        // service grabs the action, calls stopSelf and the runtime handles
        // the rest. Using start* ensures the service is awake to receive it.
        ContextCompat.startForegroundService(appCtx, stopServiceIntent);
    }
}
