package com.example.anchornotes.worker;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.anchornotes.context.RelevantCleanup;

/**
 * Worker that periodically cleans up expired relevant notes.
 * This ensures that time-based reminders are automatically retired after their expiration period,
 * even if the app is not actively being used.
 */
public class RelevantCleanupWorker extends Worker {
    public RelevantCleanupWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();

        // Run the cleanup
        RelevantCleanup.runNow(context);

        // Always return success - cleanup is best-effort
        return Result.success();
    }
}
