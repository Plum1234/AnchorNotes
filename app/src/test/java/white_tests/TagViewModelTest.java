package com.example.anchornotes;

import com.example.anchornotes.data.TagEntity;
import com.example.anchornotes.data.TagRepository;
import com.example.anchornotes.ui.tags.TagViewModel;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class TagViewModelTest {

    @Mock
    private TagRepository mockTagRepository;

    @Captor
    private ArgumentCaptor<TagEntity> tagCaptor;

    private TagViewModel viewModel;

    @Before
    public void setUp() {
        // Assumes TagViewModel has a constructor that accepts a TagRepository.
        // If your ViewModel obtains the repository differently, adapt this setup.
        viewModel = new TagViewModel(mockTagRepository);
    }

    @Test
    public void addTag_savesEntity() {
        // Act
        viewModel.addTag("Biology");

        // Assert repository received a TagEntity with name "Biology"
        verify(mockTagRepository).insert(tagCaptor.capture());
        TagEntity saved = tagCaptor.getValue();
        assertEquals("Biology", saved.getName());
    }
}