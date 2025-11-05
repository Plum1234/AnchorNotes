package com.example.anchornotes.data.db;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "notes")
public class NoteEntity {
    @PrimaryKey(autoGenerate = true) public long id;
    public String title;
    public String bodyHtml;
    public boolean hasPhoto;
    public String photoUri;
    public boolean hasVoice;
    public String voiceUri;
    public boolean pinned;
    public long createdAt;
    public long updatedAt;
    //adding
    public Double latitude;
    public Double longitude;
    public String locationLabel;
    
    // Reminder fields
    @androidx.annotation.Nullable
    public String reminderType; // "TIME" | "GEOFENCE" | null
    @androidx.annotation.Nullable
    public Long reminderAt; // millis if TIME
    @androidx.annotation.Nullable
    public String geofenceId; // stable id like "note-{id}"
    public boolean pendingActivation; // true if geofence registration failed

    public NoteEntity() {} // Room
    
    @Ignore
    public NoteEntity(long id, String title, String bodyHtml,
                      boolean hasPhoto, String photoUri,
                      boolean hasVoice, String voiceUri,
                      boolean pinned, long createdAt, long updatedAt) {
        this.id = id;
        this.title = title;
        this.bodyHtml = bodyHtml;
        this.hasPhoto = hasPhoto;
        this.photoUri = photoUri;
        this.hasVoice = hasVoice;
        this.voiceUri = voiceUri;
        this.pinned = pinned;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
