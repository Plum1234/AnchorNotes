package com.example.anchornotes.white_tests;

import android.content.Context;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;

import com.example.anchornotes.data.db.AppDatabase;
import com.example.anchornotes.data.db.NoteDao;
import com.example.anchornotes.data.db.NoteEntity;
import com.example.anchornotes.data.repo.NoteRepository;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.List;

import static org.junit.Assert.*;

/**
 * NoteRepository.insertNote PersistsNote Test
 */
@RunWith(JUnit4.class)
public class InsertNoteTest {

    private AppDatabase database;
    private NoteDao noteDao;
    private NoteRepository repository;
    private Context context;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        // Use in-memory database for testing
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .allowMainThreadQueries()
                .build();
        noteDao = database.noteDao();
        repository = new NoteRepository(noteDao);
    }

    @After
    public void tearDown() {
        database.close();
    }

    /**
     * NoteRepository.insertNote PersistsNote
     * 
     * Description: Test that createOrUpdate persists a note to the database
     */
    @Test
    public void insertNote_persistsToDao() {
        // Create a note
        long noteId = repository.createOrUpdate(null, "Test Note", "Test content", null, null, false);
        
        assertTrue("Note ID should be greater than 0", noteId > 0);
        
        // Retrieve all notes
        List<NoteEntity> allNotes = repository.getAll();
        
        // Verify note is present
        assertNotNull("Notes list should not be null", allNotes);
        assertFalse("Notes list should not be empty", allNotes.isEmpty());
        
        NoteEntity found = null;
        for (NoteEntity note : allNotes) {
            if (note.id == noteId) {
                found = note;
                break;
            }
        }
        
        assertNotNull("Note should be found in database", found);
        assertEquals("Note title should match", "Test Note", found.title);
        assertEquals("Note body should match", "Test content", found.bodyHtml);
    }
}

