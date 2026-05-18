package com.example.jvbench.ui.benchdetail;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.jvbench.R;
import com.example.jvbench.domain.model.Review;

import java.util.ArrayList;
import java.util.List;

public class ReviewsAdapter extends RecyclerView.Adapter<ReviewsAdapter.ReviewViewHolder> {

    private final List<Review> items = new ArrayList<>();

    public void submit(List<Review> data) {
        items.clear();
        if (data != null) {
            items.addAll(data);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ReviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_review, parent, false);
        return new ReviewViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReviewViewHolder holder, int position) {
        Review review = items.get(position);
        holder.ratingText.setText(holder.itemView.getContext().getString(R.string.review_rating_format, review.getRating()));
        if (review.getComment() == null || review.getComment().isBlank()) {
            holder.commentText.setVisibility(View.GONE);
        } else {
            holder.commentText.setVisibility(View.VISIBLE);
            holder.commentText.setText(review.getComment());
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ReviewViewHolder extends RecyclerView.ViewHolder {
        final TextView ratingText;
        final TextView commentText;

        ReviewViewHolder(@NonNull View itemView) {
            super(itemView);
            ratingText = itemView.findViewById(R.id.itemReviewRating);
            commentText = itemView.findViewById(R.id.itemReviewComment);
        }
    }
}
