package com.example.anchornotes.black_tests;

import android.content.Context;
import android.view.View;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.example.anchornotes.data.NoteRepository;
import com.example.anchornotes.data.ServiceLocator;
import com.example.anchornotes.ui.MainActivity;

import org.hamcrest.Matcher;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static org.hamcrest.Matchers.anything;

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