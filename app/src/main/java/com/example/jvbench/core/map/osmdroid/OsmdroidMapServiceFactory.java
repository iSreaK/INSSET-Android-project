package com.example.jvbench.core.map.osmdroid;

import android.content.Context;

import androidx.annotation.NonNull;

import com.example.jvbench.core.map.MapService;
import com.example.jvbench.core.map.MapServiceFactory;

/**
 * Default {@link MapServiceFactory} implementation, producing
 * {@link OsmdroidMapService} instances. This is the single line of code that
 * needs to change to swap the rendering engine for the whole app.
 */
public class OsmdroidMapServiceFactory implements MapServiceFactory {
    @NonNull
    @Override
    public MapService create(@NonNull Context context) {
        return new OsmdroidMapService();
    }
}
