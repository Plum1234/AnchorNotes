package com.example.anchornotes.white_tests;

import android.content.Context;
import com.example.anchornotes.context.GeofenceManager;
import com.example.anchornotes.context.ReminderManager;
import com.example.anchornotes.data.db.AppDatabase;
import com.example.anchornotes.data.db.NoteDao;
import com.example.anchornotes.data.db.NoteEntity;
import com.example.anchornotes.data.repo.NoteRepository;
import com.example.anchornotes.model.PlaceSelection;
import com.example.anchornotes.worker.GeofenceRetryWorker;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * WB-GF-05 – Geofence Registration Failure Schedules Fallback Time Reminder
 * 
 * Location: app/src/test/java/com/example/anchornotes/service/
 * 
 * Description:
 * Mock GeofencingClient so that addGeofences(...) fails (e.g., onFailureListener called).
 * Mock NoteDao and WorkManager.
 * Call noteRepository.confirmReplaceWithGeofence(noteId, place, ...).
 * Simulate failure callback.
 * Assert that:
 * - noteDao.setPendingActivation(noteId, true) is called.
 * - WorkManager.enqueue(...) is called to schedule GeofenceRetryWorker.
 */
@RunWith(MockitoJUnitRunner.class)
@Config(sdk = 28)
public class GeofenceRegistrationFailureFallbackTest {

    @Mock
    private NoteDao mockNoteDao;

    @Mock
    private AppDatabase mockDatabase;

    @Mock
    private GeofencingClient mockGeofencingClient;

    @Mock
    private ReminderManager mockReminderManager;

    private Context context;
    private NoteRepository noteRepository;
    private NoteEntity testNote;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();

        // Create test note
        testNote = new NoteEntity();
        testNote.id = 123L;
        testNote.title = "Test Note";
        testNote.reminderType = null; // No existing reminder

        when(mockDatabase.noteDao()).thenReturn(mockNoteDao);
        when(mockNoteDao.getById(anyLong())).thenReturn(testNote);
    }

    @Test
    public void geofenceRegistrationFailure_schedulesFallbackReminder() throws Exception {
        long noteId = 123L;
        PlaceSelection place = new PlaceSelection(37.7749, -122.4194, 100.0f, "Test Location");

        // Mock GeofencingClient to return a failed task
        @SuppressWarnings("unchecked")
        Task<Void> failedTask = (Task<Void>) mock(Task.class);
        ApiException failureException = new ApiException(new Status(8, "GEOFENCE_NOT_AVAILABLE"));

        when(failedTask.isSuccessful()).thenReturn(false);
        when(failedTask.getException()).thenReturn(failureException);
        
        // Create a task that will call onFailure
        Task<Void> mockTask = Tasks.forException(failureException);
        
        // Setup GeofenceManager to fail
        try (MockedStatic<LocationServices> locationServicesMock = mockStatic(LocationServices.class);
             MockedStatic<AppDatabase> dbMock = mockStatic(AppDatabase.class);
             MockedStatic<androidx.work.WorkManager> workManagerMock = mockStatic(androidx.work.WorkManager.class)) {

            locationServicesMock.when(() -> LocationServices.getGeofencingClient(any(Context.class)))
                    .thenReturn(mockGeofencingClient);

            when(mockGeofencingClient.addGeofences(any(), any())).thenReturn(mockTask);

            dbMock.when(() -> AppDatabase.get(any(Context.class))).thenReturn(mockDatabase);

            androidx.work.WorkManager mockWorkManager = mock(androidx.work.WorkManager.class);
            workManagerMock.when(() -> androidx.work.WorkManager.getInstance(any(Context.class)))
                    .thenReturn(mockWorkManager);

            // Create repository with mocked dependencies
            NoteRepository repo = new NoteRepository(mockNoteDao, null, context);

            // Since GeofenceManager is created inside NoteRepository, we need to inject it
            // For this test, we'll verify the behavior through the repository's error handling

            final CountDownLatch latch = new CountDownLatch(1);
            final boolean[] errorCallbackInvoked = {false};

            Consumer<Throwable> onError = error -> {
                errorCallbackInvoked[0] = true;
                latch.countDown();
            };

            Runnable onScheduled = () -> fail("Should not succeed when geofence registration fails");

            // Call confirmReplaceWithGeofence which will attempt to register geofence
            repo.confirmReplaceWithGeofence(noteId, place, onScheduled, onError);

            // Wait for error callback
            boolean callbackInvoked = latch.await(2, TimeUnit.SECONDS);

            // Verify error callback was invoked
            assertTrue("Error callback should be invoked on failure", callbackInvoked);
            assertTrue("Error callback should be called", errorCallbackInvoked[0]);

            // Verify that setPendingActivation was called
            // Note: This happens in the error callback of addForNote
            verify(mockNoteDao, timeout(1000).atLeastOnce()).setPendingActivation(eq(noteId), eq(true));

            // Verify that WorkManager.enqueue was called to schedule retry worker
            verify(mockWorkManager, timeout(1000).atLeastOnce()).enqueue(any(androidx.work.WorkRequest.class));
        }
    }

    @Test
    public void geofenceRegistrationSuccess_doesNotScheduleFallback() throws Exception {
        long noteId = 456L;
        PlaceSelection place = new PlaceSelection(40.7128, -74.0060, 150.0f, "Success Location");

        // Mock successful geofence registration
        Task<Void> successTask = Tasks.forResult(null);

        try (MockedStatic<LocationServices> locationServicesMock = mockStatic(LocationServices.class);
             MockedStatic<AppDatabase> dbMock = mockStatic(AppDatabase.class);
             MockedStatic<androidx.work.WorkManager> workManagerMock = mockStatic(androidx.work.WorkManager.class)) {

            locationServicesMock.when(() -> LocationServices.getGeofencingClient(any(Context.class)))
                    .thenReturn(mockGeofencingClient);

            when(mockGeofencingClient.addGeofences(any(), any())).thenReturn(successTask);

            dbMock.when(() -> AppDatabase.get(any(Context.class))).thenReturn(mockDatabase);

            androidx.work.WorkManager mockWorkManager = mock(androidx.work.WorkManager.class);
            workManagerMock.when(() -> androidx.work.WorkManager.getInstance(any(Context.class)))
                    .thenReturn(mockWorkManager);

            NoteRepository repo = new NoteRepository(mockNoteDao, null, context);

            final CountDownLatch latch = new CountDownLatch(1);
            final boolean[] successCallbackInvoked = {false};

            Runnable onScheduled = () -> {
                successCallbackInvoked[0] = true;
                latch.countDown();
            };

            Consumer<Throwable> onError = error -> fail("Should not fail when geofence registration succeeds");

            repo.confirmReplaceWithGeofence(noteId, place, onScheduled, onError);

            boolean callbackInvoked = latch.await(2, TimeUnit.SECONDS);

            assertTrue("Success callback should be invoked", callbackInvoked);
            assertTrue("Success callback should be called", successCallbackInvoked[0]);

            // Verify setPendingActivation was NOT called
            verify(mockNoteDao, never()).setPendingActivation(eq(noteId), eq(true));

            // Verify WorkManager.enqueue was NOT called
            verify(mockWorkManager, never()).enqueue(any(androidx.work.WorkRequest.class));
        }
    }
}

