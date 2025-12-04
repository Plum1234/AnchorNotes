package com.example.anchornotes.data.repo;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import com.example.anchornotes.context.GeofenceManager;
import com.example.anchornotes.context.ReminderManager;
import com.example.anchornotes.data.db.AppDatabase;
import com.example.anchornotes.data.db.NoteDao;
import com.example.anchornotes.data.db.NoteEntity;
import com.example.anchornotes.data.db.NoteTagCrossRef;
import com.example.anchornotes.data.db.NoteTagCrossRefDao;
import com.example.anchornotes.data.db.RelevantDao;
import com.example.anchornotes.data.db.RelevantNoteEntity;
import com.example.anchornotes.data.db.TagDao;
import com.example.anchornotes.data.db.TagEntity;
import com.example.anchornotes.model.PlaceSelection;
import com.example.anchornotes.model.ReminderConflict;
import com.example.anchornotes.model.ReminderType;
import com.example.anchornotes.model.RelevantNoteUi;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class NoteRepository {
    private static final ExecutorService ioExecutor = Executors.newFixedThreadPool(4);

    private final NoteDao noteDao;
    private final RelevantDao relevantDao;
    private final Context appContext;
    private ReminderManager reminderManager;
    private GeofenceManager geofenceManager;
    private TagDao tagDao;
    private NoteTagCrossRefDao refDao;

    public NoteRepository(NoteDao noteDao) {
        this.noteDao = noteDao;
        this.relevantDao = null;
        this.appContext = null;
    }

    public NoteRepository(NoteDao noteDao, RelevantDao relevantDao, Context appContext) {
        this.noteDao = noteDao;
        this.relevantDao = relevantDao;
        this.appContext = appContext.getApplicationContext();
        this.reminderManager = new ReminderManager(this.appContext);
        this.geofenceManager = new GeofenceManager(this.appContext);
        AppDatabase db = AppDatabase.get(this.appContext);
        this.tagDao = db.tagDao();
        this.refDao = db.noteTagCrossRefDao();
    }

    public List<NoteEntity> getAll() {
        return noteDao.getAll();
    }

    /** Get one note for editor prefill. */
    public NoteEntity get(long id) {                // ← added
        return noteDao.getById(id);
    }

    /** Create or update a note; preserves createdAt on update. */
    public long createOrUpdate(Long id,
                               String title,
                               String bodyHtml,
                               String photoUri,
                               String voiceUri,
                               boolean pinned) {
        long now = System.currentTimeMillis();

        if (id == null || id == 0L) {
            NoteEntity e = new NoteEntity(
                    0,
                    safe(title),
                    safe(bodyHtml),
                    notEmpty(photoUri),
                    photoUri,
                    notEmpty(voiceUri),
                    voiceUri,
                    pinned,
                    now,         // createdAt
                    now          // updatedAt
            );
            return noteDao.insert(e);
        } else {
            NoteEntity old = noteDao.getById(id);
            long created = (old != null ? old.createdAt : now); // preserve
            NoteEntity e = new NoteEntity(
                    id,
                    safe(title),
                    safe(bodyHtml),
                    notEmpty(photoUri),
                    photoUri,
                    notEmpty(voiceUri),
                    voiceUri,
                    pinned,
                    created,     // keep original createdAt
                    now          // updatedAt
            );
            noteDao.update(e);
            return id;
        }
    }

    public void setPinned(long noteId, boolean pinned) {
        noteDao.setPinned(noteId, pinned, System.currentTimeMillis());
    }

    public void setLocation(long noteId, Double lat, Double lon, String label) {
        noteDao.updateLocation(noteId, lat, lon, label, System.currentTimeMillis());
    }

    // ========== Reminder Management ==========

    /**
     * Requests to set a time reminder. Checks for conflicts and calls onConflict if another reminder exists.
     */
    public void requestSetTimeReminder(long noteId, long atMillis,
                                       Consumer<ReminderConflict> onConflict,
                                       Runnable onScheduled,
                                       Consumer<Throwable> onError) {
        ioExecutor.execute(() -> {
            NoteEntity note = noteDao.getById(noteId);
            if (note == null) {
                onError.accept(new IllegalArgumentException("Note not found"));
                return;
            }

            ReminderType existingType = parseReminderType(note.reminderType);
            if (existingType != ReminderType.NONE && existingType != ReminderType.TIME) {
                ReminderConflict conflict = new ReminderConflict(existingType, ReminderType.TIME);
                onConflict.accept(conflict);
                return;
            }

            // No conflict, proceed
            confirmReplaceWithTime(noteId, atMillis, onScheduled, onError);
        });
    }

    /**
     * Requests to set a geofence reminder. Checks for conflicts.
     */
    public void requestSetGeofenceReminder(long noteId, PlaceSelection place,
                                           Consumer<ReminderConflict> onConflict,
                                           Runnable onScheduled,
                                           Consumer<Throwable> onError) {
        ioExecutor.execute(() -> {
            NoteEntity note = noteDao.getById(noteId);
            if (note == null) {
                onError.accept(new IllegalArgumentException("Note not found"));
                return;
            }

            ReminderType existingType = parseReminderType(note.reminderType);
            if (existingType != ReminderType.NONE && existingType != ReminderType.GEOFENCE) {
                ReminderConflict conflict = new ReminderConflict(existingType, ReminderType.GEOFENCE);
                onConflict.accept(conflict);
                return;
            }

            // No conflict, proceed
            confirmReplaceWithGeofence(noteId, place, onScheduled, onError);
        });
    }

    /**
     * Confirms replacement with a time reminder (called after user confirms conflict dialog).
     */
    public void confirmReplaceWithTime(long noteId, long atMillis,
                                       Runnable onScheduled,
                                       Consumer<Throwable> onError) {
        ioExecutor.execute(() -> {
            try {
                // Cancel existing reminder
                NoteEntity note = noteDao.getById(noteId);
                if (note != null) {
                    if (ReminderType.GEOFENCE == parseReminderType(note.reminderType)) {
                        if (note.geofenceId != null && geofenceManager != null) {
                            geofenceManager.removeForNote(note.geofenceId);
                        }
                    } else if (reminderManager != null) {
                        reminderManager.cancel(noteId);
                    }
                }

                long now = System.currentTimeMillis();
                noteDao.setReminderTime(noteId, atMillis, now);

                if (reminderManager != null) {
                    reminderManager.scheduleExact(noteId, atMillis);
                }

                onScheduled.run();
            } catch (Exception e) {
                onError.accept(e);
            }
        });
    }

    /**
     * Confirms replacement with a geofence reminder (called after user confirms conflict dialog).
     */
    public void confirmReplaceWithGeofence(long noteId, PlaceSelection place,
                                           Runnable onScheduled,
                                           Consumer<Throwable> onError) {
        if (geofenceManager == null || appContext == null) {
            onError.accept(new IllegalStateException("GeofenceManager not initialized"));
            return;
        }

        ioExecutor.execute(() -> {
            try {
                // Cancel existing reminder
                NoteEntity note = noteDao.getById(noteId);
                if (note != null) {
                    if (ReminderType.TIME == parseReminderType(note.reminderType)) {
                        if (reminderManager != null) {
                            reminderManager.cancel(noteId);
                        }
                    } else if (note.geofenceId != null) {
                        geofenceManager.removeForNote(note.geofenceId);
                    }
                }

                String geofenceId = "note-" + noteId;
                long now = System.currentTimeMillis();
                noteDao.setReminderGeofence(noteId, geofenceId, now);
                noteDao.updateLocation(noteId, place.latitude, place.longitude, place.label, now);

                geofenceManager.addForNote(
                        noteId,
                        place.latitude,
                        place.longitude,
                        place.radiusMeters,
                        geofenceId,
                        result -> onScheduled.run(),
                        error -> {
                            // Mark as pending and schedule retry worker
                            noteDao.setPendingActivation(noteId, true);
                            // Schedule retry worker
                            androidx.work.OneTimeWorkRequest workRequest = new androidx.work.OneTimeWorkRequest.Builder(
                                    com.example.anchornotes.worker.GeofenceRetryWorker.class)
                                    .setInitialDelay(java.util.concurrent.TimeUnit.MINUTES.toMillis(5), java.util.concurrent.TimeUnit.MILLISECONDS)
                                    .setBackoffCriteria(
                                            androidx.work.BackoffPolicy.EXPONENTIAL,
                                            java.util.concurrent.TimeUnit.MINUTES.toMillis(5),
                                            java.util.concurrent.TimeUnit.MILLISECONDS)
                                    .build();
                            androidx.work.WorkManager.getInstance(appContext).enqueue(workRequest);
                            onError.accept(error);
                        }
                );
            } catch (Exception e) {
                onError.accept(e);
            }
        });
    }

    /**
     * Clears a reminder for a note.
     */
    public void clearReminder(long noteId, Runnable onCleared, Consumer<Throwable> onError) {
        ioExecutor.execute(() -> {
            try {
                NoteEntity note = noteDao.getById(noteId);
                if (note != null) {
                    if (ReminderType.GEOFENCE == parseReminderType(note.reminderType)) {
                        if (note.geofenceId != null && geofenceManager != null) {
                            geofenceManager.removeForNote(note.geofenceId);
                        }
                    } else if (reminderManager != null) {
                        reminderManager.cancel(noteId);
                    }
                }

                long now = System.currentTimeMillis();
                noteDao.clearReminder(noteId, now);

                if (relevantDao != null) {
                    relevantDao.delete(noteId);
                }

                onCleared.run();
            } catch (Exception e) {
                onError.accept(e);
            }
        });
    }

    /**
     * Deletes a note and cleans up associated resources (geofences, reminders, relevant notes).
     */
    public void deleteNote(long noteId) {
        ioExecutor.execute(() -> {
            try {
                NoteEntity note = noteDao.getById(noteId);
                if (note == null) {
                    return; // Note doesn't exist, nothing to delete
                }

                // Clean up geofence if exists
                if (note.geofenceId != null && geofenceManager != null) {
                    geofenceManager.removeForNote(note.geofenceId);
                }

                // Clean up reminder if exists
                ReminderType reminderType = parseReminderType(note.reminderType);
                if (reminderType == ReminderType.TIME && reminderManager != null) {
                    reminderManager.cancel(noteId);
                } else if (reminderType == ReminderType.GEOFENCE && note.geofenceId != null && geofenceManager != null) {
                    geofenceManager.removeForNote(note.geofenceId);
                }

                // Remove from relevant notes if exists
                if (relevantDao != null) {
                    relevantDao.delete(noteId);
                }

                // Delete the note
                noteDao.delete(note);
            } catch (Exception e) {
                // Log error but don't throw - deletion should be best effort
                android.util.Log.e("NoteRepository", "Error deleting note: " + e.getMessage(), e);
            }
        });
    }

    // ========== Relevant Notes Management ==========

    /**
     * Marks a note as relevant for a time reminder (expires in 20 seconds for demo).
     * NOTE: For production, change to 3600000L (1 hour)
     */
    public void markRelevantForTime(long noteId, long now) {
        if (relevantDao == null) return;
        ioExecutor.execute(() -> {
            long expiresAt = now + 20000L; // 20 seconds for demo (20 * 1000)
            // For production use: now + 3600000L (1 hour)
            RelevantNoteEntity entity = new RelevantNoteEntity(noteId, expiresAt);
            relevantDao.upsert(entity);
        });
    }

    /**
     * Marks a note as relevant for geofence enter (no expiration).
     */
    public void markRelevantForGeofenceEnter(long noteId) {
        if (relevantDao == null) return;
        ioExecutor.execute(() -> {
            RelevantNoteEntity entity = new RelevantNoteEntity(noteId, Long.MAX_VALUE);
            relevantDao.upsert(entity);
        });
    }

    /**
     * Removes a note from relevant when exiting geofence.
     */
    public void markRelevantForGeofenceExit(long noteId) {
        if (relevantDao == null) return;
        ioExecutor.execute(() -> {
            relevantDao.delete(noteId);
        });
    }

    /**
     * Gets LiveData of relevant notes.
     */
    public LiveData<List<RelevantNoteUi>> getRelevantLive() {
        if (relevantDao == null) {
            return new androidx.lifecycle.MutableLiveData<>();
        }
        LiveData<List<RelevantNoteEntity>> relevant = relevantDao.liveRelevant(System.currentTimeMillis());
        return Transformations.map(relevant, entities -> {
            java.util.ArrayList<RelevantNoteUi> result = new java.util.ArrayList<>();
            for (RelevantNoteEntity entity : entities) {
                NoteEntity note = noteDao.getById(entity.noteId);
                if (note != null) {
                    result.add(new RelevantNoteUi(note, entity.expiresAt));
                }
            }
            return result;
        });
    }

    /**
     * Cleans up expired relevant notes.
     */
    public void cleanupExpired() {
        if (relevantDao == null) return;
        ioExecutor.execute(() -> {
            long now = System.currentTimeMillis();
            relevantDao.expire(now);
        });
    }

    // ========== Helpers ==========

    private ReminderType parseReminderType(String type) {
        if (type == null) return ReminderType.NONE;
        if ("TIME".equals(type)) return ReminderType.TIME;
        if ("GEOFENCE".equals(type)) return ReminderType.GEOFENCE;
        return ReminderType.NONE;
    }

    // ========== Tag Management ==========

    /**
     * Gets all tags associated with a note.
     */
    public List<TagEntity> getTagsForNote(long noteId) {
        if (tagDao == null) {
            AppDatabase db = AppDatabase.get(appContext);
            tagDao = db.tagDao();
        }
        return tagDao.getTagsForNote(noteId);
    }

    /**
     * Removes a tag from a note.
     */
    public void removeTagFromNote(long noteId, long tagId) {
        ioExecutor.execute(() -> {
            if (refDao == null) {
                AppDatabase db = AppDatabase.get(appContext);
                refDao = db.noteTagCrossRefDao();
            }
            refDao.delete(noteId, tagId);
        });
    }

    /**
     * Adds a tag to a note. Creates the tag if it doesn't exist.
     */
    public void addTagToNote(long noteId, String tagName) {
        ioExecutor.execute(() -> {
            if (tagDao == null) {
                AppDatabase db = AppDatabase.get(appContext);
                tagDao = db.tagDao();
            }
            if (refDao == null) {
                AppDatabase db = AppDatabase.get(appContext);
                refDao = db.noteTagCrossRefDao();
            }
            TagEntity existing = tagDao.getByName(tagName);
            long tagId;
            if (existing != null) {
                tagId = existing.id;
            } else {
                tagId = tagDao.insert(new TagEntity(tagName));
            }
            refDao.insert(new NoteTagCrossRef(noteId, tagId));
        });
    }

    /**
     * Gets all available tags.
     */
    public List<TagEntity> getAllTags() {
        if (tagDao == null) {
            AppDatabase db = AppDatabase.get(appContext);
            tagDao = db.tagDao();
        }
        return tagDao.getAll();
    }

    private static String safe(String s) { return s == null ? "" : s; }
    private static boolean notEmpty(String s) { return s != null && !s.isEmpty(); }
}
