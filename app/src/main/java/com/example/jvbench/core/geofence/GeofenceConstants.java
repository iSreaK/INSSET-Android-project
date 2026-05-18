package com.example.jvbench.core.geofence;

/**
 * Constants shared between the geofence manager, the broadcast receiver and
 * the foreground service. Centralized so radius / extras names don't drift.
 */
public final class GeofenceConstants {
    /** Radius (meters) of the geofence registered around the target bench. */
    public static final float ARRIVAL_RADIUS_METERS = 15f;

    /** Single geofence ID — we only ever track one target bench at a time. */
    public static final String GEOFENCE_REQUEST_ID = "jvbench_arrival_target";

    /** Action used by the PendingIntent that wires GeofencingClient to our receiver. */
    public static final String ACTION_GEOFENCE_EVENT = "com.example.jvbench.action.GEOFENCE_EVENT";

    public static final String EXTRA_BENCH_ID = "extra_bench_id";
    public static final String EXTRA_BENCH_NAME = "extra_bench_name";
    public static final String EXTRA_BENCH_LAT = "extra_bench_lat";
    public static final String EXTRA_BENCH_LNG = "extra_bench_lng";

    private GeofenceConstants() {
        // Constants class.
    }
}
