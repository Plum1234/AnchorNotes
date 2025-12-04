package com.example.anchornotes.white_tests;

import android.content.Context;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;

import com.example.anchornotes.data.db.AppDatabase;
import com.example.anchornotes.data.db.NoteEntity;
import com.example.anchornotes.data.db.TemplateDao;
import com.example.anchornotes.data.db.TemplateEntity;
import com.example.anchornotes.data.repo.NoteRepository;
import com.example.anchornotes.data.repo.TemplateRepository;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.*;

/**
 * TemplateViewModel.deleteTemplate DoesNotAffectExistingNotes Test
 * 
 * Note: Testing at repository/DAO level since ViewModel uses ServiceLocator
 */
@RunWith(JUnit4.class)
public class DeleteTemplateTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private Context context;
    private AppDatabase database;
    private TemplateDao templateDao;
    private TemplateRepository templateRepository;
    private NoteRepository noteRepository;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        // Use in-memory database for testing
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .allowMainThreadQueries()
                .build();
        templateDao = database.templateDao();
        // Note: TemplateRepository uses AppDatabase.get() singleton
        // We'll test using the actual repository which will use the singleton
        // In a production test, you'd use dependency injection
        templateRepository = new TemplateRepository(context);
        noteRepository = new NoteRepository(database.noteDao());
    }

    @After
    public void tearDown() {
        if (database != null) {
            database.close();
        }
    }

    /**
     * TemplateViewModel.deleteTemplate DoesNotAffectExistingNotes
     * 
     * Description: Test that deleting a template doesn't affect existing notes created from that template
     */
    @Test
    public void deleteTemplate_keepsExistingNotesUntouched() {
        // Create a template
        TemplateEntity template = new TemplateEntity();
        template.name = "Test Template";
        template.prefilledHtml = "<p>Template content</p>";
        template.isExample = false;
        
        long templateId = templateRepository.create(template);
        
        // Create a note from this template (copy the content)
        String noteBodyFromTemplate = template.prefilledHtml;
        long noteId = noteRepository.createOrUpdate(null, "Note from Template", noteBodyFromTemplate, null, null, false);
        
        // Verify note exists with template content
        NoteEntity noteBefore = noteRepository.get(noteId);
        assertNotNull("Note should exist", noteBefore);
        assertEquals("Note should have template content", "<p>Template content</p>", noteBefore.bodyHtml);
        
        // Delete the template
        TemplateEntity templateToDelete = templateRepository.getById(templateId);
        templateRepository.delete(templateToDelete);
        
        // Verify template is deleted
        TemplateEntity deletedTemplate = templateRepository.getById(templateId);
        assertNull("Template should be deleted", deletedTemplate);
        
        // Verify the note still exists with unchanged content
        NoteEntity noteAfter = noteRepository.get(noteId);
        assertNotNull("Note should still exist after template deletion", noteAfter);
        assertEquals("Note content should remain unchanged", 
                "<p>Template content</p>", noteAfter.bodyHtml);
    }
}

