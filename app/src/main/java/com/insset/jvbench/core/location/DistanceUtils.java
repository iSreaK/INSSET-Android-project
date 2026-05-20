package com.insset.jvbench.core.location;

/**
 * Pure-Java geographic helpers. Kept dependency-free so the same code can run
 * in unit tests, ViewModels, and background services without dragging Android
 * imports along.
 */
public final class DistanceUtils {
    /** Mean Earth radius in meters. Good enough for terrestrial distances under a few thousand kilometers. */
    private static final double EARTH_RADIUS_METERS = 6_371_000d;

    private DistanceUtils() {
        // Utility class — not instantiable.
    }

    /**
     * Great-circle distance between two WGS-84 coordinates, expressed in
     * meters. Uses the Haversine formula, which is accurate to better than
     * 0.5% for any pair of points on Earth.
     */
    public static double haversineMeters(double lat1, double lng1, double lat2, double lng2) {
        double phi1 = Math.toRadians(lat1);
        double phi2 = Math.toRadians(lat2);
        double deltaPhi = Math.toRadians(lat2 - lat1);
        double deltaLambda = Math.toRadians(lng2 - lng1);

        double a = Math.sin(deltaPhi / 2) * Math.sin(deltaPhi / 2)
                + Math.cos(phi1) * Math.cos(phi2)
                * Math.sin(deltaLambda / 2) * Math.sin(deltaLambda / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_METERS * c;
    }
}
