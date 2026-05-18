package com.example.jvbench.core.network;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.CopyOnWriteArrayList;

/**
 * App-wide observer of network connectivity.
 *
 * <p>Wraps a {@link NetworkStateReceiver} (which the project intentionally uses
 * to satisfy the "BroadcastReceiver" requirement) plus a synchronous
 * connectivity check based on {@link NetworkCapabilities}. Consumers register
 * a {@link Listener} and get notified whenever the device transitions between
 * online and offline.</p>
 *
 * <p>Lifecycle: a single instance is created in {@code AppContainer} and started
 * from {@link com.example.jvbench.di.App#onCreate()}. The receiver lives for
 * the whole process. Listeners are weakly coupled and free to come and go.</p>
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

    private volatile boolean online;
    private boolean started;

    public NetworkMonitor(Context context) {
        this.appContext = context.getApplicationContext();
        this.connectivityManager = (ConnectivityManager) appContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        this.receiver = new NetworkStateReceiver(this);
        this.online = computeOnline();
    }

    /** Registers the BroadcastReceiver. Safe to call multiple times. */
    @SuppressWarnings("deprecation")
    public synchronized void start() {
        if (started) return;
        IntentFilter filter = new IntentFilter();
        // CONNECTIVITY_ACTION is deprecated but still delivered when registered
        // dynamically and is the broadcast the PDF expects to see used.
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        filter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        appContext.registerReceiver(receiver, filter);
        started = true;
    }

    public synchronized void stop() {
        if (!started) return;
        try {
            appContext.unregisterReceiver(receiver);
        } catch (IllegalArgumentException ignored) {
            // Already unregistered; safe to ignore.
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
        boolean previous = online;
        boolean current = computeOnline();
        online = current;
        if (previous != current) {
            for (Listener l : listeners) {
                mainHandler.post(() -> l.onConnectivityChanged(current));
            }
        }
    }

    private boolean computeOnline() {
        if (connectivityManager == null) return false;
        Network active = connectivityManager.getActiveNetwork();
        if (active == null) return false;
        NetworkCapabilities caps = connectivityManager.getNetworkCapabilities(active);
        if (caps == null) return false;
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
    }
}
