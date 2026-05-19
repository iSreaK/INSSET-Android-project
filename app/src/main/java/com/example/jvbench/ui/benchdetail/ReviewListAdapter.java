package com.example.jvbench.ui.benchdetail;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.jvbench.R;
import com.example.jvbench.domain.model.Review;

import java.util.ArrayList;
import java.util.List;

public class ReviewListAdapter extends RecyclerView.Adapter<ReviewListAdapter.ReviewViewHolder> {

    public interface Listener {
        void onEdit(Review review);
        void onDelete(Review review);
    }

    private final List<Review> reviews = new ArrayList<>();
    private String currentUserId;
    private boolean currentUserIsAdmin;
    private final Listener listener;

    public ReviewListAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submit(List<Review> data, String currentUserId, boolean currentUserIsAdmin) {
        reviews.clear();
        if (data != null) {
            reviews.addAll(data);
        }
        this.currentUserId = currentUserId;
        this.currentUserIsAdmin = currentUserIsAdmin;
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
        Review review = reviews.get(position);
        holder.ratingText.setText(holder.itemView.getResources()
                .getString(R.string.review_item_format, review.getRating()));
        String comment = review.getComment();
        holder.commentText.setText(comment == null || comment.isBlank() ? "—" : comment);

        boolean canManage = currentUserIsAdmin
                || (currentUserId != null && currentUserId.equals(review.getUserId()));
        holder.editButton.setVisibility(canManage ? View.VISIBLE : View.GONE);
        holder.deleteButton.setVisibility(canManage ? View.VISIBLE : View.GONE);

        holder.editButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEdit(review);
            }
        });
        holder.deleteButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDelete(review);
            }
        });
    }

    @Override
    public int getItemCount() {
        return reviews.size();
    }

    static class ReviewViewHolder extends RecyclerView.ViewHolder {
        final TextView ratingText;
        final TextView commentText;
        final Button editButton;
        final Button deleteButton;

        ReviewViewHolder(@NonNull View itemView) {
            super(itemView);
            ratingText = itemView.findViewById(R.id.reviewRatingText);
            commentText = itemView.findViewById(R.id.reviewCommentText);
            editButton = itemView.findViewById(R.id.reviewEditButton);
            deleteButton = itemView.findViewById(R.id.reviewDeleteButton);
        }
    }
}
