package com.example.anchornotes.black_tests;

import android.Manifest;
import android.content.Context;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.rule.GrantPermissionRule;

import com.example.anchornotes.data.NoteRepository;
import com.example.anchornotes.data.ServiceLocator;
import com.example.anchornotes.ui.MainActivity;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static org.hamcrest.Matchers.containsString;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class ContextTests {

    // Grant location permissions so the app can obtain a mock/location at test time.
    @Rule
    public GrantPermissionRule permissionRule = GrantPermissionRule.grant(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    );

    @Test
    public void testAttachLocationOnCreate() throws Exception {
        // Launch home
        ActivityScenario.launch(MainActivity.class);

        // Open new note editor (adjust id if your app uses a different FAB id)
        onView(withId(R.id.fabAddNote)).perform(click());

        // Enter title/body (adjust view ids if different)
        onView(withId(R.id.etNoteTitle)).perform(replaceText("Location note"), closeSoftKeyboard());
        onView(withId(R.id.etNoteBody)).perform(replaceText("body"), closeSoftKeyboard());

        // Save the note (adjust if save control differs)
        onView(withText("Save")).perform(click());

        // Small wait for save/navigation (replace with idling resource in production)
        Thread.sleep(400);

        // Open the newly created note (assumes it appears at position 0)
        onView(withId(R.id.rvNotes))
                .perform(RecyclerViewActions.actionOnItemAtPosition(0, click()));

        // Tap "View Location"
        onView(withText("View Location")).perform(click());

        // Assert that a latitude/longitude-like string is shown in the displayed dialog.
        // We check for a decimal point as a minimal assertion for a numeric coordinate.
        onView(withText(containsString("."))).inRoot(isDialog()).check(matches(isDisplayed()));
    }
}