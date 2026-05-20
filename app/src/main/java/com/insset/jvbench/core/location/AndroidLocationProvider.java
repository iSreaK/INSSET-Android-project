package com.insset.jvbench.core.location;

import android.annotation.SuppressLint;
import android.content.Context;

import com.insset.jvbench.core.common.ResultCallback;
import com.insset.jvbench.domain.model.GeoPoint;
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
        // No hardcoded fallback: surface "no fix" as an error so the caller can
        // decide what to do (typically: keep France-wide view).
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location == null) {
                        callback.onError("no_fix");
                        return;
                    }
                    callback.onSuccess(new GeoPoint(location.getLatitude(), location.getLongitude()));
                })
                .addOnFailureListener(error -> callback.onError(error.getMessage()));
    }
}
