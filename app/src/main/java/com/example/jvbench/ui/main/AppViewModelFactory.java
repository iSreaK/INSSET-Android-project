package com.example.jvbench.ui.main;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.jvbench.di.AppContainer;
import com.example.jvbench.ui.auth.LoginViewModel;
import com.example.jvbench.ui.auth.RegisterViewModel;
import com.example.jvbench.ui.account.AccountViewModel;
import com.example.jvbench.ui.benchdetail.BenchDetailViewModel;
import com.example.jvbench.ui.benchform.BenchFormViewModel;
import com.example.jvbench.ui.map.MapViewModel;
import com.example.jvbench.ui.admin.AdminViewModel;
import com.example.jvbench.ui.mybenches.MyBenchesViewModel;
import com.example.jvbench.ui.nearby.NearbyBenchesViewModel;
import com.example.jvbench.ui.reviewform.ReviewFormViewModel;

public class AppViewModelFactory implements ViewModelProvider.Factory {
    private final AppContainer container;

    public AppViewModelFactory(AppContainer container) {
        this.container = container;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(LoginViewModel.class)) {
            return (T) new LoginViewModel(container.authRepository);
        }
        if (modelClass.isAssignableFrom(RegisterViewModel.class)) {
            return (T) new RegisterViewModel(container.authRepository);
        }
        if (modelClass.isAssignableFrom(AccountViewModel.class)) {
            return (T) new AccountViewModel(container.authRepository);
        }
        if (modelClass.isAssignableFrom(MapViewModel.class)) {
            return (T) new MapViewModel(container.benchRepository, container.locationProvider);
        }
        if (modelClass.isAssignableFrom(BenchFormViewModel.class)) {
            return (T) new BenchFormViewModel(container.benchRepository, container.benchImageRepository, container.authRepository);
        }
        if (modelClass.isAssignableFrom(BenchDetailViewModel.class)) {
            return (T) new BenchDetailViewModel(container.benchRepository, container.benchImageRepository, container.reviewRepository, container.authRepository, container.locationProvider);
        }
        if (modelClass.isAssignableFrom(MyBenchesViewModel.class)) {
            return (T) new MyBenchesViewModel(container.benchRepository, container.authRepository);
        }
        if (modelClass.isAssignableFrom(ReviewFormViewModel.class)) {
            return (T) new ReviewFormViewModel(container.reviewRepository, container.authRepository);
        }
        if (modelClass.isAssignableFrom(AdminViewModel.class)) {
            return (T) new AdminViewModel(container.adminRepository);
        }
        if (modelClass.isAssignableFrom(NearbyBenchesViewModel.class)) {
            return (T) new NearbyBenchesViewModel(container.benchRepository, container.locationProvider);
        }
        throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
    }
}
