package com.example.anchornotes.white_tests;

import com.example.anchornotes.data.NoteEntity;
import com.example.anchornotes.data.NoteRepository;
import com.example.anchornotes.location.LocationService;
import com.example.anchornotes.ui.context.ContextViewModel;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ContextViewModelRemoveLocationTest {

    @Mock
    private NoteRepository mockNoteRepository;

    @Mock
    private LocationService mockLocationService;

    @Captor
    private ArgumentCaptor<NoteEntity> noteCaptor;

    private ContextViewModel viewModel;

    @Before
    public void setUp() {
        // Match constructor used in the codebase (NoteRepository, LocationService)
        viewModel = new ContextViewModel(mockNoteRepository, mockLocationService);
    }

    @Test
    public void removeLocation_nullifiesField() {
        long noteId = 21L;

        // Given a note with an existing location
        NoteEntity initial = new NoteEntity();
        initial.setId(noteId);
        initial.setTitle("Remove loc test");
        initial.setBody("body");
        initial.setLocation("12.34,56.78");

        when(mockNoteRepository.get(noteId)).thenReturn(initial);

        // Act: remove location
        viewModel.removeLocation(noteId);

        // Assert repository was asked to persist the note with location set to null
        verify(mockNoteRepository).createOrUpdate(noteCaptor.capture());
        NoteEntity saved = noteCaptor.getValue();
        assertNull("Expected location to be null after removal", saved.getLocation());
    }
}
```// filepath: /Users/sabornikundu/Desktop/AnchorNotes/app/src/test/java/com/example/anchornotes/white_tests/ContextViewModelRemoveLocationTest.java