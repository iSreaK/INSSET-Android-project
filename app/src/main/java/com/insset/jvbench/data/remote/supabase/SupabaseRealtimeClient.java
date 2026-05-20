package com.insset.jvbench.data.remote.supabase;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

/**
 * Minimal Supabase Realtime client built on top of OkHttp WebSocket.
 *
 * It opens a single connection to <SUPABASE_URL>/realtime/v1/websocket and lets
 * fragments subscribe to changes on a given table. The listener fires on the
 * main thread with the event payload (INSERT / UPDATE / DELETE).
 *
 * This is intentionally tiny: no schema validation, no replay, no presence.
 * If the socket drops we attempt to reconnect with a small backoff; on reconnect
 * we replay every previous join automatically.
 */
public class SupabaseRealtimeClient {
    private static final String TAG = "SupabaseRealtime";
    private static final long HEARTBEAT_INTERVAL_MS = 25_000L;
    private static final long INITIAL_RECONNECT_DELAY_MS = 1_000L;
    private static final long MAX_RECONNECT_DELAY_MS = 30_000L;

    public interface ChangeListener {
        /**
         * Called on the main thread whenever the subscribed table receives an
         * INSERT, UPDATE or DELETE. The record argument is the row content (new
         * row for INSERT/UPDATE, old row for DELETE) or null if absent.
         */
        void onChange(@NonNull String eventType, @Nullable JSONObject record);
    }

    private static class Subscription {
        final String schema;
        final String table;
        final ChangeListener listener;
        @Nullable
        String topic; // realtime:<schema>:<table>:<id>
        @Nullable
        String joinRef;

        Subscription(String schema, String table, ChangeListener listener) {
            this.schema = schema;
            this.table = table;
            this.listener = listener;
        }
    }

    private final SupabaseClientProvider provider;
    private final SupabaseSessionStore sessionStore;
    private final OkHttpClient httpClient;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final AtomicInteger refCounter = new AtomicInteger(0);

    private final Map<Integer, Subscription> subsBySubId = new LinkedHashMap<>();
    private final List<Subscription> allSubscriptions = new CopyOnWriteArrayList<>();

    @Nullable
    private WebSocket webSocket;
    private boolean connected = false;
    private boolean shouldRun = false;
    private long currentReconnectDelay = INITIAL_RECONNECT_DELAY_MS;

    private final Runnable heartbeatTask = new Runnable() {
        @Override
        public void run() {
            sendHeartbeat();
            mainHandler.postDelayed(this, HEARTBEAT_INTERVAL_MS);
        }
    };

    public SupabaseRealtimeClient(SupabaseClientProvider provider, SupabaseSessionStore sessionStore) {
        this.provider = provider;
        this.sessionStore = sessionStore;
        this.httpClient = new OkHttpClient.Builder()
                .pingInterval(20, TimeUnit.SECONDS)
                .build();
    }

    /**
     * Subscribes to row changes on a table. Safe to call before the socket is
     * connected; the join will be issued as soon as the socket opens.
     * Returns a handle that can be passed to {@link #unsubscribe(Object)}.
     */
    public Object subscribeTable(String schema, String table, ChangeListener listener) {
        Subscription sub = new Subscription(schema, table, listener);
        allSubscriptions.add(sub);
        ensureConnected();
        if (connected) {
            joinSubscription(sub);
        }
        return sub;
    }

    public void unsubscribe(Object handle) {
        if (!(handle instanceof Subscription)) return;
        Subscription sub = (Subscription) handle;
        allSubscriptions.remove(sub);
        synchronized (subsBySubId) {
            subsBySubId.entrySet().removeIf(entry -> entry.getValue() == sub);
        }
        if (allSubscriptions.isEmpty()) {
            shutdown();
        }
    }

    private synchronized void ensureConnected() {
        if (!provider.isConfigured()) return;
        shouldRun = true;
        if (webSocket != null || connected) return;
        openSocket();
    }

    private void openSocket() {
        String anon = provider.getAnonKey();
        // Auth: prefer user JWT so RLS is honored; fall back to anon key.
        String token = sessionStore.isAuthenticated() ? sessionStore.getAccessToken() : anon;
        String url = provider.getBaseUrl().replaceFirst("^http", "ws")
                + "/realtime/v1/websocket?apikey=" + anon
                + "&vsn=1.0.0";
        Request request = new Request.Builder()
                .url(url)
                .build();
        webSocket = httpClient.newWebSocket(request, new RealtimeSocketListener(token));
    }

    private void shutdown() {
        shouldRun = false;
        mainHandler.removeCallbacks(heartbeatTask);
        if (webSocket != null) {
            try {
                webSocket.close(1000, "client shutdown");
            } catch (Exception ignored) {
            }
            webSocket = null;
        }
        connected = false;
        synchronized (subsBySubId) {
            subsBySubId.clear();
        }
    }

    private void scheduleReconnect() {
        if (!shouldRun || allSubscriptions.isEmpty()) return;
        long delay = currentReconnectDelay;
        currentReconnectDelay = Math.min(MAX_RECONNECT_DELAY_MS, currentReconnectDelay * 2);
        mainHandler.postDelayed(() -> {
            if (!shouldRun) return;
            openSocket();
        }, delay);
    }

