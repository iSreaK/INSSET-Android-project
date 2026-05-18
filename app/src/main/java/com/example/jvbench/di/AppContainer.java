package com.example.jvbench.di;

import android.content.Context;

import com.example.jvbench.core.location.AndroidLocationProvider;
import com.example.jvbench.core.location.LocationProvider;
import com.example.jvbench.core.network.NetworkMonitor;
import com.example.jvbench.core.permissions.PermissionManager;
import com.example.jvbench.data.remote.supabase.SupabaseApiClient;
import com.example.jvbench.data.remote.supabase.SupabaseClientProvider;
import com.example.jvbench.data.remote.supabase.SupabaseRealtimeClient;
import com.example.jvbench.data.remote.supabase.SupabaseSessionStore;
import com.example.jvbench.data.repository.SupabaseAdminRepository;
import com.example.jvbench.data.repository.SupabaseAuthRepository;
import com.example.jvbench.data.repository.SupabaseBenchImageRepository;
import com.example.jvbench.data.repository.SupabaseBenchRepository;
import com.example.jvbench.data.repository.SupabaseReviewRepository;
import com.example.jvbench.domain.repository.AdminRepository;
import com.example.jvbench.domain.repository.AuthRepository;
import com.example.jvbench.domain.repository.BenchImageRepository;
import com.example.jvbench.domain.repository.BenchRepository;
import com.example.jvbench.domain.repository.ReviewRepository;

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
    }
}
