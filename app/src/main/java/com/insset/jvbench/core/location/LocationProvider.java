package com.insset.jvbench.core.location;

import com.insset.jvbench.domain.model.GeoPoint;
import com.insset.jvbench.core.common.ResultCallback;

public interface LocationProvider {
    void getLastKnownLocation(ResultCallback<GeoPoint> callback);
}
