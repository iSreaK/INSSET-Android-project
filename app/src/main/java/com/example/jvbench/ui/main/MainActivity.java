package com.example.jvbench.ui.main;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.jvbench.R;
import com.example.jvbench.di.App;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        View root = findViewById(R.id.nav_host_fragment);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
        // Force a dispatch on first creation; without this the listener
        // sometimes doesn't fire until the first configuration change.
        ViewCompat.requestApplyInsets(root);

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
