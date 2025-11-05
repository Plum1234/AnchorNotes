package com.example.anchornotes.worker;

import android.content.Context;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.anchornotes.context.GeofenceManager;
import com.example.anchornotes.context.NotificationHelper;
import com.example.anchornotes.context.ReminderManager;
import com.example.anchornotes.data.db.AppDatabase;
import com.example.anchornotes.data.db.NoteEntity;
import com.example.anchornotes.data.repo.NoteRepository;
import com.google.android.gms.common.api.ApiException;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Worker that retries geofence registration for notes that failed to activate.
 */
public class GeofenceRetryWorker extends Worker {
    public GeofenceRetryWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        AppDatabase db = AppDatabase.get(context);
        List<NoteEntity> pendingNotes = db.noteDao().getPendingActivationNotes();

        if (pendingNotes.isEmpty()) {
            return Result.success();
        }

        // Check location permissions
        boolean hasPermission = ContextCompat.checkSelfPermission(context,
                android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        if (!hasPermission) {
            // Permissions not granted, will retry later
            return Result.retry();
        }

        GeofenceManager geofenceManager = new GeofenceManager(context);
        NoteRepository repo = new NoteRepository(db.noteDao());
        ReminderManager reminderManager = new ReminderManager(context);

        boolean allSucceeded = true;
        int failureCount = 0;

        for (NoteEntity note : pendingNotes) {
            if (note.latitude == null || note.longitude == null || note.geofenceId == null) {
                continue;
            }

            AtomicBoolean success = new AtomicBoolean(false);
            CountDownLatch latch = new CountDownLatch(1);

            geofenceManager.addForNote(
                    note.id,
                    note.latitude,
                    note.longitude,
                    175.0f, // Default radius
                    note.geofenceId,
                    result -> {
                        success.set(true);
                        latch.countDown();
                    },
                    exception -> {
                        success.set(false);
                        latch.countDown();
                    }
            );

            try {
                latch.await();
                if (success.get()) {
                    // Clear pending flag
                    db.noteDao().setPendingActivation(note.id, false);
                } else {
                    allSucceeded = false;
                    failureCount++;

                    // After multiple failures, schedule fallback time reminder (5 minutes from now)
                    if (failureCount >= 3) {
                        long fallbackTime = System.currentTimeMillis() + (5 * 60 * 1000);
                        reminderManager.scheduleExact(note.id, fallbackTime);
                        db.noteDao().setReminderTime(note.id, fallbackTime, System.currentTimeMillis());
                        db.noteDao().setPendingActivation(note.id, false);

                        // Notify user
                        NotificationHelper notificationHelper = new NotificationHelper(context);
                        // Create a simple notification text for fallback
                        NoteEntity fallbackNote = new NoteEntity();
                        fallbackNote.id = note.id;
                        fallbackNote.title = note.title;
                        fallbackNote.bodyHtml = "Geofence reminder failed, using time reminder instead";
                        notificationHelper.showReminder(fallbackNote);
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return Result.retry();
            }
        }

        return allSucceeded ? Result.success() : Result.retry();
    }
}
