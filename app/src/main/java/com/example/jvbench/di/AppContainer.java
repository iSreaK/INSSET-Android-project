package com.example.jvbench.di;

import android.content.Context;

import com.example.jvbench.core.location.AndroidLocationProvider;
import com.example.jvbench.core.location.LocationProvider;
import com.example.jvbench.core.permissions.PermissionManager;
import com.example.jvbench.data.remote.supabase.SupabaseApiClient;
import com.example.jvbench.data.remote.supabase.SupabaseClientProvider;
import com.example.jvbench.data.remote.supabase.SupabaseSessionStore;
import com.example.jvbench.data.repository.SupabaseAuthRepository;
import com.example.jvbench.data.repository.SupabaseBenchRepository;
import com.example.jvbench.data.repository.SupabaseReviewRepository;
import com.example.jvbench.domain.repository.AuthRepository;
import com.example.jvbench.domain.repository.BenchRepository;
import com.example.jvbench.domain.repository.ReviewRepository;

public class AppContainer {
    public final SupabaseClientProvider supabaseClientProvider;
    public final SupabaseSessionStore supabaseSessionStore;
    public final SupabaseApiClient supabaseApiClient;
    public final AuthRepository authRepository;
    public final BenchRepository benchRepository;
    public final ReviewRepository reviewRepository;
    public final LocationProvider locationProvider;
    public final PermissionManager permissionManager;

    public AppContainer(Context context) {
        supabaseClientProvider = new SupabaseClientProvider();
        supabaseSessionStore = new SupabaseSessionStore();
        supabaseApiClient = new SupabaseApiClient(supabaseClientProvider, supabaseSessionStore);
        authRepository = new SupabaseAuthRepository(supabaseClientProvider, supabaseApiClient, supabaseSessionStore);
        benchRepository = new SupabaseBenchRepository(supabaseClientProvider, supabaseApiClient);
        reviewRepository = new SupabaseReviewRepository(supabaseClientProvider, supabaseApiClient);
        locationProvider = new AndroidLocationProvider(context);
        permissionManager = new PermissionManager();
    }
}
