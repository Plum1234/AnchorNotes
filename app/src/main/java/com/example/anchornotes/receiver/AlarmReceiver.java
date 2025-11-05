package com.example.anchornotes.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;

import com.example.anchornotes.context.NotificationHelper;
import com.example.anchornotes.data.ServiceLocator;
import com.example.anchornotes.data.db.AppDatabase;
import com.example.anchornotes.data.db.NoteEntity;
import com.example.anchornotes.data.repo.NoteRepository;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * BroadcastReceiver for handling time-based reminder alarms.
 */
public class AlarmReceiver extends BroadcastReceiver {
    public static final String EXTRA_NOTE_ID = "noteId";

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    public void onReceive(@NonNull Context context, @NonNull Intent intent) {
        if (!intent.hasExtra(EXTRA_NOTE_ID)) {
            return;
        }

        long noteId = intent.getLongExtra(EXTRA_NOTE_ID, -1);
        if (noteId == -1) {
            return;
        }

        executor.execute(() -> {
            AppDatabase db = AppDatabase.get(context);
            NoteEntity note = db.noteDao().getById(noteId);
            if (note == null) {
                return;
            }

            // Show notification
            NotificationHelper notificationHelper = new NotificationHelper(context);
            notificationHelper.showReminder(note);

            // Mark note as relevant (expires in 1 hour)
            NoteRepository repo = ServiceLocator.noteRepository(context);
            repo.markRelevantForTime(noteId, System.currentTimeMillis());
        });
    }
}
