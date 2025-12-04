package com.example.anchornotes.white_tests;

import com.example.anchornotes.data.NoteEntity;
import com.example.anchornotes.data.NoteRepository;
import com.example.anchornotes.ui.context.ContextViewModel;
import com.example.anchornotes.location.LocationService;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ContextViewModelTest {

    @Mock
    private NoteRepository mockNoteRepository;

    @Mock
    private LocationService mockLocationService;

    @Captor
    private ArgumentCaptor<NoteEntity> noteCaptor;

    private ContextViewModel viewModel;

    @Before
    public void setUp() {
        // Assumes ContextViewModel has a constructor that accepts (NoteRepository, LocationService).
        // If your constructor order differs, adjust the instantiation accordingly.
        viewModel = new ContextViewModel(mockNoteRepository, mockLocationService);
    }

    @Test
    public void updateLocationOnEdit_overwritesLocationIfEnabled() {
        long noteId = 11L;

        // initial note with different location
        NoteEntity initial = new NoteEntity();
        initial.setId(noteId);
        initial.setTitle("Location edit test");
        initial.setBody("body");
        initial.setLocation("0.0,0.0");

        // L1 returned by location service
        String L1 = "12.34,56.78";

        when(mockNoteRepository.get(noteId)).thenReturn(initial);
        when(mockLocationService.getCurrentLocation()).thenReturn(L1);

        // Act: user confirms location update
        viewModel.updateLocation(noteId, true);

        // Assert repository saved the note with location updated to L1
        verify(mockNoteRepository).createOrUpdate(noteCaptor.capture());
        NoteEntity saved = noteCaptor.getValue();
        assertEquals(L1, saved.getLocation());
    }
}