    private void onSocketOpen(@Nullable String token) {
        connected = true;
        currentReconnectDelay = INITIAL_RECONNECT_DELAY_MS;

        // Send access_token if we have one so RLS-protected tables work.
        if (token != null && !token.isBlank()) {
            try {
                JSONObject access = new JSONObject().put("access_token", token);
                send(new JSONObject()
                        .put("topic", "realtime:")
                        .put("event", "access_token")
                        .put("payload", access)
                        .put("ref", String.valueOf(refCounter.incrementAndGet())));
            } catch (JSONException ignored) {
            }
        }

        // Replay every subscription
        for (Subscription sub : allSubscriptions) {
            joinSubscription(sub);
        }

        mainHandler.removeCallbacks(heartbeatTask);
        mainHandler.postDelayed(heartbeatTask, HEARTBEAT_INTERVAL_MS);
    }

    private void joinSubscription(Subscription sub) {
        try {
            int ref = refCounter.incrementAndGet();
            String topic = "realtime:" + sub.schema + ":" + sub.table;
            sub.topic = topic;
            sub.joinRef = String.valueOf(ref);
            synchronized (subsBySubId) {
                subsBySubId.put(ref, sub);
            }
            JSONObject change = new JSONObject()
                    .put("event", "*")
                    .put("schema", sub.schema)
                    .put("table", sub.table);
            JSONObject config = new JSONObject()
                    .put("postgres_changes", new JSONArray().put(change));
            JSONObject payload = new JSONObject().put("config", config);
            JSONObject joinMsg = new JSONObject()
                    .put("topic", topic)
                    .put("event", "phx_join")
                    .put("payload", payload)
                    .put("ref", String.valueOf(ref));
            send(joinMsg);
        } catch (JSONException e) {
            Log.w(TAG, "Failed to send join: " + e.getMessage());
        }
    }

    private void sendHeartbeat() {
        if (!connected || webSocket == null) return;
        try {
            JSONObject hb = new JSONObject()
                    .put("topic", "phoenix")
                    .put("event", "heartbeat")
                    .put("payload", new JSONObject())
                    .put("ref", String.valueOf(refCounter.incrementAndGet()));
            send(hb);
        } catch (JSONException ignored) {
        }
    }

    private void send(JSONObject message) {
        if (webSocket == null) return;
        try {
            webSocket.send(message.toString());
        } catch (Exception e) {
            Log.w(TAG, "send failed: " + e.getMessage());
        }
    }

    private void dispatchPayload(JSONObject payload) {
        // postgres_changes events come as { "event": "postgres_changes", "payload": { "data": { type, record, old_record, table, schema } } }
        JSONObject data = payload.optJSONObject("data");
        if (data == null) return;
        String type = data.optString("type", "");
        String table = data.optString("table", "");
        String schema = data.optString("schema", "");
        JSONObject record = data.optJSONObject("record");
        if (record == null) {
            record = data.optJSONObject("old_record");
        }

        // Deliver to every matching subscription on main thread.
        List<Subscription> matches = new ArrayList<>();
        for (Subscription sub : allSubscriptions) {
            if (sub.schema.equals(schema) && sub.table.equals(table)) {
                matches.add(sub);
            }
        }
        if (matches.isEmpty()) return;
        final JSONObject finalRecord = record;
        for (Subscription sub : matches) {
            mainHandler.post(() -> sub.listener.onChange(type, finalRecord));
        }
    }

    private class RealtimeSocketListener extends WebSocketListener {
        private final String token;

        RealtimeSocketListener(@Nullable String token) {
            this.token = token;
        }

        @Override
        public void onOpen(@NonNull WebSocket webSocket, @NonNull Response response) {
            mainHandler.post(() -> onSocketOpen(token));
        }

        @Override
        public void onMessage(@NonNull WebSocket webSocket, @NonNull String text) {
            try {
                JSONObject envelope = new JSONObject(text);
                String event = envelope.optString("event", "");
                if ("postgres_changes".equals(event)) {
                    JSONObject payload = envelope.optJSONObject("payload");
                    if (payload != null) {
                        dispatchPayload(payload);
                    }
                }
                // We ignore phx_reply (join ack), heartbeat reply, presence, system, etc.
            } catch (JSONException e) {
                Log.w(TAG, "Bad realtime message: " + e.getMessage());
            }
        }

        @Override
        public void onClosing(@NonNull WebSocket webSocket, int code, @NonNull String reason) {
            connected = false;
            SupabaseRealtimeClient.this.webSocket = null;
            mainHandler.removeCallbacks(heartbeatTask);
            if (shouldRun) scheduleReconnect();
        }

        @Override
        public void onFailure(@NonNull WebSocket webSocket, @NonNull Throwable t, @Nullable Response response) {
            Log.w(TAG, "Realtime socket failure: " + t.getMessage());
            connected = false;
            SupabaseRealtimeClient.this.webSocket = null;
            mainHandler.removeCallbacks(heartbeatTask);
            if (shouldRun) scheduleReconnect();
        }
    }
}
