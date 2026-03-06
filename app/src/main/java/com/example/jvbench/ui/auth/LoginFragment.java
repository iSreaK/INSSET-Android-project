package com.example.jvbench.ui.auth;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.example.jvbench.R;
import com.example.jvbench.di.App;
import com.example.jvbench.ui.main.AppViewModelFactory;

public class LoginFragment extends Fragment {
    private LoginViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_login, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        App app = (App) requireActivity().getApplication();
        viewModel = new ViewModelProvider(this, new AppViewModelFactory(app.getAppContainer())).get(LoginViewModel.class);

        EditText emailInput = view.findViewById(R.id.loginEmailInput);
        EditText passwordInput = view.findViewById(R.id.loginPasswordInput);
        TextView statusText = view.findViewById(R.id.loginStatusText);

        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            if (state != null && state.message != null) {
                statusText.setText(state.message);
            } else if (state != null && state.loading) {
                statusText.setText(R.string.loading);
            }
        });

        view.findViewById(R.id.loginButton).setOnClickListener(v -> {
            String email = emailInput.getText().toString().trim();
            String password = passwordInput.getText().toString();
            if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
                statusText.setText(R.string.error_missing_credentials);
                return;
            }
            viewModel.login(email, password, () -> {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> NavHostFragment.findNavController(this)
                            .navigate(R.id.action_loginFragment_to_mapFragment));
                }
            });
        });

        view.findViewById(R.id.goRegisterButton).setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(R.id.action_loginFragment_to_registerFragment));
    }
}
