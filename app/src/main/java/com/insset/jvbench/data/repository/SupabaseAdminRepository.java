package com.insset.jvbench.data.repository;

import com.insset.jvbench.core.common.ResultCallback;
import com.insset.jvbench.data.mapper.UserMapper;
import com.insset.jvbench.data.remote.supabase.SupabaseApiClient;
import com.insset.jvbench.data.remote.supabase.SupabaseClientProvider;
import com.insset.jvbench.data.remote.supabase.SupabaseResponse;
import com.insset.jvbench.domain.model.User;
import com.insset.jvbench.domain.model.UserRole;
import com.insset.jvbench.domain.repository.AdminRepository;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SupabaseAdminRepository implements AdminRepository {
    private final SupabaseClientProvider clientProvider;
    private final SupabaseApiClient apiClient;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public SupabaseAdminRepository(SupabaseClientProvider clientProvider, SupabaseApiClient apiClient) {
        this.clientProvider = clientProvider;
        this.apiClient = apiClient;
    }

    @Override
    public void listUsers(ResultCallback<List<User>> callback) {
        executor.execute(() -> {
            String url = clientProvider.getRestBaseUrl()
                    + "/profiles?select=id,email,username,role,created_at"
                    + "&order=created_at.desc";
            SupabaseResponse response = apiClient.get(url, true);
            if (!response.isSuccessful()) {
                callback.onError(response.getError());
                return;
            }
            try {
                JSONArray array = new JSONArray(response.getBody());
                List<User> users = new ArrayList<>();
                for (int i = 0; i < array.length(); i++) {
                    users.add(UserMapper.fromProfileJson(array.getJSONObject(i)));
                }
                callback.onSuccess(users);
            } catch (JSONException e) {
                callback.onError(e.getMessage());
            }
        });
    }

    @Override
    public void changeUserRole(String userId, UserRole newRole, ResultCallback<Void> callback) {
        executor.execute(() -> {
            if (userId == null || userId.isBlank() || newRole == null) {
                callback.onError("Parametres invalides.");
                return;
            }
            try {
                JSONObject payload = new JSONObject().put("role", newRole.name());
                String url = clientProvider.getRestBaseUrl() + "/profiles?id=eq." + userId;
                SupabaseResponse response = apiClient.patch(url, payload, true, "return=minimal");
                if (!response.isSuccessful()) {
                    callback.onError(response.getError());
                    return;
                }
                callback.onSuccess(null);
            } catch (JSONException e) {
                callback.onError(e.getMessage());
            }
        });
    }

    @Override
    public void deleteUser(String userId, ResultCallback<Void> callback) {
        executor.execute(() -> {
            if (userId == null || userId.isBlank()) {
                callback.onError("User id requis.");
                return;
            }
            try {
                // Call the SECURITY DEFINER RPC that enforces "no admin can
                // delete another admin" server-side, then cascades to
                // auth.users -> profiles -> benches/reviews via FK.
                JSONObject payload = new JSONObject().put("p_user_id", userId);
                String url = clientProvider.getRestBaseUrl() + "/rpc/admin_delete_user";
                SupabaseResponse response = apiClient.post(url, payload, true);
                if (!response.isSuccessful()) {
                    callback.onError(response.getError());
                    return;
                }
                callback.onSuccess(null);
            } catch (JSONException e) {
                callback.onError(e.getMessage());
            }
        });
    }
}
