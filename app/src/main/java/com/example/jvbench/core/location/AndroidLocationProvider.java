package com.example.jvbench.core.location;

import android.annotation.SuppressLint;
import android.content.Context;

import com.example.jvbench.core.common.ResultCallback;
import com.example.jvbench.domain.model.GeoPoint;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

public class AndroidLocationProvider implements LocationProvider {
    private final FusedLocationProviderClient fusedLocationClient;

    public AndroidLocationProvider(Context context) {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
    }

    @SuppressLint("MissingPermission")
    @Override
    public void getLastKnownLocation(ResultCallback<GeoPoint> callback) {
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location == null) {
                        callback.onSuccess(new GeoPoint(49.8941, 2.2958));
                        return;
                    }
                    callback.onSuccess(new GeoPoint(location.getLatitude(), location.getLongitude()));
                })
                .addOnFailureListener(error -> callback.onError(error.getMessage()));
    }
}
