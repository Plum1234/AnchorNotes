package com.example.anchornotes.data

import android.content.Context
import com.example.anchornotes.data.db.AppDatabase
import com.example.anchornotes.data.db.NoteEntity
import com.example.anchornotes.data.db.TagEntity
import com.example.anchornotes.data.db.NoteTagCrossRef

/**
 * Helper class to populate test data for testing search and filter functionality.
 * 
 * Usage in MainActivity.onCreate():
 * TestDataHelper.populateTestData(this)
 * 
 * Or call from a button click for easy testing.
 */
object TestDataHelper {
    
    fun populateTestData(context: Context) {
        val database = AppDatabase.get(context)
        val noteDao = database.noteDao()
        val tagDao: com.example.anchornotes.data.db.TagDao = database.tagDao()
        val crossRefDao: com.example.anchornotes.data.db.NoteTagCrossRefDao = database.noteTagCrossRefDao()
        
        val now = System.currentTimeMillis()
        val oneDayAgo = now - (24 * 60 * 60 * 1000)
        val twoDaysAgo = now - (2 * 24 * 60 * 60 * 1000)
        
        // Create test tags
        val workTag = TagEntity(0, "Work")
        val personalTag = TagEntity(0, "Personal")
        val shoppingTag = TagEntity(0, "Shopping")
        val workTagId = tagDao.insert(workTag)
        val personalTagId = tagDao.insert(personalTag)
        val shoppingTagId = tagDao.insert(shoppingTag)
        
        // Create test notes - NoteEntity constructor matches Java version
        val note1Id = noteDao.insert(NoteEntity(
            0, "Shopping List", "Milk, Eggs, Bread, Cheese",
            false, null, false, null, false, now, now
        ))
        
        val note2Id = noteDao.insert(NoteEntity(
            0, "Meeting Notes", "Discuss project timeline and deliverables",
            false, null, false, null, true, oneDayAgo, oneDayAgo  // pinned
        ))
        
        val note3Id = noteDao.insert(NoteEntity(
            0, "Recipe Ideas", "Try pasta carbonara and risotto",
            false, null, false, null, false, twoDaysAgo, twoDaysAgo
        ))
        
        val note4Id = noteDao.insert(NoteEntity(
            0, "Important Task", "Finish assignment by Friday",
            false, null, false, null, true, now, now  // pinned
        ))
        
        val note5Id = noteDao.insert(NoteEntity(
            0, "Work Project", "Update documentation and code review",
            true, "content://photo", false, null, false, now, now  // has photo
        ))
        
        val note6Id = noteDao.insert(NoteEntity(
            0, "Voice Memo", "Remember to call client tomorrow",
            false, null, true, "content://voice", false, oneDayAgo, oneDayAgo  // has voice
        ))
        
        // Link notes to tags
        crossRefDao.insert(NoteTagCrossRef(note1Id, shoppingTagId))
        crossRefDao.insert(NoteTagCrossRef(note2Id, workTagId))
        crossRefDao.insert(NoteTagCrossRef(note4Id, workTagId))
        crossRefDao.insert(NoteTagCrossRef(note5Id, workTagId))
        crossRefDao.insert(NoteTagCrossRef(note3Id, personalTagId))
    }
}

