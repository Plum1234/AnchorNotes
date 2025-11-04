package com.example.anchornotes.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Tag entity for organizing notes.
 */
@Entity(tableName = "tags")
data class TagEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String
)

