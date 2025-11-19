package com.example.anchornotes;

import android.content.Context;

import androidx.recyclerview.widget.RecyclerView;
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
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.longClick;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;
import static androidx.test.espresso.matcher.RootMatchers.isPlatformPopup;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withHint;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class TagTests {

    @Test
    public void testCreateTagFromHome() throws Exception {
        // ensure there's at least one note to long-press
        Context ctx = ApplicationProvider.getApplicationContext();
        NoteRepository repo = ServiceLocator.noteRepository(ctx);
        repo.createOrUpdate(null, "Test note for tag", "body", null, null, false);

        // Launch the main activity (home)
        ActivityScenario.launch(MainActivity.class);

        // brief pause for UI population (replace with idling in production)
        Thread.sleep(300);

        // Long-press the first note in the list to open the popup menu
        onView(withId(R.id.rvNotes))
                .perform(RecyclerViewActions.actionOnItemAtPosition(0, longClick()));

        // Click the popup menu item "Add Tag"
        onView(withText("Add Tag")).inRoot(isPlatformPopup()).perform(click());

        // Type the tag name into the dialog's EditText (hint = "e.g., Biology")
        onView(withHint("e.g., Biology")).perform(replaceText("Biology"), closeSoftKeyboard());

        // Confirm by clicking the Add button
        onView(withText("Add")).perform(click());

        // Open the Filter dialog (toolbar menu item 'Filter') to view available tags
        onView(withId(R.id.action_filter)).perform(click());

        // Verify that the tag "Biology" is listed in the dialog
        onView(withText("Biology")).inRoot(isDialog()).check(matches(isDisplayed()));
    }
}