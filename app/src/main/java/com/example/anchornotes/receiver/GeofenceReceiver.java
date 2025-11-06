package com.example.anchornotes.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;

import com.example.anchornotes.context.GeofenceManager;
import com.example.anchornotes.context.NotificationHelper;
import com.example.anchornotes.data.ServiceLocator;
import com.example.anchornotes.data.db.AppDatabase;
import com.example.anchornotes.data.db.NoteEntity;
import com.example.anchornotes.data.repo.NoteRepository;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * BroadcastReceiver for handling geofence transition events.
 */
public class GeofenceReceiver extends BroadcastReceiver {
    public static final String EXTRA_NOTE_ID = "noteId";

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    public void onReceive(@NonNull Context context, @NonNull Intent intent) {
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        if (geofencingEvent == null || geofencingEvent.hasError()) {
            return;
        }

        int geofenceTransition = geofencingEvent.getGeofenceTransition();
        if (geofenceTransition != Geofence.GEOFENCE_TRANSITION_ENTER &&
            geofenceTransition != Geofence.GEOFENCE_TRANSITION_EXIT) {
            return;
        }

        List<Geofence> triggeringGeofences = geofencingEvent.getTriggeringGeofences();
        if (triggeringGeofences == null || triggeringGeofences.isEmpty()) {
            return;
        }

        executor.execute(() -> {
            AppDatabase db = AppDatabase.get(context);
            NoteRepository repo = ServiceLocator.noteRepository(context);
            GeofenceManager geofenceManager = ServiceLocator.geofenceManager(context);

            for (Geofence geofence : triggeringGeofences) {
                String geofenceId = geofence.getRequestId();
                if (geofenceId == null) {
                    continue;
                }

                // Handle both note geofences (note-123) and template geofences (template-office, template-home, etc.)
                if (geofenceId.startsWith("note-")) {
                    try {
                        long noteId = Long.parseLong(geofenceId.substring(5));
                        NoteEntity note = db.noteDao().getById(noteId);
                        if (note == null) {
                            continue;
                        }

                        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
                            // Show notification and mark as relevant (no expiration)
                            NotificationHelper notificationHelper = new NotificationHelper(context);
                            notificationHelper.showReminder(note);
                            repo.markRelevantForGeofenceEnter(noteId);
                        } else if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
                            // Remove from relevant
                            repo.markRelevantForGeofenceExit(noteId);
                        }
                    } catch (NumberFormatException e) {
                        // Invalid geofence ID format
                    }
                }

                // Track active geofences for template prioritization
                if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
                    geofenceManager.addToActiveGeofences(geofenceId);
                } else if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
                    geofenceManager.removeFromActiveGeofences(geofenceId);
                }
            }
        });
    }
}
