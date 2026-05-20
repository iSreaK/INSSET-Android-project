package com.insset.jvbench.core.geofence;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;

import java.util.Collections;

/**
 * Thin wrapper around {@link GeofencingClient}. Hides the boilerplate around
 * building geofences, configuring the PendingIntent and handling missing
 * permissions.
 *
 * <p>Lifecycle: one instance lives in {@code AppContainer}. Callers register
 * and remove geofences as the user starts / finishes navigating to a bench.</p>
 */
public class GeofenceManager {
    private static final String TAG = "GeofenceManager";

    private final Context appContext;
    private final GeofencingClient geofencingClient;

    public GeofenceManager(Context context) {
        this.appContext = context.getApplicationContext();
        this.geofencingClient = LocationServices.getGeofencingClient(appContext);
    }

    /**
     * Registers a single geofence around the bench identified by the extras.
     *
     * <p>If the required runtime permissions ({@code ACCESS_FINE_LOCATION} on
     * all versions, plus {@code ACCESS_BACKGROUND_LOCATION} on Q+) are
     * missing, the method returns {@code false} so the caller can decide
     * whether to fall back on in-foreground polling.</p>
     */
    @SuppressLint("MissingPermission")
    public boolean registerArrivalGeofence(@NonNull String benchId, @NonNull String benchName,
                                           double latitude, double longitude) {
        if (!hasRequiredPermissions()) {
            Log.w(TAG, "Cannot register geofence: missing location permissions");
            return false;
        }

        Geofence geofence = new Geofence.Builder()
                .setRequestId(GeofenceConstants.GEOFENCE_REQUEST_ID)
                .setCircularRegion(latitude, longitude, GeofenceConstants.ARRIVAL_RADIUS_METERS)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                // INITIAL_TRIGGER_ENTER fires immediately if the user is
                // already inside the radius when we register the geofence
                // (useful when the user starts navigation when already close).
                .build();

        GeofencingRequest request = new GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofences(Collections.singletonList(geofence))
                .build();

        try {
            geofencingClient.addGeofences(request, buildPendingIntent(benchId, benchName, latitude, longitude))
                    .addOnFailureListener(e -> Log.e(TAG, "addGeofences failed: " + e.getMessage()));
            return true;
        } catch (SecurityException e) {
            // Last-resort guard: hasRequiredPermissions should have caught this.
            Log.e(TAG, "SecurityException registering geofence", e);
            return false;
        }
    }

    /** Removes the (single) currently-registered geofence. Safe to call when none is set. */
    public void removeArrivalGeofence() {
        geofencingClient.removeGeofences(
                Collections.singletonList(GeofenceConstants.GEOFENCE_REQUEST_ID));
    }

    private boolean hasRequiredPermissions() {
        boolean fine = ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        if (!fine) return false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    private PendingIntent buildPendingIntent(String benchId, String benchName, double lat, double lng) {
        Intent intent = new Intent(appContext, GeofenceTransitionReceiver.class);
        intent.setAction(GeofenceConstants.ACTION_GEOFENCE_EVENT);
        intent.putExtra(GeofenceConstants.EXTRA_BENCH_ID, benchId);
        intent.putExtra(GeofenceConstants.EXTRA_BENCH_NAME, benchName);
        intent.putExtra(GeofenceConstants.EXTRA_BENCH_LAT, lat);
        intent.putExtra(GeofenceConstants.EXTRA_BENCH_LNG, lng);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        // FLAG_MUTABLE is required since API 31 for geofence PendingIntents,
        // because Play Services needs to add the transition data to the intent.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            flags |= PendingIntent.FLAG_MUTABLE;
        }
        return PendingIntent.getBroadcast(appContext, 0, intent, flags);
    }
}
