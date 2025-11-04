package com.example.anchornotes.data.db;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;

@Entity(
        tableName = "note_tag_cross_ref",   // << change here
        primaryKeys = {"noteId", "tagId"},
        foreignKeys = {
                @ForeignKey(entity = NoteEntity.class, parentColumns = "id", childColumns = "noteId", onDelete = ForeignKey.CASCADE),
                @ForeignKey(entity = TagEntity.class, parentColumns = "id", childColumns = "tagId", onDelete = ForeignKey.CASCADE)
        },
        indices = {@Index("noteId"), @Index("tagId")}
)
public class NoteTagCrossRef {
    public long noteId;
    public long tagId;

    public NoteTagCrossRef(long noteId, long tagId) {
        this.noteId = noteId;
        this.tagId = tagId;
    }
}

