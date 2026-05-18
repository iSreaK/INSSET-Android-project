package com.example.jvbench.ui.mybenches;

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
import com.example.jvbench.core.navigation.NavConstants;
import com.example.jvbench.di.App;
import com.example.jvbench.ui.main.AppViewModelFactory;

public class MyBenchesFragment extends Fragment {

    private MyBenchesViewModel viewModel;
    private MyBenchesAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_my_benches, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        App app = (App) requireActivity().getApplication();
        viewModel = new ViewModelProvider(this, new AppViewModelFactory(app.getAppContainer())).get(MyBenchesViewModel.class);

        TextView statusText = view.findViewById(R.id.myBenchesStatusText);
        RecyclerView recyclerView = view.findViewById(R.id.myBenchesRecycler);

        adapter = new MyBenchesAdapter(bench -> {
            Bundle args = new Bundle();
            args.putString(NavConstants.ARG_BENCH_ID, bench.getId());
            NavHostFragment.findNavController(this).navigate(R.id.action_myBenchesFragment_to_benchDetailFragment, args);
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            if (state == null) return;
            if (state.loading) {
                statusText.setText(R.string.loading);
                statusText.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
                return;
            }
            if (state.notLoggedIn) {
                statusText.setText(R.string.error_guest_action_blocked);
                statusText.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
                return;
            }
            if (state.error != null) {
                statusText.setText(state.error);
                statusText.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
                return;
            }
            if (state.benches == null || state.benches.isEmpty()) {
                statusText.setText(R.string.my_benches_empty);
                statusText.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
                return;
            }
            statusText.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            adapter.submit(state.benches);
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (viewModel != null) {
            viewModel.load();
        }
    }
}
