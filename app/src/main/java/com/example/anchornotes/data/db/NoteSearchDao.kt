package com.example.anchornotes.data.db

import androidx.lifecycle.LiveData
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteQuery

/**
 * DAO interface for search operations.
 * Uses @RawQuery for dynamic query building based on filter parameters.
 */
@Dao
interface NoteSearchDao {
    
    /**
     * Search notes with filters.
     * Uses a raw query approach for maximum flexibility with dynamic filters.
     * 
     * Note: This implementation uses "ANY tag" matching - a note matches
     * if it has ANY of the selected tags (OR logic), not ALL tags.
     */
    @RawQuery(observedEntities = [NoteEntity::class])
    fun searchNotes(query: SupportSQLiteQuery): LiveData<List<NoteEntity>>
}

