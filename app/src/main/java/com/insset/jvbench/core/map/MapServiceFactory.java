package com.insset.jvbench.core.map;

import android.content.Context;

import androidx.annotation.NonNull;

/**
 * Single seam between the DI container and the active map provider.
 * Swapping providers (OSM ↔ Google Maps) is done by binding a different
 * implementation of this factory in {@code AppContainer}.
 */
public interface MapServiceFactory {
    @NonNull
    MapService create(@NonNull Context context);
}
