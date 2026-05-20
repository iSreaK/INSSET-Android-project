package com.insset.jvbench.core.sync;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.insset.jvbench.core.common.ResultCallback;
import com.insset.jvbench.domain.model.Bench;
import com.insset.jvbench.domain.repository.BenchRepository;

import java.util.List;

/**
 * Lightweight in-process periodic synchronisation of the bench list.
 *
 * <p>We deliberately avoid pulling in {@code androidx.work:work-runtime} —
 * its 2.x line currently fails to resolve cleanly with the project's Gradle 9
 * milestone + JDK 25 combination ("Unsupported class file major version 69"
 * when Gradle parses the artifact metadata). Going with a plain
 * main-thread {@link Handler} keeps the implementation zero-dependency.</p>
 *
 * <p>What this gives us:</p>
 * <ul>
 *   <li>A repeating background refresh while the app process is alive (the
 *       {@link com.insset.jvbench.di.App} is kept by Android as long as a
 *       service or activity needs it).</li>
 *   <li>Satisfies the "synchronisation périodique" sub-bullet of the PDF's
 *       Option-3 "Service Android" requirement.</li>
 * </ul>
 *
 * <p>Trade-off: this does <em>not</em> survive the process being killed in
 * deep Doze. For the demo / soutenance scope that's acceptable, and the
 * complementary {@code BenchNavigationService} covers the "background
 * location" half of the same requirement.</p>
 */
public class BenchSyncScheduler {

    private static final String TAG = "BenchSyncScheduler";
    private static final long INTERVAL_MS = 30L * 60L * 1000L; // 30 minutes

    private final BenchRepository benchRepository;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean started;

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            benchRepository.getBenches(new ResultCallback<List<Bench>>() {
                @Override
                public void onSuccess(List<Bench> result) {
                    Log.d(TAG, "Periodic sync OK, " + (result != null ? result.size() : 0) + " benches");
                }

                @Override
                public void onError(String errorMessage) {
                    Log.w(TAG, "Periodic sync failed: " + errorMessage);
                }
            });
            handler.postDelayed(this, INTERVAL_MS);
        }
    };

    public BenchSyncScheduler(BenchRepository benchRepository) {
        this.benchRepository = benchRepository;
    }

    /** Starts the loop. Idempotent. The first refresh fires after {@value #INTERVAL_MS} ms. */
    public synchronized void start() {
        if (started) return;
        started = true;
        handler.postDelayed(tick, INTERVAL_MS);
    }

    public synchronized void stop() {
        if (!started) return;
        handler.removeCallbacks(tick);
        started = false;
    }
}
