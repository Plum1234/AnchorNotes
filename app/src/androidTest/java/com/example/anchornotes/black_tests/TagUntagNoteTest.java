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
import static androidx.test.espresso.matcher.RootMatchers.isPlatformPopup;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;

import static org.hamcrest.Matchers.allOf;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class TagUntagNoteTest {

    @Test
    public void testUntagNote() throws Exception {
        // Create a note programmatically so it appears in the list
        Context ctx = ApplicationProvider.getApplicationContext();
        NoteRepository repo = ServiceLocator.noteRepository(ctx);
        repo.createOrUpdate(null, "Note to untag", "body", null, null, false);

        // Launch home
        ActivityScenario.launch(MainActivity.class);

        // small delay to allow UI to populate (replace with idling resources if flaky)
        Thread.sleep(300);

        // Open the first note in the list (position 0) to go to the editor/preview
        onView(withId(R.id.rvNotes))
                .perform(RecyclerViewActions.actionOnItemAtPosition(0, click()));

        // Add the "Biology" tag so the test has a tagged note (reuse UI flow)
        onView(withText("Add Tag")).perform(click());
        onView(withText("Biology")).inRoot(isDialog()).perform(click());
        onView(withText("Save")).perform(click());

        // Wait for save/navigation
        Thread.sleep(300);

        // Re-open the same note to edit and remove the tag
        onView(withId(R.id.rvNotes))
                .perform(RecyclerViewActions.actionOnItemAtPosition(0, click()));

        Thread.sleep(200);

        // Click the 'X' (close) on the "Biology" chip.
        // Assumes the chip's close icon has contentDescription "Close" and is a descendant of the chip view containing the text.
        onView(allOf(
                withContentDescription("Close"),
                isDescendantOfA(hasDescendant(withText("Biology")))
        )).perform(click());

        // Save the note after removing the tag
        onView(withText("Save")).perform(click());

        // Wait for save/navigation
        Thread.sleep(300);

        // Verify that the tag "Biology" no longer appears in the home preview
        onView(withText("Biology")).check(doesNotExist());
    }
}