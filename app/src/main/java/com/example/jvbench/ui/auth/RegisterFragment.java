package com.example.jvbench.ui.auth;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

public class RegisterFragment extends Fragment {
    private RegisterViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_register, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        App app = (App) requireActivity().getApplication();
        viewModel = new ViewModelProvider(this, new AppViewModelFactory(app.getAppContainer())).get(RegisterViewModel.class);

        EditText emailInput = view.findViewById(R.id.registerEmailInput);
        EditText passwordInput = view.findViewById(R.id.registerPasswordInput);
        EditText confirmPasswordInput = view.findViewById(R.id.registerConfirmPasswordInput);
        View registerButton = view.findViewById(R.id.registerButton);
        View goLoginButton = view.findViewById(R.id.goLoginText);
        TextView statusText = view.findViewById(R.id.registerStatusText);

        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            if (state == null) {
                return;
            }
            boolean loading = state.status == RegisterViewModel.Status.LOADING;
            registerButton.setEnabled(!loading);
            goLoginButton.setEnabled(!loading);

            if (loading) {
                statusText.setText(R.string.loading);
            } else if (state.message != null) {
                statusText.setText(state.message);
            } else {
                statusText.setText("");
            }
        });

        registerButton.setOnClickListener(v -> {
            String email = emailInput.getText().toString().trim();
            String password = passwordInput.getText().toString();
            String confirmPassword = confirmPasswordInput.getText().toString();
            if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
                statusText.setText(R.string.error_missing_credentials);
                return;
            }
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                statusText.setText(R.string.error_invalid_email);
                return;
            }
            if (password.length() < 6) {
                statusText.setText(R.string.error_password_too_short);
                return;
            }
            if (!password.equals(confirmPassword)) {
                statusText.setText(R.string.error_password_mismatch);
                return;
            }
            viewModel.register(email, password, () -> {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> NavHostFragment.findNavController(this)
                            .navigate(R.id.action_registerFragment_to_accountFragment));
                }
            });
        });

        goLoginButton.setOnClickListener(v -> NavHostFragment.findNavController(this).navigateUp());
    }
}
