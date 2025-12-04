package com.example.anchornotes.white_tests;

import android.content.Context;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;

import com.example.anchornotes.data.db.AppDatabase;
import com.example.anchornotes.data.db.NoteDao;
import com.example.anchornotes.data.db.NoteEntity;
import com.example.anchornotes.data.db.NoteTagCrossRef;
import com.example.anchornotes.data.db.NoteTagCrossRefDao;
import com.example.anchornotes.data.db.TagDao;
import com.example.anchornotes.data.db.TagEntity;
import com.example.anchornotes.data.repo.NoteRepository;
import com.example.anchornotes.data.repo.NoteSearchRepository;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * NoteRepository.getNotesByTag ReturnsOnlyMatching Test
 */
@RunWith(JUnit4.class)
public class GetNotesByTagTest {

    private AppDatabase database;
    private NoteDao noteDao;
    private TagDao tagDao;
    private NoteTagCrossRefDao crossRefDao;
    private NoteRepository repository;
    private NoteSearchRepository searchRepository;
    private Context context;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        // Use in-memory database for testing
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .allowMainThreadQueries()
                .build();
        noteDao = database.noteDao();
        tagDao = database.tagDao();
        crossRefDao = database.noteTagCrossRefDao();
        repository = new NoteRepository(noteDao);
        searchRepository = new NoteSearchRepository(database.noteSearchDao());
    }

    @After
    public void tearDown() {
        database.close();
    }

    /**
     * NoteRepository.getNotesByTag ReturnsOnlyMatching
     * 
     * Description: Test tag filtering using NoteSearchRepository
     * 
     * Note: Adapted to use NoteSearchRepository.search() with tagIds parameter
     */
    @Test
    public void getNotesByTag_returnsFiltered() {
        // Create tags
        TagEntity workTag = new TagEntity();
        workTag.name = "work";
        long workTagId = tagDao.insert(workTag);
        
        TagEntity personalTag = new TagEntity();
        personalTag.name = "personal";
        long personalTagId = tagDao.insert(personalTag);
        
        // Create notes
        long workNoteId = repository.createOrUpdate(null, "Work Note", "Content", null, null, false);
        long personalNoteId = repository.createOrUpdate(null, "Personal Note", "Content", null, null, false);
        long bothNoteId = repository.createOrUpdate(null, "Both Note", "Content", null, null, false);
        
        // Link tags
        crossRefDao.insert(new NoteTagCrossRef(workNoteId, workTagId));
        crossRefDao.insert(new NoteTagCrossRef(personalNoteId, personalTagId));
        crossRefDao.insert(new NoteTagCrossRef(bothNoteId, workTagId));
        crossRefDao.insert(new NoteTagCrossRef(bothNoteId, personalTagId));
        
        // Search for notes with #work tag
        List<Long> workTagIds = new ArrayList<>();
        workTagIds.add(workTagId);
        
        List<NoteEntity> results = searchRepository.search(
                null, // no text query
                workTagIds,
                null, null, null, null, null // no other filters
        );
        
        // Verify results
        assertNotNull("Results should not be null", results);
        assertFalse("Results should not be empty", results.isEmpty());
        
        // Verify all results have the work tag
        boolean foundWorkNote = false;
        boolean foundBothNote = false;
        boolean foundPersonalNote = false;
        
        for (NoteEntity note : results) {
            if (note.id == workNoteId) foundWorkNote = true;
            if (note.id == bothNoteId) foundBothNote = true;
            if (note.id == personalNoteId) foundPersonalNote = true;
        }
        
        assertTrue("Work note should be in results", foundWorkNote);
        assertTrue("Both note should be in results", foundBothNote);
        assertFalse("Personal note should NOT be in results", foundPersonalNote);
    }
}

