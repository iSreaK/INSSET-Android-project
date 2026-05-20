package com.insset.jvbench.ui.benchdetail;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.insset.jvbench.R;
import com.insset.jvbench.domain.model.Review;

import java.util.ArrayList;
import java.util.List;

public class ReviewsAdapter extends RecyclerView.Adapter<ReviewsAdapter.ReviewViewHolder> {

    public interface OnReviewActionListener {
        void onDeleteOwnReview(Review review);
    }

    private final List<Review> items = new ArrayList<>();
    @Nullable
    private String currentUserId;
    private boolean canModerate;
    @Nullable
    private final OnReviewActionListener listener;

    public ReviewsAdapter(@Nullable OnReviewActionListener listener) {
        this.listener = listener;
    }

    public void setCurrentUserId(@Nullable String currentUserId) {
        this.currentUserId = currentUserId;
        notifyDataSetChanged();
    }

    public void setCanModerate(boolean canModerate) {
        this.canModerate = canModerate;
        notifyDataSetChanged();
    }

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
        String username = review.getAuthorUsername();
        if (username == null || username.isBlank()) {
            username = holder.itemView.getContext().getString(R.string.review_unknown_author);
        }
        holder.authorText.setText(username);
        holder.ratingText.setText(holder.itemView.getContext().getString(R.string.review_rating_short_format, review.getRating()));
        if (review.getComment() == null || review.getComment().isBlank()) {
            holder.commentText.setVisibility(View.GONE);
        } else {
            holder.commentText.setVisibility(View.VISIBLE);
            holder.commentText.setText(review.getComment());
        }

        boolean isOwn = currentUserId != null && currentUserId.equals(review.getUserId());
        boolean showDelete = isOwn || canModerate;
        holder.deleteButton.setVisibility(showDelete ? View.VISIBLE : View.GONE);
        holder.deleteButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteOwnReview(review);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ReviewViewHolder extends RecyclerView.ViewHolder {
        final TextView authorText;
        final TextView ratingText;
        final TextView commentText;
        final ImageButton deleteButton;

        ReviewViewHolder(@NonNull View itemView) {
            super(itemView);
            authorText = itemView.findViewById(R.id.itemReviewAuthor);
            ratingText = itemView.findViewById(R.id.itemReviewRating);
            commentText = itemView.findViewById(R.id.itemReviewComment);
            deleteButton = itemView.findViewById(R.id.itemReviewDelete);
        }
    }
}
