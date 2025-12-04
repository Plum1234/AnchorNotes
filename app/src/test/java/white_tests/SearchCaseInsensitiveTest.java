package com.example.anchornotes.white_tests;

import android.content.Context;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;

import com.example.anchornotes.data.db.AppDatabase;
import com.example.anchornotes.data.db.NoteDao;
import com.example.anchornotes.data.db.NoteEntity;
import com.example.anchornotes.data.repo.NoteRepository;
import com.example.anchornotes.data.repo.NoteSearchRepository;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.List;

import static org.junit.Assert.*;

/**
 * NoteRepository.searchNotes CaseInsensitive Test
 */
@RunWith(JUnit4.class)
public class SearchCaseInsensitiveTest {

    private AppDatabase database;
    private NoteDao noteDao;
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
        repository = new NoteRepository(noteDao);
        searchRepository = new NoteSearchRepository(database.noteSearchDao());
    }

    @After
    public void tearDown() {
        database.close();
    }

    /**
     * NoteRepository.searchNotes CaseInsensitive
     * 
     * Description: Test that search is case-insensitive
     */
    @Test
    public void searchNotes_isCaseInsensitive() {
        // Create notes
        repository.createOrUpdate(null, "Meeting Notes", "Content", null, null, false);
        repository.createOrUpdate(null, "Shopping List", "Content", null, null, false);
        
        // Search with lowercase "meeting"
        List<NoteEntity> results1 = searchRepository.search(
                "meeting",
                null, null, null, null, null, null
        );
        
        // Search with uppercase "MEETING"
        List<NoteEntity> results2 = searchRepository.search(
                "MEETING",
                null, null, null, null, null, null
        );
        
        // Search with mixed case "MeEtInG"
        List<NoteEntity> results3 = searchRepository.search(
                "MeEtInG",
                null, null, null, null, null, null
        );
        
        // All searches should return the same result
        assertNotNull("Results 1 should not be null", results1);
        assertNotNull("Results 2 should not be null", results2);
        assertNotNull("Results 3 should not be null", results3);
        
        // Verify all contain "Meeting Notes"
        boolean found1 = false, found2 = false, found3 = false;
        for (NoteEntity note : results1) {
            if (note.title.equals("Meeting Notes")) found1 = true;
        }
        for (NoteEntity note : results2) {
            if (note.title.equals("Meeting Notes")) found2 = true;
        }
        for (NoteEntity note : results3) {
            if (note.title.equals("Meeting Notes")) found3 = true;
        }
        
        assertTrue("Lowercase search should find note", found1);
        assertTrue("Uppercase search should find note", found2);
        assertTrue("Mixed case search should find note", found3);
    }
}

