package com.example.jvbench.core.navigation;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.jvbench.R;

import java.util.Locale;

/**
 * Helpers to delegate turn-by-turn navigation to whichever mapping app the
 * user has installed (Google Maps, Waze, OsmAnd, ...).
 *
 * <p>We build an {@code Intent.ACTION_VIEW} with the standard {@code geo:}
 * URI scheme; Android then shows the user the chooser if multiple apps can
 * handle it, or opens the single one available. This is the simplest way to
 * satisfy the "navigation vers un événement" requirement without rolling our
 * own routing.</p>
 */
public final class ExternalNavigation {

    private ExternalNavigation() {
        // Utility class — not instantiable.
    }

    /**
     * Launches an external navigation app pointing at the given coordinates.
     *
     * @param context  any {@link Context}; preferably from a hosting Activity
     *                 so that the system can use the foreground task stack.
     * @param latitude target latitude (decimal degrees, WGS-84)
     * @param longitude target longitude
     * @param label    optional human-readable label appended to the URI so the
     *                 destination shows up named (e.g. the bench name).
     * @return {@code true} if a navigation app accepted the intent, {@code false} otherwise.
     */
    public static boolean openDirections(@NonNull Context context, double latitude, double longitude, @Nullable String label) {
        Uri uri = buildGeoUri(latitude, longitude, label);
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            context.startActivity(intent);
            return true;
        } catch (Exception e) {
            // No app on the device can handle geo: URIs.
            Toast.makeText(context, R.string.error_no_navigation_app, Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    private static Uri buildGeoUri(double latitude, double longitude, @Nullable String label) {
        // "geo:0,0?q=lat,lng(Label)" is the documented format that triggers
        // a search pin with a named marker. The leading 0,0 is intentional:
        // it tells the receiving app to use the q= parameter as the focus
        // point rather than centering on the leading coordinates.
        String coords = String.format(Locale.US, "%.6f,%.6f", latitude, longitude);
        StringBuilder builder = new StringBuilder("geo:0,0?q=").append(coords);
        if (label != null && !label.isBlank()) {
            builder.append('(').append(Uri.encode(label)).append(')');
        }
        return Uri.parse(builder.toString());
    }
}
