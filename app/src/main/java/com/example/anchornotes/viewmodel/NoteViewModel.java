package com.example.anchornotes.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.anchornotes.data.ServiceLocator;
import com.example.anchornotes.data.repo.NoteRepository;
import com.example.anchornotes.model.PlaceSelection;
import com.example.anchornotes.model.ReminderConflict;
import com.example.anchornotes.model.ReminderType;
import com.example.anchornotes.model.RelevantNoteUi;
import com.example.anchornotes.util.SingleLiveEvent;

import java.util.List;

/**
 * ViewModel for managing note reminders and relevant notes.
 */
public class NoteViewModel extends AndroidViewModel {
    private final NoteRepository repository;
    private final LiveData<List<RelevantNoteUi>> relevantNotes;
    private final SingleLiveEvent<ReplaceDialogPayload> replaceDialogEvent = new SingleLiveEvent<>();

    public NoteViewModel(@NonNull Application application) {
        super(application);
        repository = ServiceLocator.noteRepository(application);
        relevantNotes = repository.getRelevantLive();
    }

    public LiveData<List<RelevantNoteUi>> getRelevantNotes() {
        return relevantNotes;
    }

    public SingleLiveEvent<ReplaceDialogPayload> getReplaceDialogEvent() {
        return replaceDialogEvent;
    }

    /**
     * Sets a time reminder for a note.
     */
    public void onSetTimeReminder(long noteId, long atMillis) {
        repository.requestSetTimeReminder(
                noteId,
                atMillis,
                conflict -> {
                    // Conflict detected, show dialog - handled in UI
                    android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
                    handler.post(() -> {
                        ReplaceDialogPayload payload = new ReplaceDialogPayload(
                                noteId,
                                ReminderType.TIME,
                                atMillis,
                                null
                        );
                        replaceDialogEvent.setValue(payload);
                    });
                },
                () -> {
                    // Successfully scheduled
                },
                error -> {
                    // Handle error (could show toast, etc.)
                    error.printStackTrace();
                }
        );
    }

    /**
     * Sets a geofence reminder for a note.
     */
    public void onSetGeofenceReminder(long noteId, PlaceSelection place) {
        repository.requestSetGeofenceReminder(
                noteId,
                place,
                conflict -> {
                    // Conflict detected, show dialog - handled in UI
                    android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
                    handler.post(() -> {
                        ReplaceDialogPayload payload = new ReplaceDialogPayload(
                                noteId,
                                ReminderType.GEOFENCE,
                                -1,
                                place
                        );
                        replaceDialogEvent.setValue(payload);
                    });
                },
                () -> {
                    // Successfully scheduled
                },
                error -> {
                    // Handle error
                    error.printStackTrace();
                }
        );
    }

    /**
     * Confirms replacement of existing reminder (called from dialog).
     */
    public void onConfirmReplace(ReminderType newType, long noteId, long atMillis, PlaceSelection place) {
        if (newType == ReminderType.TIME) {
            repository.confirmReplaceWithTime(
                    noteId,
                    atMillis,
                    () -> {},
                    error -> error.printStackTrace()
            );
        } else if (newType == ReminderType.GEOFENCE) {
            repository.confirmReplaceWithGeofence(
                    noteId,
                    place,
                    () -> {},
                    error -> error.printStackTrace()
            );
        }
    }

    /**
     * Clears a reminder for a note.
     */
    public void onClearReminder(long noteId) {
        repository.clearReminder(
                noteId,
                () -> {},
                error -> error.printStackTrace()
        );
    }

    /**
     * Payload for showing replace reminder dialog.
     */
    public static class ReplaceDialogPayload {
        public final long noteId;
        public final ReminderType newType;
        public final long atMillis; // -1 if not time reminder
        public final PlaceSelection place; // null if not geofence reminder

        public ReplaceDialogPayload(long noteId, ReminderType newType, long atMillis, PlaceSelection place) {
            this.noteId = noteId;
            this.newType = newType;
            this.atMillis = atMillis;
            this.place = place;
        }
    }
}
