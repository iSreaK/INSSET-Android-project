package com.example.jvbench.ui.account;

import android.os.Bundle;
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
import com.example.jvbench.core.common.ResultCallback;
import com.example.jvbench.di.App;
import com.example.jvbench.ui.main.AppViewModelFactory;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class AccountFragment extends Fragment {
    private AccountViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_account, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        App app = (App) requireActivity().getApplication();
        viewModel = new ViewModelProvider(this, new AppViewModelFactory(app.getAppContainer())).get(AccountViewModel.class);

        TextView statusText = view.findViewById(R.id.accountStatusText);
        View notConnectedBlock = view.findViewById(R.id.accountNotConnectedBlock);
        View connectedBlock = view.findViewById(R.id.accountConnectedBlock);
        TextView emailValue = view.findViewById(R.id.accountEmailValue);
        TextView usernameValue = view.findViewById(R.id.accountUsernameValue);
        TextView roleValue = view.findViewById(R.id.accountRoleValue);
        EditText usernameInput = view.findViewById(R.id.accountUsernameInput);
        Button saveUsernameButton = view.findViewById(R.id.accountSaveUsernameButton);
        Button goLoginButton = view.findViewById(R.id.accountGoLoginButton);
        Button goRegisterButton = view.findViewById(R.id.accountGoRegisterButton);
        Button signOutButton = view.findViewById(R.id.accountSignOutButton);

        BottomNavigationView bottomNavigationView = view.findViewById(R.id.accountBottomNav);
        bottomNavigationView.setSelectedItemId(R.id.navAccountItem);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.navAccountItem) {
                return true;
            }
            if (item.getItemId() == R.id.navMapItem) {
                NavHostFragment.findNavController(this).navigate(R.id.action_accountFragment_to_mapFragment);
                return true;
            }
            return false;
        });

        goLoginButton.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(R.id.action_accountFragment_to_loginFragment));
        goRegisterButton.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(R.id.action_accountFragment_to_registerFragment));

        signOutButton.setOnClickListener(v -> {
            signOutButton.setEnabled(false);
            saveUsernameButton.setEnabled(false);
            viewModel.signOut(new ResultCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    if (!isAdded()) {
                        return;
                    }
                    requireActivity().runOnUiThread(() -> {
                        signOutButton.setEnabled(true);
                        saveUsernameButton.setEnabled(true);
                        viewModel.loadAccount();
                    });
                }

                @Override
                public void onError(String errorMessage) {
                    if (!isAdded()) {
                        return;
                    }
                    requireActivity().runOnUiThread(() -> {
                        signOutButton.setEnabled(true);
                        saveUsernameButton.setEnabled(true);
                        statusText.setText(errorMessage);
                    });
                }
            });
        });

        saveUsernameButton.setOnClickListener(v -> {
            String newUsername = usernameInput.getText().toString().trim();
            if (!newUsername.matches("^[a-zA-Z0-9_]{3,20}$")) {
                statusText.setText(R.string.error_invalid_username);
                return;
            }
            saveUsernameButton.setEnabled(false);
            signOutButton.setEnabled(false);
            viewModel.updateUsername(newUsername);
        });

        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            if (state == null) {
                return;
            }
            if (state.loading) {
                statusText.setText(R.string.loading);
                saveUsernameButton.setEnabled(false);
                signOutButton.setEnabled(false);
                connectedBlock.setVisibility(View.GONE);
                notConnectedBlock.setVisibility(View.GONE);
                return;
            }
            saveUsernameButton.setEnabled(true);
            signOutButton.setEnabled(true);
            if (state.user == null) {
                statusText.setText(R.string.account_not_connected);
                connectedBlock.setVisibility(View.GONE);
                notConnectedBlock.setVisibility(View.VISIBLE);
                return;
            }
            statusText.setText(state.message != null ? state.message : getString(R.string.account_connected));
            notConnectedBlock.setVisibility(View.GONE);
            connectedBlock.setVisibility(View.VISIBLE);
            emailValue.setText(state.user.getEmail());
            usernameValue.setText(state.user.getUsername());
            usernameInput.setText(state.user.getUsername());
            roleValue.setText(state.user.getRole().name());
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (viewModel != null) {
            viewModel.loadAccount();
        }
    }
}
