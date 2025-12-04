package com.example.anchornotes.white_tests;

import android.content.Context;
import com.example.anchornotes.context.GeofenceManager;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.Task;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

/**
 * WB-GF-04 – GeofenceManager.cancelLocationReminder Unregisters Geofence
 * 
 * Location: app/src/test/java/com/example/anchornotes/service/
 * 
 * Description:
 * Mock GeofencingClient.
 * Call geofenceManager.removeForNote(geofenceId).
 * Verify geofencingClient.removeGeofences(...) is called with a list containing exactly that note's geofence request id.
 */
@RunWith(MockitoJUnitRunner.class)
public class GeofenceManagerCancelTest {

    @Mock
    private Context mockContext;

    @Mock
    private GeofencingClient mockGeofencingClient;

    @Mock
    private Task<Void> mockRemoveTask;

    private GeofenceManager geofenceManager;
    private ArgumentCaptor<List<String>> geofenceIdsCaptor;

    @Before
    public void setUp() {
        geofenceIdsCaptor = ArgumentCaptor.forClass(List.class);
        
        // Setup mock for removeGeofences
        when(mockRemoveTask.addOnSuccessListener(any())).thenReturn(mockRemoveTask);
        when(mockRemoveTask.addOnFailureListener(any())).thenReturn(mockRemoveTask);
        when(mockGeofencingClient.removeGeofences(anyList())).thenReturn(mockRemoveTask);

        // Create GeofenceManager with mocked GeofencingClient
        try (MockedStatic<LocationServices> locationServicesMock = mockStatic(LocationServices.class)) {
            locationServicesMock.when(() -> LocationServices.getGeofencingClient(any(Context.class)))
                    .thenReturn(mockGeofencingClient);

            geofenceManager = new GeofenceManager(mockContext);
        }
    }

    @Test
    public void cancelLocationReminder_callsRemoveGeofencesWithCorrectId() {
        String geofenceId = "note-123";

        // Call removeForNote
        geofenceManager.removeForNote(geofenceId);

        // Verify removeGeofences was called
        verify(mockGeofencingClient, times(1)).removeGeofences(anyList());

        // Capture the argument
        verify(mockGeofencingClient).removeGeofences(geofenceIdsCaptor.capture());

        // Verify the list contains exactly the geofence ID
        List<String> capturedIds = geofenceIdsCaptor.getValue();
        assertNotNull("Geofence IDs list should not be null", capturedIds);
        assertEquals("List should contain exactly one ID", 1, capturedIds.size());
        assertEquals("Geofence ID should match", geofenceId, capturedIds.get(0));
    }

    @Test
    public void cancelLocationReminder_withDifferentNoteId_usesCorrectId() {
        long noteId = 456L;
        String geofenceId = "note-456";

        geofenceManager.removeForNote(geofenceId);

        verify(mockGeofencingClient).removeGeofences(geofenceIdsCaptor.capture());

        List<String> capturedIds = geofenceIdsCaptor.getValue();
        assertEquals("List should contain exactly one ID", 1, capturedIds.size());
        assertEquals("Geofence ID should match note ID format", geofenceId, capturedIds.get(0));
        assertTrue("Geofence ID should start with 'note-'", capturedIds.get(0).startsWith("note-"));
    }

    @Test
    public void cancelLocationReminder_calledMultipleTimes_removesEachGeofence() {
        String geofenceId1 = "note-100";
        String geofenceId2 = "note-200";
        String geofenceId3 = "note-300";

        geofenceManager.removeForNote(geofenceId1);
        geofenceManager.removeForNote(geofenceId2);
        geofenceManager.removeForNote(geofenceId3);

        // Verify removeGeofences was called 3 times
        verify(mockGeofencingClient, times(3)).removeGeofences(anyList());

        // Each call should have exactly one ID
        verify(mockGeofencingClient, times(1)).removeGeofences(argThat(list -> 
                list.size() == 1 && list.contains(geofenceId1)));
        verify(mockGeofencingClient, times(1)).removeGeofences(argThat(list -> 
                list.size() == 1 && list.contains(geofenceId2)));
        verify(mockGeofencingClient, times(1)).removeGeofences(argThat(list -> 
                list.size() == 1 && list.contains(geofenceId3)));
    }
}

