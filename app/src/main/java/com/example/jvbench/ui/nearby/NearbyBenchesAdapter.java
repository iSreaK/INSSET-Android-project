package com.example.jvbench.ui.nearby;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.jvbench.R;
import com.example.jvbench.domain.model.Bench;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for the "Bancs autour de vous" list. Each row shows a small bench
 * card (image, name, rating, distance) with two CTAs: open the bench detail
 * screen, or hand off to an external navigation app.
 */
public class NearbyBenchesAdapter extends RecyclerView.Adapter<NearbyBenchesAdapter.NearbyHolder> {

    public interface Callbacks {
        void onOpenDetail(@NonNull Bench bench);
        void onNavigate(@NonNull Bench bench);
    }

    private final List<NearbyBenchesViewModel.NearbyBench> items = new ArrayList<>();
    private final Callbacks callbacks;

    public NearbyBenchesAdapter(Callbacks callbacks) {
        this.callbacks = callbacks;
    }

    public void submit(List<NearbyBenchesViewModel.NearbyBench> data) {
        items.clear();
        if (data != null) {
            items.addAll(data);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public NearbyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_nearby_bench, parent, false);
        return new NearbyHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NearbyHolder holder, int position) {
        NearbyBenchesViewModel.NearbyBench item = items.get(position);
        Bench bench = item.bench;
        holder.name.setText(bench.getName());
        holder.meta.setText(holder.itemView.getContext().getString(
                R.string.bench_meta_format, bench.getAverageRating(), bench.getReviewCount()));
        holder.distance.setText(formatDistance(holder.itemView.getContext(), item.distanceMeters));

        String imageUrl = bench.getImageUrl();
        if (imageUrl != null && !imageUrl.isBlank()) {
            holder.image.setVisibility(View.VISIBLE);
            Glide.with(holder.itemView).load(imageUrl).centerCrop().into(holder.image);
        } else {
            holder.image.setVisibility(View.GONE);
        }

        holder.detailButton.setOnClickListener(v -> {
            if (callbacks != null) callbacks.onOpenDetail(bench);
        });
        holder.navigateButton.setOnClickListener(v -> {
            if (callbacks != null) callbacks.onNavigate(bench);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private static String formatDistance(android.content.Context ctx, double meters) {
        if (meters < 1000d) {
            return ctx.getString(R.string.nearby_distance_meters, (int) Math.round(meters));
        }
        return ctx.getString(R.string.nearby_distance_kilometers, meters / 1000d)
                .replace(',', '.'); // safeguard so "1,2 km" doesn't slip in on FR locales
    }

    /** Same formatting as {@link #formatDistance} but locale-independent for unit tests. */
    @SuppressWarnings("unused")
    static String formatDistanceRaw(double meters) {
        if (meters < 1000d) {
            return Math.round(meters) + " m";
        }
        return String.format(Locale.US, "%.1f km", meters / 1000d);
    }

    static class NearbyHolder extends RecyclerView.ViewHolder {
        final ImageView image;
        final TextView name;
        final TextView meta;
        final TextView distance;
        final Button detailButton;
        final Button navigateButton;

        NearbyHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.itemNearbyImage);
            name = itemView.findViewById(R.id.itemNearbyName);
            meta = itemView.findViewById(R.id.itemNearbyMeta);
            distance = itemView.findViewById(R.id.itemNearbyDistance);
            detailButton = itemView.findViewById(R.id.itemNearbyDetailButton);
            navigateButton = itemView.findViewById(R.id.itemNearbyNavigateButton);
        }
    }
}
