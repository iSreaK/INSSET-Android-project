package com.example.jvbench.core.network;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class NetworkBroadcastReceiver extends BroadcastReceiver {

    public interface ConnectivityListener {
        void onConnectivityChanged(boolean connected);
    }

    private final ConnectivityListener listener;
    private Boolean lastKnownState;

    public NetworkBroadcastReceiver(ConnectivityListener listener) {
        this.listener = listener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (listener == null) {
            return;
        }
        boolean connected = isConnected(context);
        if (lastKnownState != null && lastKnownState == connected) {
            return;
        }
        lastKnownState = connected;
        listener.onConnectivityChanged(connected);
    }

    public static boolean isConnected(Context context) {
        ConnectivityManager manager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (manager == null) {
            return false;
        }
        NetworkInfo info = manager.getActiveNetworkInfo();
        return info != null && info.isConnected();
    }
}
