package com.example.anchornotes.data.db

import androidx.room.Embedded
import androidx.room.Relation

/**
 * NoteEntity with its associated tags.
 * Used for search results that need tag information.
 */
data class NoteWithTags(
    @Embedded
    val note: NoteEntity,
    
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = androidx.room.Junction(
            value = NoteTagCrossRef::class,
            parentColumn = "noteId",
            entityColumn = "tagId"
        )
    )
    val tags: List<TagEntity> = emptyList()
)

