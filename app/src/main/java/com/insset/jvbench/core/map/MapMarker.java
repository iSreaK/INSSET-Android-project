package com.insset.jvbench.core.map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.insset.jvbench.domain.model.GeoPoint;

/**
 * Map-provider-agnostic representation of a marker the UI wants to display.
 *
 * <p>Each {@link MapMarker} is built from a domain object (a {@link com.insset.jvbench.domain.model.Bench}
 * typically) and handed to a {@link MapService} implementation. The
 * implementation is in charge of converting it to whatever native type the
 * underlying map library uses (osmdroid {@code Marker}, Google Maps
 * {@code MarkerOptions}, MapLibre symbol, ...).</p>
 */
public final class MapMarker {
    private final String id;
    private final GeoPoint position;
    private final String title;
    @Nullable
    private final String snippet;
    @Nullable
    private final Object payload;

    public MapMarker(@NonNull String id,
                     @NonNull GeoPoint position,
                     @NonNull String title,
                     @Nullable String snippet,
                     @Nullable Object payload) {
        this.id = id;
        this.position = position;
        this.title = title;
        this.snippet = snippet;
        this.payload = payload;
    }

    @NonNull public String getId() { return id; }
    @NonNull public GeoPoint getPosition() { return position; }
    @NonNull public String getTitle() { return title; }
    @Nullable public String getSnippet() { return snippet; }
    /** Optional ad-hoc data attached by the caller (used by the Fragment to recover the source domain object). */
    @Nullable public Object getPayload() { return payload; }
}
