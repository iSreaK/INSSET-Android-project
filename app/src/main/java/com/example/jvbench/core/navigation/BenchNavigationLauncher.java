package com.example.jvbench.core.navigation;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.example.jvbench.core.service.BenchNavigationService;
import com.example.jvbench.domain.model.Bench;

/**
 * Single entry-point used by the UI to "navigate to a bench".
 *
 * <p>The work is split in two:</p>
 * <ol>
 *   <li>If the device has the required runtime permissions, kick off the
 *       {@link BenchNavigationService} so a foreground notification is shown
 *       and a 15m geofence around the target bench is registered. The
 *       geofence is what fires the "you have arrived, leave a review"
 *       notification later on.</li>
 *   <li>Then hand off to whichever external mapping app the user has
 *       installed (Google Maps, Waze, OsmAnd, ...) via
 *       {@link ExternalNavigation#openDirections}.</li>
 * </ol>
 *
 * <p>If permissions are missing we still launch the external navigation —
 * the geofence reminder is a nice-to-have, not a blocker.</p>
 */
public final class BenchNavigationLauncher {

    private BenchNavigationLauncher() {
        // Helper class.
    }

    public static void start(@NonNull Context context, @NonNull Bench bench) {
        if (hasRequiredPermissions(context)) {
            ContextCompat.startForegroundService(context,
                    BenchNavigationService.buildStartIntent(
                            context,
                            bench.getId(),
                            bench.getName() != null ? bench.getName() : "",
                            bench.getLatitude(),
                            bench.getLongitude()));
        }
        ExternalNavigation.openDirections(context,
                bench.getLatitude(), bench.getLongitude(),
                bench.getName());
    }

    /**
     * @return true when we can safely register a geofence and post a
     *         foreground notification. The check is conservative: any
     *         missing permission disables the background-tracking branch.
     */
    public static boolean hasRequiredPermissions(@NonNull Context context) {
        boolean fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        if (!fine) return false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            boolean bg = ContextCompat.checkSelfPermission(context,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED;
            if (!bg) return false;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(context,
                    Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }
}
