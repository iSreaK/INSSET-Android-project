package com.insset.jvbench.di;

import android.content.Context;

import com.insset.jvbench.core.geofence.GeofenceManager;
import com.insset.jvbench.core.location.AndroidLocationProvider;
import com.insset.jvbench.core.location.LocationProvider;
import com.insset.jvbench.core.map.MapServiceFactory;
import com.insset.jvbench.core.map.osmdroid.OsmdroidMapServiceFactory;
import com.insset.jvbench.core.network.NetworkMonitor;
import com.insset.jvbench.core.permissions.PermissionManager;
import com.insset.jvbench.data.remote.supabase.SupabaseApiClient;
import com.insset.jvbench.data.remote.supabase.SupabaseClientProvider;
import com.insset.jvbench.data.remote.supabase.SupabaseRealtimeClient;
import com.insset.jvbench.data.remote.supabase.SupabaseSessionStore;
import com.insset.jvbench.data.repository.SupabaseAdminRepository;
import com.insset.jvbench.data.repository.SupabaseAuthRepository;
import com.insset.jvbench.data.repository.SupabaseBenchImageRepository;
import com.insset.jvbench.data.repository.SupabaseBenchRepository;
import com.insset.jvbench.data.repository.SupabaseReviewRepository;
import com.insset.jvbench.domain.repository.AdminRepository;
import com.insset.jvbench.domain.repository.AuthRepository;
import com.insset.jvbench.domain.repository.BenchImageRepository;
import com.insset.jvbench.domain.repository.BenchRepository;
import com.insset.jvbench.domain.repository.ReviewRepository;

public class AppContainer {
    public final SupabaseClientProvider supabaseClientProvider;
    public final SupabaseSessionStore supabaseSessionStore;
    public final SupabaseApiClient supabaseApiClient;
    public final SupabaseRealtimeClient supabaseRealtimeClient;
    public final AuthRepository authRepository;
    public final BenchRepository benchRepository;
    public final BenchImageRepository benchImageRepository;
    public final ReviewRepository reviewRepository;
    public final AdminRepository adminRepository;
    public final LocationProvider locationProvider;
    public final PermissionManager permissionManager;
    public final NetworkMonitor networkMonitor;
    public final GeofenceManager geofenceManager;
    /**
     * The active map engine binding. To swap OpenStreetMap for Google Maps
     * (or any other provider), implement {@link MapServiceFactory} and bind
     * it here — the UI layer never references a concrete map type.
     */
    public final MapServiceFactory mapServiceFactory;

    public AppContainer(Context context) {
        supabaseClientProvider = new SupabaseClientProvider();
        supabaseSessionStore = new SupabaseSessionStore(context);
        supabaseApiClient = new SupabaseApiClient(supabaseClientProvider, supabaseSessionStore);
        supabaseRealtimeClient = new SupabaseRealtimeClient(supabaseClientProvider, supabaseSessionStore);
        authRepository = new SupabaseAuthRepository(supabaseClientProvider, supabaseApiClient, supabaseSessionStore);
        benchRepository = new SupabaseBenchRepository(supabaseClientProvider, supabaseApiClient);
        benchImageRepository = new SupabaseBenchImageRepository(supabaseClientProvider, supabaseApiClient);
        reviewRepository = new SupabaseReviewRepository(supabaseClientProvider, supabaseApiClient);
        adminRepository = new SupabaseAdminRepository(supabaseClientProvider, supabaseApiClient);
        locationProvider = new AndroidLocationProvider(context);
        permissionManager = new PermissionManager();
        networkMonitor = new NetworkMonitor(context);
        geofenceManager = new GeofenceManager(context);
        mapServiceFactory = new OsmdroidMapServiceFactory();
    }
}
