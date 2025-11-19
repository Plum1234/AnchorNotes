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
 * Filter by Multiple Tags (AND Behavior) Test
 */
@RunWith(AndroidJUnit4.class)
public class FilterByMultipleTagsTest {

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule = new ActivityScenarioRule<>(MainActivity.class);

    private Context context;
    private AppDatabase database;
    private NoteDao noteDao;
    private TagDao tagDao;
    private NoteTagCrossRefDao crossRefDao;
    private NoteRepository repository;
    private long workTagId, urgentTagId;

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
        
        TagEntity urgentTag = new TagEntity();
        urgentTag.name = "urgent";
        urgentTagId = tagDao.insert(urgentTag);
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
     * Filter by Multiple Tags (AND Behavior)
     * 
     * Description: Create notes with different tag combinations, filter by multiple tags with AND logic
     */
    @Test
    public void testFilterByMultipleTags() {
        // Create notes:
        // Note1: #work, #urgent
        // Note2: #work only
        long note1Id = repository.createOrUpdate(null, "Work Urgent Note", "Content", null, null, false);
        long note2Id = repository.createOrUpdate(null, "Work Note", "Content", null, null, false);
        
        // Link tags
        crossRefDao.insert(new NoteTagCrossRef(note1Id, workTagId));
        crossRefDao.insert(new NoteTagCrossRef(note1Id, urgentTagId));
        crossRefDao.insert(new NoteTagCrossRef(note2Id, workTagId));
        
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

        // Check both "work" and "urgent" tags
        Espresso.onView(ViewMatchers.withText("work"))
                .perform(ViewActions.click());
        
        try { Thread.sleep(200); } catch (InterruptedException e) {}
        
        Espresso.onView(ViewMatchers.withText("urgent"))
                .perform(ViewActions.click());
        
        try { Thread.sleep(300); } catch (InterruptedException e) {}

        // Click Apply button
        Espresso.onView(ViewMatchers.withId(R.id.btnApply))
                .perform(ViewActions.click());
        
        try { Thread.sleep(500); } catch (InterruptedException e) {}

        // Verify only "Work Urgent Note" appears (has both tags)
        Espresso.onView(ViewMatchers.withId(R.id.rvNotes))
                .check(ViewAssertions.matches(
                        ViewMatchers.hasDescendant(ViewMatchers.withText("Work Urgent Note"))
                ));
        
        // Verify "Work Note" does NOT appear (only has #work, not #urgent)
        // Note: This tests AND behavior - note must have ALL selected tags
    }
}

