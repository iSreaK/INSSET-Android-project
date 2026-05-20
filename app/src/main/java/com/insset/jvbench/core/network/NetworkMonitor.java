package com.insset.jvbench.core.network;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import java.util.concurrent.CopyOnWriteArrayList;

/**
 * App-wide observer of network connectivity.
 *
 * <p>Two complementary mechanisms are used:</p>
 * <ul>
 *   <li>A {@link NetworkStateReceiver} (dynamic BroadcastReceiver) listening
 *       to the legacy {@code CONNECTIVITY_ACTION} and
 *       {@code ACTION_AIRPLANE_MODE_CHANGED}. Kept to satisfy the PDF's
 *       "BroadcastReceiver" Option-2 requirement.</li>
 *   <li>A {@link ConnectivityManager.NetworkCallback} registered on the
 *       default network (API 24+). It delivers
 *       {@code onAvailable / onLost / onCapabilitiesChanged} essentially
 *       instantly and reliably, where the legacy broadcasts can be delayed
 *       by 5–15 seconds on Android 10+ (especially when coming out of
 *       airplane mode — the broadcast arrives early, but the network is
 *       only validated after a successful Google captive-portal probe).</li>
 * </ul>
 *
 * <p>The connectivity check intentionally does NOT require
 * {@code NET_CAPABILITY_VALIDATED}: that flag is only set once Android
 * has run its own HTTP probe, which adds the same 5–15 s lag. For our use
 * case (Supabase requests, image downloads) {@code NET_CAPABILITY_INTERNET}
 * is enough — the worst case is a failed request that the user can retry,
 * which is much better UX than buttons stuck in the disabled state.</p>
 *
 * <p>Lifecycle: a single instance is created in {@code AppContainer} and
 * started from {@link com.insset.jvbench.di.App#onCreate()}. Both the
 * receiver and the network callback live for the whole process. Listeners
 * are weakly coupled and free to come and go.</p>
 */
public class NetworkMonitor {

    /** Callback invoked on the main thread whenever the online state flips. */
    public interface Listener {
        void onConnectivityChanged(boolean online);
    }

    private final Context appContext;
    private final ConnectivityManager connectivityManager;
    private final NetworkStateReceiver receiver;
    private final CopyOnWriteArrayList<Listener> listeners = new CopyOnWriteArrayList<>();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    /** Reacts to default-network changes much faster than CONNECTIVITY_ACTION. */
    private final ConnectivityManager.NetworkCallback networkCallback = new ConnectivityManager.NetworkCallback() {
        @Override
        public void onAvailable(@NonNull Network network) {
            updateState(true);
        }

        @Override
        public void onLost(@NonNull Network network) {
            // Force a fresh check rather than blindly flipping to false:
            // there might be another usable network (e.g. mobile data when
            // wifi is dropped).
            updateState(computeOnline());
        }

        @Override
        public void onCapabilitiesChanged(@NonNull Network network, @NonNull NetworkCapabilities caps) {
            updateState(caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET));
        }
    };

    private volatile boolean online;
    private boolean started;

    public NetworkMonitor(Context context) {
        this.appContext = context.getApplicationContext();
        this.connectivityManager = (ConnectivityManager) appContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        this.receiver = new NetworkStateReceiver(this);
        this.online = computeOnline();
    }

    /** Registers the BroadcastReceiver and the NetworkCallback. Safe to call multiple times. */
    @SuppressWarnings("deprecation")
    public synchronized void start() {
        if (started) return;
        IntentFilter filter = new IntentFilter();
        // CONNECTIVITY_ACTION is deprecated but still delivered when registered
        // dynamically and is the broadcast the PDF expects to see used.
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        filter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        appContext.registerReceiver(receiver, filter);

        // Primary signal: the network callback. Fires within ~tens of ms of
        // a real connectivity change. minSdk is 24, so registerDefaultNetworkCallback
        // is always available.
        if (connectivityManager != null) {
            try {
                connectivityManager.registerDefaultNetworkCallback(networkCallback);
            } catch (RuntimeException ignored) {
                // Some OEM builds throw when registering on app start with
                // no network — non-fatal, the broadcast receiver will pick
                // up the next change.
            }
        }
        started = true;
    }

    public synchronized void stop() {
        if (!started) return;
        try {
            appContext.unregisterReceiver(receiver);
        } catch (IllegalArgumentException ignored) {
            // Already unregistered; safe to ignore.
        }
        if (connectivityManager != null) {
            try {
                connectivityManager.unregisterNetworkCallback(networkCallback);
            } catch (IllegalArgumentException ignored) {
                // Not registered; safe to ignore.
            }
        }
        started = false;
    }

    public boolean isOnline() {
        return online;
    }

    public void addListener(Listener listener) {
        if (listener == null) return;
        listeners.addIfAbsent(listener);
        // Push the current value immediately so callers don't have to wait
        // for the next broadcast to render the right UI state.
        mainHandler.post(() -> listener.onConnectivityChanged(online));
    }

    public void removeListener(Listener listener) {
        if (listener == null) return;
        listeners.remove(listener);
    }

    /** Called by the receiver whenever a relevant broadcast arrives. */
    void refreshFromBroadcast() {
        updateState(computeOnline());
    }

    /**
     * Centralized state transition. Diffs against the previous value and
     * fans out to listeners on the main thread only when something changed.
     */
    private void updateState(boolean nextOnline) {
        boolean previous = online;
        online = nextOnline;
        if (previous != nextOnline) {
            for (Listener l : listeners) {
                mainHandler.post(() -> l.onConnectivityChanged(nextOnline));
            }
        }
    }

    private boolean computeOnline() {
        if (connectivityManager == null) return false;
        Network active = connectivityManager.getActiveNetwork();
        if (active == null) return false;
        NetworkCapabilities caps = connectivityManager.getNetworkCapabilities(active);
        if (caps == null) return false;
        // We deliberately do NOT require NET_CAPABILITY_VALIDATED here. That
        // flag is only set after Android's own captive-portal HTTP probe
        // succeeds, which adds a 5–15 s lag on reconnection. Allowing the
        // INTERNET-only state lets the UI re-enable instantly; a request
        // that ends up failing is recoverable, a button stuck disabled is
        // not from the user's point of view.
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }
}
