package com.example.anchornotes.data.db;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

/**
 * Entity tracking notes that are currently "relevant" (should appear in Relevant Notes section).
 * expiresAt determines when the note should no longer be shown.
 */
@Entity(tableName = "relevant")
public class RelevantNoteEntity {
    @PrimaryKey
    public long noteId;
    
    public long expiresAt; // System.currentTimeMillis() when this note should expire

    public RelevantNoteEntity() {} // Room
    
    @Ignore
    public RelevantNoteEntity(long noteId, long expiresAt) {
        this.noteId = noteId;
        this.expiresAt = expiresAt;
    }
}
