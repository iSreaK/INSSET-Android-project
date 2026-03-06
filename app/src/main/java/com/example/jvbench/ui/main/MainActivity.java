package com.example.jvbench.ui.main;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.jvbench.R;
import com.example.jvbench.di.App;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Warm up session restore once at app start.
        App app = (App) getApplication();
        app.getAppContainer().authRepository.loadCurrentUser(new com.example.jvbench.core.common.ResultCallback<com.example.jvbench.domain.model.User>() {
            @Override
            public void onSuccess(com.example.jvbench.domain.model.User result) {
            }

            @Override
            public void onError(String errorMessage) {
            }
        });
    }
}
