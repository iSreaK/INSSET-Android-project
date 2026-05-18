package com.example.jvbench.ui.admin;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.jvbench.R;
import com.example.jvbench.core.theme.WindowInsetsHelper;
import com.example.jvbench.di.App;
import com.example.jvbench.domain.model.User;
import com.example.jvbench.ui.main.AppViewModelFactory;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class AdminFragment extends Fragment {

    private AdminViewModel viewModel;
    private AdminUsersAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        App app = (App) requireActivity().getApplication();
        viewModel = new ViewModelProvider(this, new AppViewModelFactory(app.getAppContainer())).get(AdminViewModel.class);

        TextView statusText = view.findViewById(R.id.adminStatusText);
        RecyclerView recycler = view.findViewById(R.id.adminUsersRecycler);

        User currentUser = app.getAppContainer().authRepository.getCurrentUser();
        // Defensive: if a non-admin somehow reached this screen, bounce them.
        if (currentUser == null || !currentUser.getRole().isAdmin()) {
            NavHostFragment.findNavController(this).navigateUp();
            return;
        }
        adapter = new AdminUsersAdapter(new AdminUsersAdapter.OnUserAction() {
            @Override
            public void onChangeRole(User user, com.example.jvbench.domain.model.UserRole newRole) {
                viewModel.changeRole(user.getId(), newRole);
            }

            @Override
            public void onDelete(User user) {
                new AlertDialog.Builder(requireContext())
                        .setTitle(R.string.admin_confirm_delete_title)
                        .setMessage(getString(R.string.admin_confirm_delete_message, user.getUsername()))
                        .setPositiveButton(R.string.action_delete, (d, w) -> viewModel.deleteUser(user.getId()))
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
            }
        });
        adapter.setCurrentUserId(currentUser != null ? currentUser.getId() : null);
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        recycler.setAdapter(adapter);

        viewModel.getState().observe(getViewLifecycleOwner(), state -> {
            if (state == null) return;
            if (state.loading) {
                statusText.setText(R.string.loading);
                statusText.setVisibility(View.VISIBLE);
            } else if (state.error != null) {
                statusText.setText(state.error);
                statusText.setVisibility(View.VISIBLE);
            } else if (state.users == null || state.users.isEmpty()) {
                statusText.setText(R.string.admin_no_users);
                statusText.setVisibility(View.VISIBLE);
            } else {
                statusText.setVisibility(View.GONE);
            }
            if (state.users != null) {
                adapter.submit(state.users);
            }
        });

        BottomNavigationView nav = view.findViewById(R.id.adminBottomNav);
        WindowInsetsHelper.addBottomSystemInset(nav);
        nav.getMenu().findItem(R.id.navAdminItem).setVisible(true);
        nav.setSelectedItemId(R.id.navAdminItem);
        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.navAdminItem) return true;
            if (id == R.id.navMapItem) {
                NavHostFragment.findNavController(this).navigate(R.id.action_adminFragment_to_mapFragment);
                return true;
            }
            if (id == R.id.navAccountItem) {
                NavHostFragment.findNavController(this).navigate(R.id.action_adminFragment_to_accountFragment);
                return true;
            }
            if (id == R.id.navSettingsItem) {
                NavHostFragment.findNavController(this).navigate(R.id.action_adminFragment_to_settingsFragment);
                return true;
            }
            return false;
        });

        viewModel.load();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (viewModel != null) viewModel.load();
    }
}
