package com.example.anchornotes.white_tests;

import com.example.anchornotes.data.NoteEntity;
import com.example.anchornotes.data.NoteRepository;
import com.example.anchornotes.ui.PinViewModel;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PinViewModelTest {

    @Mock
    private NoteRepository mockNoteRepository;

    @Captor
    private ArgumentCaptor<NoteEntity> noteCaptor;

    private PinViewModel viewModel;

    @Before
    public void setUp() {
        // Assumes PinViewModel has a constructor that accepts a NoteRepository.
        viewModel = new PinViewModel(mockNoteRepository);
    }

    @Test
    public void pinNote_setsFlag() {
        long noteId = 7L;

        // Prepare an initial NoteEntity with isPinned = false.
        NoteEntity initial = new NoteEntity();
        initial.setId(noteId);
        initial.setTitle("Pin test");
        initial.setBody("body");
        initial.setPinned(false);

        // Stub repository to return the initial note when requested.
        when(mockNoteRepository.get(noteId)).thenReturn(initial);

        // Act: pin the note via the ViewModel.
        viewModel.pin(noteId);

        // Assert repository was asked to persist an updated NoteEntity with isPinned = true.
        verify(mockNoteRepository).createOrUpdate(noteCaptor.capture());
        NoteEntity saved = noteCaptor.getValue();
        assertTrue("Expected note to be pinned", saved.isPinned());
    }
}