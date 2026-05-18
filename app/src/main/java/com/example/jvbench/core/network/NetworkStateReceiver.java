package com.example.jvbench.core.network;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * BroadcastReceiver implementation used by {@link NetworkMonitor} to react to
 * system connectivity broadcasts.
 *
 * <p>The receiver itself is intentionally thin: every broadcast simply asks
 * the monitor to recompute the current online state and dispatch it to its
 * listeners. We register dynamically (from {@link NetworkMonitor#start(Context)})
 * because implicit CONNECTIVITY broadcasts cannot be declared statically in
 * the manifest since Android 8.</p>
 */
public class NetworkStateReceiver extends BroadcastReceiver {
    private final NetworkMonitor monitor;

    public NetworkStateReceiver(NetworkMonitor monitor) {
        this.monitor = monitor;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        monitor.refreshFromBroadcast();
    }
}
