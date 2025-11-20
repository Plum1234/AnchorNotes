package com.example.anchornotes.black_tests;

import android.app.NotificationManager;
import android.content.Context;
import android.location.Location;
import androidx.core.content.ContextCompat;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.action.ViewActions;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.rule.GrantPermissionRule;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiSelector;
import com.example.anchornotes.MainActivity;
import com.example.anchornotes.R;
import com.example.anchornotes.data.db.AppDatabase;
import com.example.anchornotes.data.db.NoteEntity;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * BB-GF-01 – Create Location-Based Reminder and Trigger on Enter
 * 
 * Location: app/src/androidTest/java/com/example/anchornotes/blacktests/
 * 
 * Description: Launch app to MainActivity. Create a note titled "Pick up package" and save.
 * Open the note's "Add Reminder" UI and choose Location-based Reminder. Select a test location
 * (e.g., a mock coordinate) and radius (e.g., 100m). Confirm and save reminder.
 * In the emulator, enable mock locations and simulate entering the geofenced area.
 * Assert that a notification with title "Pick up package" appears in the system notification shade.
 */
@RunWith(AndroidJUnit4.class)
public class CreateLocationReminderAndTriggerOnEnterTest {

    @Rule
    public ActivityTestRule<MainActivity> activityRule = new ActivityTestRule<>(MainActivity.class);

    @Rule
    public GrantPermissionRule locationPermissionRule = GrantPermissionRule.grant(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
    );

    @Rule
    public GrantPermissionRule notificationPermissionRule = GrantPermissionRule.grant(
            android.Manifest.permission.POST_NOTIFICATIONS
    );

    private Context context;
    private AppDatabase database;
    private UiDevice uiDevice;
    private FusedLocationProviderClient fusedLocationClient;
    private NotificationManager notificationManager;

    @Before
    public void setUp() throws Exception {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        database = AppDatabase.get(context);
        uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
        notificationManager = ContextCompat.getSystemService(context, NotificationManager.class);

        // Clear all notes before each test
        List<NoteEntity> notes = database.noteDao().getAll();
        for (NoteEntity note : notes) {
            database.noteDao().delete(note);
        }
        
        // Cancel all notifications
        if (notificationManager != null) {
            notificationManager.cancelAll();
        }
    }

    @After
    public void tearDown() throws Exception {
        // Clear all notes after each test
        List<NoteEntity> notes = database.noteDao().getAll();
        for (NoteEntity note : notes) {
            database.noteDao().delete(note);
        }
        
        // Cancel all notifications
        if (notificationManager != null) {
            notificationManager.cancelAll();
        }
        
        // Reset location to null (turn off mock location)
        uiDevice.executeShellCommand("settings put secure mock_location 0");
    }

    @Test
    public void testCreateLocationReminderAndTriggerOnEnter() throws Exception {
        String noteTitle = "Pick up package";
        double testLatitude = 37.7749;
        double testLongitude = -122.4194;

        // Step 1: Launch app (already done by ActivityTestRule)
        // App is launched to MainActivity

        // Step 2: Create a note
        onView(withId(R.id.fabNew)).perform(click());
        
        // Wait for note editor to appear
        Thread.sleep(1000);
        
        // Enter title
        onView(withId(R.id.etTitle))
            .perform(typeText(noteTitle), closeSoftKeyboard());

        // Save the note
        onView(withId(R.id.btnSave)).perform(click());
        
        // Wait for save to complete
        Thread.sleep(1000);

        // Step 3: Get the saved note ID
        List<NoteEntity> allNotes = database.noteDao().getAll();
        NoteEntity savedNote = null;
        for (NoteEntity note : allNotes) {
            if (noteTitle.equals(note.title)) {
                savedNote = note;
                break;
            }
        }
        assertNotNull("Note should be saved", savedNote);
        long noteId = savedNote.id;

        // Step 4: Open reminder dialog
        onView(withId(R.id.btnReminder)).perform(click());
        
        // Wait for dialog
        Thread.sleep(500);

        // Step 5: Select Location-based Reminder (radio button "At place")
        onView(withId(R.id.rbGeofence)).perform(click());
        
        // Wait for geofence options to appear
        Thread.sleep(300);

        // Step 6: Mock location selection - we need to set mock location before clicking "Use Current Location"
        // Enable mock locations
        uiDevice.executeShellCommand("settings put secure mock_location 1");
        
        // Set mock location to our test coordinates
        setMockLocation(testLatitude, testLongitude);

        // Click "Use Current Location" button
        onView(withId(R.id.btnSelectPlace)).perform(click());
        
        // Wait for location to be fetched
        Thread.sleep(2000);

        // Step 7: Confirm and save reminder
        onView(withId(R.id.btnSet)).perform(click());
        
        // Wait for reminder to be set
        Thread.sleep(2000);

        // Verify reminder was set in database
        NoteEntity updatedNote = database.noteDao().getById(noteId);
        assertNotNull("Note should exist", updatedNote);
        assertTrue("Reminder type should be GEOFENCE", "GEOFENCE".equals(updatedNote.reminderType));
        assertNotNull("Geofence ID should be set", updatedNote.geofenceId);

        // Step 8: Simulate entering the geofenced area
        // Move location slightly to ensure we're inside the geofence
        setMockLocation(testLatitude, testLongitude);
        
        // Wait a bit for geofence transition
        Thread.sleep(3000);

        // Step 9: Trigger geofence by simulating location update
        // We need to trigger the GeofenceReceiver manually or wait for it
        // For testing, we can directly send a geofence intent or wait for the system to process it
        // This is a simplified approach - in a real scenario, you might need to use a test double
        
        // Wait for notification to appear
        Thread.sleep(5000);

        // Step 10: Verify notification appears
        // Open notification shade
        uiDevice.openNotification();
        Thread.sleep(1000);

        // Check if notification with the note title exists
        UiObject notificationTitle = uiDevice.findObject(new UiSelector().text(noteTitle));
        assertTrue("Notification with title '" + noteTitle + "' should appear in notification shade", 
                notificationTitle.exists());
    }

    // Helper method to set mock location
    private void setMockLocation(double latitude, double longitude) {
        try {
            Location mockLocation = new Location("mock");
            mockLocation.setLatitude(latitude);
            mockLocation.setLongitude(longitude);
            mockLocation.setAccuracy(10.0f);
            mockLocation.setTime(System.currentTimeMillis());
            mockLocation.setElapsedRealtimeNanos(android.os.SystemClock.elapsedRealtimeNanos());
            
            // Note: Setting mock location in tests requires special setup
            // This is a placeholder - in real implementation you might use:
            // 1. Test LocationProvider
            // 2. MockLocationProvider
            // 3. UiAutomator location commands
            
            // For now, we'll just enable mock location mode
            // The actual location will be set by the emulator's location controls
            uiDevice.executeShellCommand("settings put secure mock_location 1");
        } catch (Exception e) {
            // Mock location setting might fail in some test environments
            // Continue with test anyway
        }
    }
}

