package com.insset.jvbench.ui.mybenches;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.insset.jvbench.R;
import com.insset.jvbench.domain.model.Bench;

import java.util.ArrayList;
import java.util.List;

public class MyBenchesAdapter extends RecyclerView.Adapter<MyBenchesAdapter.BenchViewHolder> {

    public interface OnBenchClickListener {
        void onClick(Bench bench);
    }

    private final List<Bench> items = new ArrayList<>();
    private final OnBenchClickListener listener;

    public MyBenchesAdapter(OnBenchClickListener listener) {
        this.listener = listener;
    }

    public void submit(List<Bench> data) {
        items.clear();
        if (data != null) {
            items.addAll(data);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public BenchViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_bench, parent, false);
        return new BenchViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BenchViewHolder holder, int position) {
        Bench bench = items.get(position);
        holder.nameText.setText(bench.getName());
        holder.metaText.setText(holder.itemView.getContext().getString(
                R.string.bench_meta_format, bench.getAverageRating(), bench.getReviewCount()));
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onClick(bench);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class BenchViewHolder extends RecyclerView.ViewHolder {
        final TextView nameText;
        final TextView metaText;

        BenchViewHolder(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.itemBenchName);
            metaText = itemView.findViewById(R.id.itemBenchMeta);
        }
    }
}
