package com.example.jvbench.core.location;

import com.example.jvbench.domain.model.GeoPoint;
import com.example.jvbench.core.common.ResultCallback;

public interface LocationProvider {
    void getLastKnownLocation(ResultCallback<GeoPoint> callback);
}
