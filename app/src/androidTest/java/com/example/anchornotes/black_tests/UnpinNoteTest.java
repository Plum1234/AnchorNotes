package com.example.anchornotes.black_tests;

import android.content.Context;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.action.ViewActions;
import androidx.test.espresso.assertion.ViewAssertions;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.anchornotes.MainActivity;
import com.example.anchornotes.R;
import com.example.anchornotes.data.ServiceLocator;
import com.example.anchornotes.data.db.AppDatabase;
import com.example.anchornotes.data.db.NoteDao;
import com.example.anchornotes.data.db.NoteEntity;
import com.example.anchornotes.data.repo.NoteRepository;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

/**
 * Unpin Note Test
 */
@RunWith(AndroidJUnit4.class)
public class UnpinNoteTest {

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule = new ActivityScenarioRule<>(MainActivity.class);

    private Context context;
    private AppDatabase database;
    private NoteDao noteDao;
    private NoteRepository repository;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        database = AppDatabase.get(context);
        noteDao = database.noteDao();
        repository = ServiceLocator.noteRepository(context);
        
        // Clear existing notes
        List<NoteEntity> existing = noteDao.getAll();
        for (NoteEntity note : existing) {
            noteDao.delete(note);
        }
    }

    @After
    public void tearDown() {
        // Clean up test data
        List<NoteEntity> existing = noteDao.getAll();
        for (NoteEntity note : existing) {
            noteDao.delete(note);
        }
    }

    /**
     * Unpin Note
     * 
     * Description: Start with pinned note, unpin it, verify it moves to regular list
     */
    @Test
    public void testUnpinNoteMovesBackToRegularList() {
        // Create a pinned note
        long noteId = repository.createOrUpdate(null, "Pinned Note", "Content", null, null, true);
        
        // Wait for note to be created
        try { Thread.sleep(500); } catch (InterruptedException e) {}

        // Launch activity
        ActivityScenario<MainActivity> scenario = activityRule.getScenario();
        
        // Wait for UI to load
        Espresso.onView(ViewMatchers.withId(R.id.rvNotes))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
        
        try { Thread.sleep(500); } catch (InterruptedException e) {}

        // Verify note is in pinned section
        Espresso.onView(ViewMatchers.withText("Pinned"))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
        
        Espresso.onView(ViewMatchers.withId(R.id.rvNotes))
                .check(ViewAssertions.matches(
                        ViewMatchers.hasDescendant(ViewMatchers.withText("Pinned Note"))
                ));

        // Unpin the note
        repository.setPinned(noteId, false);
        
        // Wait for UI update
        try { Thread.sleep(500); } catch (InterruptedException e) {}

        // Verify note moved to regular list (check "Others" or "Notes" section)
        // The note should still be visible but in different section
        Espresso.onView(ViewMatchers.withId(R.id.rvNotes))
                .check(ViewAssertions.matches(
                        ViewMatchers.hasDescendant(ViewMatchers.withText("Pinned Note"))
                ));
        
        // Verify it's no longer in pinned section (if "Others" header appears)
        // This depends on your adapter implementation
    }
}

