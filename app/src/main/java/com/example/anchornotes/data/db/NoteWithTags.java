package com.example.anchornotes.data.db;

import androidx.room.Embedded;
import androidx.room.Junction;
import androidx.room.Relation;
import java.util.List;

public class NoteWithTags {
    @Embedded public NoteEntity note;

    @Relation(
            parentColumn = "id",
            entityColumn = "id",
            associateBy = @Junction(
                    value = NoteTagCrossRef.class,
                    parentColumn = "noteId",
                    entityColumn = "tagId"
            )
    )
    public List<TagEntity> tags;
}
