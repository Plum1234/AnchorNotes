package com.example.anchornotes.data.repo

import androidx.lifecycle.LiveData
import androidx.room.RoomDatabase
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import com.example.anchornotes.data.NoteSearchFilter
import com.example.anchornotes.data.db.*

/**
 * Kotlin extension/functions for search functionality.
 * Builds dynamic SQL queries based on NoteSearchFilter.
 */
class NoteSearchRepository(
    private val searchDao: NoteSearchDao,
    private val tagDao: TagDao,
    private val database: RoomDatabase
) {
    
    /**
     * Search notes with filters.
     * Builds a dynamic query based on the filter parameters.
     */
    fun searchNotes(filter: NoteSearchFilter): LiveData<List<NoteEntity>> {
        val query = buildSearchQuery(filter)
        return searchDao.searchNotes(query)
    }
    
    /**
     * Get all tags for filter dialog.
     */
    fun getAllTags(): LiveData<List<TagEntity>> {
        return tagDao.getAllTags()
    }
    
    /**
     * Build a dynamic SQL query based on filter parameters.
     * Uses ANY tag matching (OR logic) - note matches if it has ANY selected tag.
     */
    private fun buildSearchQuery(filter: NoteSearchFilter): SupportSQLiteQuery {
        val conditions = mutableListOf<String>()
        val args = mutableListOf<Any>()
        
        // Search query: title OR bodyHtml LIKE
        if (!filter.query.isNullOrBlank()) {
            val searchPattern = "%${filter.query}%"
            conditions.add("(title LIKE ? OR bodyHtml LIKE ?)")
            args.add(searchPattern)
            args.add(searchPattern)
        }
        
        // Date range filter
        if (filter.fromDate != null && filter.toDate != null) {
            conditions.add("updatedAt BETWEEN ? AND ?")
            args.add(filter.fromDate)
            args.add(filter.toDate)
        } else if (filter.fromDate != null) {
            conditions.add("updatedAt >= ?")
            args.add(filter.fromDate)
        } else if (filter.toDate != null) {
            conditions.add("updatedAt <= ?")
            args.add(filter.toDate)
        }
        
        // Has photo filter
        if (filter.hasPhoto == true) {
            conditions.add("hasPhoto = 1")
        }
        
        // Has voice filter
        if (filter.hasVoice == true) {
            conditions.add("hasVoice = 1")
        }
        
        // Has location filter (check if latitude is not null)
        // TODO: NoteEntity currently doesn't have latitude/longitude fields.
        // When location fields are added to NoteEntity, uncomment this:
        // if (filter.hasLocation == true) {
        //     conditions.add("latitude IS NOT NULL AND longitude IS NOT NULL")
        // }
        // For now, location filter is disabled
        
        // Tag filter: ANY tag matching
        // If note has ANY of the selected tags, include it
        if (filter.tagIds.isNotEmpty()) {
            val placeholders = filter.tagIds.joinToString(",") { "?" }
            conditions.add("""
                id IN (
                    SELECT DISTINCT noteId FROM note_tag_cross_ref
                    WHERE tagId IN ($placeholders)
                )
            """.trimIndent())
            args.addAll(filter.tagIds)
        }
        
        // Build the WHERE clause
        val whereClause = if (conditions.isNotEmpty()) {
            "WHERE ${conditions.joinToString(" AND ")}"
        } else {
            ""
        }
        
        // Build the full query
        val sql = """
            SELECT DISTINCT * FROM notes
            $whereClause
            ORDER BY pinned DESC, updatedAt DESC
        """.trimIndent()
        
        // Create SupportSQLiteQuery with bound arguments
        return SimpleSQLiteQuery(sql, args.toTypedArray())
    }
}

