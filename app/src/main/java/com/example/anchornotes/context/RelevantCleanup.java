package com.example.anchornotes.context;

import android.content.Context;

import androidx.annotation.NonNull;

import com.example.anchornotes.data.ServiceLocator;
import com.example.anchornotes.data.db.AppDatabase;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Handles cleanup of expired relevant notes.
 */
public class RelevantCleanup {
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    /**
     * Runs cleanup now to remove expired relevant notes.
     * @param context Application context
     */
    public static void runNow(@NonNull Context context) {
        executor.execute(() -> {
            AppDatabase db = AppDatabase.get(context);
            long now = System.currentTimeMillis();
            db.relevantDao().expire(now);
        });
    }
}
