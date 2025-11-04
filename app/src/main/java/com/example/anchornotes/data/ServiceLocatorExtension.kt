package com.example.anchornotes.data

import android.content.Context
import com.example.anchornotes.data.db.AppDatabase
import com.example.anchornotes.data.repo.NoteSearchRepository

/**
 * Kotlin extension for ServiceLocator to provide search repository.
 */
object ServiceLocatorExtension {
    fun noteSearchRepository(context: Context): NoteSearchRepository {
        val database = AppDatabase.get(context)
        return NoteSearchRepository(
            database.noteSearchDao(),
            database.tagDao(),
            database
        )
    }
}

