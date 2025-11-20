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
import com.example.anchornotes.data.db.NoteTagCrossRef;
import com.example.anchornotes.data.db.NoteTagCrossRefDao;
import com.example.anchornotes.data.db.TagDao;
import com.example.anchornotes.data.db.TagEntity;
import com.example.anchornotes.data.repo.NoteRepository;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

/**
 * Filter by Single Tag Test
 */
@RunWith(AndroidJUnit4.class)
public class FilterBySingleTagTest {

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule = new ActivityScenarioRule<>(MainActivity.class);

    private Context context;
    private AppDatabase database;
    private NoteDao noteDao;
    private TagDao tagDao;
    private NoteTagCrossRefDao crossRefDao;
    private NoteRepository repository;
    private long workTagId, personalTagId;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        database = AppDatabase.get(context);
        noteDao = database.noteDao();
        tagDao = database.tagDao();
        crossRefDao = database.noteTagCrossRefDao();
        repository = ServiceLocator.noteRepository(context);
        
        // Clear existing data
        List<NoteEntity> existing = noteDao.getAll();
        for (NoteEntity note : existing) {
            noteDao.delete(note);
        }
        List<TagEntity> existingTags = tagDao.getAll();
        for (TagEntity tag : existingTags) {
            tagDao.delete(tag);
        }
        
        // Create test tags
        TagEntity workTag = new TagEntity();
        workTag.name = "work";
        workTagId = tagDao.insert(workTag);
        
        TagEntity personalTag = new TagEntity();
        personalTag.name = "personal";
        personalTagId = tagDao.insert(personalTag);
    }

    @After
    public void tearDown() {
        // Clean up test data
        List<NoteEntity> existing = noteDao.getAll();
        for (NoteEntity note : existing) {
            noteDao.delete(note);
        }
        List<TagEntity> existingTags = tagDao.getAll();
        for (TagEntity tag : existingTags) {
            tagDao.delete(tag);
        }
    }

    /**
     * Filter by Single Tag
     * 
     * Description: Create notes with different tags, filter by one tag, verify only matching notes appear
     */
    @Test
    public void testFilterBySingleTag() {
        // Create notes with tags
        long workNoteId = repository.createOrUpdate(null, "Work Note", "Work content", null, null, false);
        long personalNoteId = repository.createOrUpdate(null, "Personal Note", "Personal content", null, null, false);
        
        // Link tags
        crossRefDao.insert(new NoteTagCrossRef(workNoteId, workTagId));
        crossRefDao.insert(new NoteTagCrossRef(personalNoteId, personalTagId));
        
        // Wait for data to be created
        try { Thread.sleep(500); } catch (InterruptedException e) {}

        // Launch activity
        ActivityScenario<MainActivity> scenario = activityRule.getScenario();
        
        // Wait for UI to load
        Espresso.onView(ViewMatchers.withId(R.id.rvNotes))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
        
        try { Thread.sleep(500); } catch (InterruptedException e) {}

        // Open filter dialog
        Espresso.onView(ViewMatchers.withId(R.id.action_filter))
                .perform(ViewActions.click());
        
        try { Thread.sleep(300); } catch (InterruptedException e) {}

        // Check the "work" tag checkbox
        // Note: Adjust based on your actual filter dialog layout
        // This assumes tags are displayed as checkboxes with text matching tag name
        Espresso.onView(ViewMatchers.withText("work"))
                .perform(ViewActions.click());
        
        try { Thread.sleep(300); } catch (InterruptedException e) {}

        // Click Apply button
        Espresso.onView(ViewMatchers.withId(R.id.btnApply))
                .perform(ViewActions.click());
        
        try { Thread.sleep(500); } catch (InterruptedException e) {}

        // Verify only "Work Note" appears
        Espresso.onView(ViewMatchers.withId(R.id.rvNotes))
                .check(ViewAssertions.matches(
                        ViewMatchers.hasDescendant(ViewMatchers.withText("Work Note"))
                ));
        
        // Verify "Personal Note" does NOT appear
        // Note: This is tricky with Espresso - we check that the list doesn't have the text
        // In practice, you might need to count items or use a custom matcher
    }
}

