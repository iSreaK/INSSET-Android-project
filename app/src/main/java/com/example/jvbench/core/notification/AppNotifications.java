package com.example.jvbench.core.notification;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.example.jvbench.R;
import com.example.jvbench.core.navigation.NavConstants;
import com.example.jvbench.ui.main.MainActivity;

/**
 * Central place for the app's notification channels and notification builders.
 *
 * <p>We expose two channels:</p>
 * <ul>
 *   <li>{@link #CHANNEL_NAVIGATION} — low priority, used for the persistent
 *       notification of the {@code BenchNavigationService} (the user is
 *       walking, they don't need a sound).</li>
 *   <li>{@link #CHANNEL_ARRIVAL} — high priority, used to alert the user when
 *       they enter the geofence around the target bench.</li>
 * </ul>
 */
public final class AppNotifications {

    public static final String CHANNEL_NAVIGATION = "jvbench_navigation";
    public static final String CHANNEL_ARRIVAL = "jvbench_arrival";

    public static final int NOTIF_ID_NAVIGATION = 4201;
    public static final int NOTIF_ID_ARRIVAL = 4202;

    private AppNotifications() {
        // Utility class.
    }

    /** Idempotent. Safe to call from {@code App.onCreate()} or from any service start. */
    public static void ensureChannels(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null) return;

        if (manager.getNotificationChannel(CHANNEL_NAVIGATION) == null) {
            NotificationChannel navChannel = new NotificationChannel(
                    CHANNEL_NAVIGATION,
                    context.getString(R.string.channel_navigation_name),
                    NotificationManager.IMPORTANCE_LOW);
            navChannel.setDescription(context.getString(R.string.channel_navigation_desc));
            manager.createNotificationChannel(navChannel);
        }
        if (manager.getNotificationChannel(CHANNEL_ARRIVAL) == null) {
            NotificationChannel arrivalChannel = new NotificationChannel(
                    CHANNEL_ARRIVAL,
                    context.getString(R.string.channel_arrival_name),
                    NotificationManager.IMPORTANCE_HIGH);
            arrivalChannel.setDescription(context.getString(R.string.channel_arrival_desc));
            manager.createNotificationChannel(arrivalChannel);
        }
    }

    /**
     * The persistent notification displayed while the user is walking to a bench.
     * Tapping it brings the app to the foreground on the bench detail screen.
     */
    public static Notification buildNavigationNotification(Context context, String benchId, String benchName) {
        ensureChannels(context);
        PendingIntent contentIntent = openBenchDetailIntent(context, benchId);
        return new NotificationCompat.Builder(context, CHANNEL_NAVIGATION)
                .setSmallIcon(R.drawable.ic_app_logo)
                .setContentTitle(context.getString(R.string.notif_navigation_title))
                .setContentText(context.getString(R.string.notif_navigation_text, benchName))
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentIntent(contentIntent)
                .setCategory(NotificationCompat.CATEGORY_NAVIGATION)
                .build();
    }

    /**
     * The "you have arrived, leave a review" notification. Built as a one-shot
     * high-importance notification with a deep-link that opens the review form
     * for the target bench.
     */
    public static Notification buildArrivalNotification(Context context, String benchId, String benchName) {
        ensureChannels(context);
        PendingIntent contentIntent = openBenchDetailIntent(context, benchId);
        return new NotificationCompat.Builder(context, CHANNEL_ARRIVAL)
                .setSmallIcon(R.drawable.ic_app_logo)
                .setContentTitle(context.getString(R.string.notif_arrival_title, benchName))
                .setContentText(context.getString(R.string.notif_arrival_text))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(contentIntent)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .build();
    }

    private static PendingIntent openBenchDetailIntent(Context context, String benchId) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        // Make the PendingIntent unique per bench so the system doesn't share
        // one cached extra-set across different notifications.
        intent.setData(Uri.parse("jvbench://bench/" + benchId));
        intent.putExtra(NavConstants.ARG_BENCH_ID, benchId);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        return PendingIntent.getActivity(context, 0, intent, flags);
    }
}
