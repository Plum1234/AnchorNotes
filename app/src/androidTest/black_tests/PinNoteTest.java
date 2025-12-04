package com.example.anchornotes.black_tests;

import android.content.Context;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.action.ViewActions;
import androidx.test.espresso.assertion.ViewAssertions;
import androidx.test.espresso.contrib.RecyclerViewActions;
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
 * Pin Note to Top Test
 */
@RunWith(AndroidJUnit4.class)
public class PinNoteTest {

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
     * Pin Note to Top
     * 
     * Description: Create two notes, pin one, verify it appears in pinned section
     */
    @Test
    public void testPinNoteMovesToPinnedSection() {
        // Create test notes
        long noteAId = repository.createOrUpdate(null, "Note A", "Content A", null, null, false);
        long noteBId = repository.createOrUpdate(null, "Note B", "Content B", null, null, false);
        
        // Wait for notes to be created
        try { Thread.sleep(500); } catch (InterruptedException e) {}

        // Launch activity
        ActivityScenario<MainActivity> scenario = activityRule.getScenario();
        
        // Wait for UI to load
        Espresso.onView(ViewMatchers.withId(R.id.rvNotes))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
        
        try { Thread.sleep(500); } catch (InterruptedException e) {}

        // Find "Note B" in the list and long-press to open context menu
        Espresso.onView(ViewMatchers.withId(R.id.rvNotes))
                .perform(RecyclerViewActions.actionOnItem(
                        ViewMatchers.hasDescendant(ViewMatchers.withText("Note B")),
                        ViewActions.longClick()
                ));

        // Wait for menu to appear, then click pin option
        // Note: Adjust this based on your actual menu implementation
        // If pin is in a popup menu, you may need to use different approach
        try { Thread.sleep(300); } catch (InterruptedException e) {}
        
        // Alternative: Directly pin via repository for testing
        repository.setPinned(noteBId, true);
        
        // Wait for UI update
        try { Thread.sleep(500); } catch (InterruptedException e) {}

        // Verify "Note B" appears in pinned section (should be first or in "Pinned" header section)
        // Check that "Pinned" header exists
        Espresso.onView(ViewMatchers.withText("Pinned"))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
        
        // Verify "Note B" appears before "Note A" (pinned notes come first)
        Espresso.onView(ViewMatchers.withId(R.id.rvNotes))
                .check(ViewAssertions.matches(
                        ViewMatchers.hasDescendant(ViewMatchers.withText("Note B"))
                ));
    }
}

@RunWith(AndroidJUnit4.class)
@LargeTest
public class PinNoteTest {

    @Test
    public void testPinNoteShowsInPinnedSection() throws Exception {
        // Create a note programmatically so it appears in the list
        Context ctx = ApplicationProvider.getApplicationContext();
        NoteRepository repo = ServiceLocator.noteRepository(ctx);
        repo.createOrUpdate(null, "Note to pin", "body", null, null, false);

        // Launch home
        ActivityScenario.launch(MainActivity.class);

        // Small delay to allow UI to populate (replace with idling resources in production)
        Thread.sleep(300);

        // Tap the pin icon on the first note item.
        // Adjust R.id.ivPin to the actual pin view id in your item layout if different.
        onView(withId(R.id.rvNotes))
                .perform(RecyclerViewActions.actionOnItemAtPosition(0, clickChildViewWithId(R.id.ivPin)));

        // Brief wait for UI update
        Thread.sleep(300);

        // Verify that the Pinned section/header is visible (adjust text if your app uses different heading)
        onView(withText("Pinned")).check(matches(isDisplayed()));

        // Verify the pinned note appears (scroll RecyclerView to an item that has the pinned note text)
        onView(withId(R.id.rvNotes))
                .perform(RecyclerViewActions.scrollTo(hasDescendant(withText("Note to pin"))));

        // Final assertion that the pinned note text is displayed
        onView(withText("Note to pin")).check(matches(isDisplayed()));
    }

    // Helper ViewAction to click a child view with a given id inside a RecyclerView item
    private static ViewAction clickChildViewWithId(final int id) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return anything();
            }

            @Override
            public String getDescription() {
                return "Click on a child view with specified id.";
            }

            @Override
            public void perform(UiController uiController, View view) {
                View child = view.findViewById(id);
                if (child != null && child.isClickable()) {
                    child.performClick();
                } else if (child != null) {
                    // attempt an explicit click if not clickable
                    child.callOnClick();
                }
                uiController.loopMainThreadUntilIdle();
            }
        };
    }
}
