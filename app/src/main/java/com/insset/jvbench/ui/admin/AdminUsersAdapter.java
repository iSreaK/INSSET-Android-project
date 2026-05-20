package com.insset.jvbench.ui.admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.insset.jvbench.R;
import com.insset.jvbench.domain.model.User;
import com.insset.jvbench.domain.model.UserRole;

import java.util.ArrayList;
import java.util.List;

public class AdminUsersAdapter extends RecyclerView.Adapter<AdminUsersAdapter.UserVH> {

    public interface OnUserAction {
        void onChangeRole(User user, UserRole newRole);
        void onDelete(User user);
    }

    private final List<User> items = new ArrayList<>();
    @Nullable
    private String currentUserId;
    @Nullable
    private final OnUserAction listener;

    public AdminUsersAdapter(@Nullable OnUserAction listener) {
        this.listener = listener;
    }

    public void setCurrentUserId(@Nullable String currentUserId) {
        this.currentUserId = currentUserId;
    }

    public void submit(List<User> users) {
        items.clear();
        if (users != null) items.addAll(users);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public UserVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_user, parent, false);
        return new UserVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull UserVH holder, int position) {
        User u = items.get(position);
        holder.username.setText(u.getUsername());
        holder.email.setText(u.getEmail());
        holder.role.setText(u.getRole().name());

        boolean isSelf = currentUserId != null && currentUserId.equals(u.getId());
        boolean isTargetAdmin = u.getRole() == UserRole.ADMINISTRATOR;

        // Cannot manage other admins, cannot manage self.
        boolean canManage = !isSelf && !isTargetAdmin;

        holder.promoteModButton.setVisibility(canManage && u.getRole() != UserRole.MODERATOR ? View.VISIBLE : View.GONE);
        holder.demoteUserButton.setVisibility(canManage && u.getRole() != UserRole.USER ? View.VISIBLE : View.GONE);
        holder.deleteButton.setVisibility(canManage ? View.VISIBLE : View.GONE);

        holder.promoteModButton.setOnClickListener(v -> {
            if (listener != null) listener.onChangeRole(u, UserRole.MODERATOR);
        });
        holder.demoteUserButton.setOnClickListener(v -> {
            if (listener != null) listener.onChangeRole(u, UserRole.USER);
        });
        holder.deleteButton.setOnClickListener(v -> {
            if (listener != null) listener.onDelete(u);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class UserVH extends RecyclerView.ViewHolder {
        final TextView username;
        final TextView email;
        final TextView role;
        final View promoteModButton;
        final View demoteUserButton;
        final ImageButton deleteButton;

        UserVH(@NonNull View itemView) {
            super(itemView);
            username = itemView.findViewById(R.id.adminUserUsername);
            email = itemView.findViewById(R.id.adminUserEmail);
            role = itemView.findViewById(R.id.adminUserRole);
            promoteModButton = itemView.findViewById(R.id.adminUserPromoteMod);
            demoteUserButton = itemView.findViewById(R.id.adminUserDemoteUser);
            deleteButton = itemView.findViewById(R.id.adminUserDelete);
        }
    }
}
