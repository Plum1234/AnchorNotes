package com.example.anchornotes.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import com.example.anchornotes.data.NoteSearchFilter
import com.example.anchornotes.data.ServiceLocator
import com.example.anchornotes.data.ServiceLocatorExtension
import com.example.anchornotes.data.db.NoteEntity
import com.example.anchornotes.data.repo.NoteSearchRepository

/**
 * ViewModel for note list and search functionality.
 * Handles search queries and filter updates.
 */
class NoteViewModel(application: Application) : AndroidViewModel(application) {
    
    private val searchRepository: NoteSearchRepository = 
        ServiceLocatorExtension.noteSearchRepository(application)
    
    private val _searchFilter = MutableLiveData<NoteSearchFilter>(NoteSearchFilter())
    val searchFilter: LiveData<NoteSearchFilter> = _searchFilter
    
    // Search results - transformed from filter to LiveData<List<NoteEntity>>
    val searchResults: LiveData<List<NoteEntity>> = _searchFilter.switchMap { filter ->
        // If filter is empty (no query, no filters), return all notes
        if (isFilterEmpty(filter)) {
            // Return all notes from regular repository
            // Convert List to LiveData
            val allNotes = ServiceLocator.noteRepository(application).getAll()
            val liveData = MutableLiveData<List<NoteEntity>>()
            liveData.value = allNotes
            liveData
        } else {
            // Use search repository
            searchRepository.searchNotes(filter)
        }
    }
    
    // Get all tags for filter dialog
    val allTags: LiveData<List<com.example.anchornotes.data.db.TagEntity>> = 
        searchRepository.getAllTags()
    
    /**
     * Update the search query text.
     */
    fun updateSearchQuery(query: String) {
        val currentFilter = _searchFilter.value ?: NoteSearchFilter()
        _searchFilter.value = currentFilter.copy(
            query = if (query.isBlank()) null else query
        )
    }
    
    /**
     * Update filters (tags, dates, flags).
     * Preserves the current query.
     */
    fun updateFilters(filter: NoteSearchFilter) {
        val currentQuery = _searchFilter.value?.query
        _searchFilter.value = filter.copy(query = currentQuery)
    }
    
    /**
     * Clear all filters and search query.
     */
    fun clearFilters() {
        _searchFilter.value = NoteSearchFilter()
    }
    
    /**
     * Check if filter is empty (no active filters).
     */
    private fun isFilterEmpty(filter: NoteSearchFilter): Boolean {
        return filter.query.isNullOrBlank() &&
                filter.tagIds.isEmpty() &&
                filter.fromDate == null &&
                filter.toDate == null &&
                filter.hasPhoto == null &&
                filter.hasVoice == null &&
                filter.hasLocation == null
    }
}

