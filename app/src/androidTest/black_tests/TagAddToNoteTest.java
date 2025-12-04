package com.example.anchornotes.black_tests;

import android.content.Context;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.example.anchornotes.data.NoteRepository;
import com.example.anchornotes.data.ServiceLocator;
import com.example.anchornotes.ui.MainActivity;

import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class TagAddToNoteTest {

    @Test
    public void testAddTagToNote() throws Exception {
        // Create a note programmatically so it appears in the list
        Context ctx = ApplicationProvider.getApplicationContext();
        NoteRepository repo = ServiceLocator.noteRepository(ctx);
        repo.createOrUpdate(null, "Note for tag", "body", null, null, false);

        // Launch home
        ActivityScenario.launch(MainActivity.class);

        // small delay to allow UI to populate (replace with idling resources if flaky)
        Thread.sleep(300);

        // Open the first note in the list (position 0) to go to the editor/preview
        onView(withId(R.id.rvNotes))
                .perform(RecyclerViewActions.actionOnItemAtPosition(0, click()));

        // Tap "Add Tag" in the note editor
        onView(withText("Add Tag")).perform(click());

        // Select the "Biology" tag from the dialog/list
        onView(withText("Biology")).inRoot(isDialog()).perform(click());

        // Close keyboard if shown and save the note (adjust if your app uses a different save control)
        onView(withText("Save")).perform(click());

        // Return to notes list if needed (some apps auto-navigate back on save). Wait briefly.
        Thread.sleep(300);

        // Verify the tag chip "Biology" appears on the note preview in the RecyclerView
        onView(withId(R.id.rvNotes))
                .perform(RecyclerViewActions.scrollTo(hasDescendant(withText("Biology"))));

        onView(withText("Biology")).check(matches(isDisplayed()));
    }
}