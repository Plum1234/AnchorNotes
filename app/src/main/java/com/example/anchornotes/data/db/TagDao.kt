package com.example.anchornotes.data.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * DAO for tag operations.
 */
@Dao
interface TagDao {
    @Query("SELECT * FROM tags ORDER BY name ASC")
    fun getAllTags(): LiveData<List<TagEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(tag: TagEntity): Long
}

/**
 * DAO for note-tag cross-reference operations.
 */
@Dao
interface NoteTagCrossRefDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(crossRef: NoteTagCrossRef)
}

