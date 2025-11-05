package com.example.anchornotes.model;

import com.example.anchornotes.data.db.NoteEntity;

/**
 * UI model for a relevant note, combining note data with expiration info.
 */
public class RelevantNoteUi {
    public final NoteEntity note;
    public final long expiresAt;

    public RelevantNoteUi(NoteEntity note, long expiresAt) {
        this.note = note;
        this.expiresAt = expiresAt;
    }
}
