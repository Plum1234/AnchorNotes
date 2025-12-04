package com.example.anchornotes.white_tests;

import com.example.anchornotes.data.NoteEntity;
import com.example.anchornotes.data.NoteRepository;
import com.example.anchornotes.data.TagRepository;
import com.example.anchornotes.ui.tags.TagViewModel;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TagViewModelNoteTagTest {

    @Mock
    private NoteRepository mockNoteRepository;

    @Mock
    private TagRepository mockTagRepository;

    @Captor
    private ArgumentCaptor<NoteEntity> noteCaptor;

    private TagViewModel viewModel;

    @Before
    public void setUp() {
        // Assumes TagViewModel has a constructor that accepts TagRepository and NoteRepository.
        // If your TagViewModel constructor differs, adjust this line accordingly.
        viewModel = new TagViewModel(mockTagRepository, mockNoteRepository);
    }

    @Test
    public void addTagToNote_updatesTagList() {
        long noteId = 42L;

        // Prepare an initial NoteEntity with an empty tag list.
        // Adjust constructor / setters to match your NoteEntity API if different.
        NoteEntity initial = new NoteEntity();
        initial.setId(noteId);
        initial.setTitle("Test note");
        initial.setBody("body");
        initial.setTags(new ArrayList<>()); // empty list

        // Stub repository to return the initial note when requested.
        // Adjust method name if your repository uses getNoteById / find / load etc.
        when(mockNoteRepository.get(noteId)).thenReturn(initial);

        // Act: add "Biology" to the note via the ViewModel.
        viewModel.addTagToNote(noteId, "Biology");

        // Assert: repository was asked to persist an updated NoteEntity whose tag list contains "Biology".
        verify(mockNoteRepository).createOrUpdate(noteCaptor.capture());
        NoteEntity saved = noteCaptor.getValue();

        List<String> tags = saved.getTags();
        // Ensure a defensive copy was made and tag was added.
        assertEquals(1, tags.size());
        assertEquals("Biology", tags.get(0));
    }
}