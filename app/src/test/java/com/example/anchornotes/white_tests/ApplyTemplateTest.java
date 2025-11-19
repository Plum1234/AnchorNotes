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
 * TemplateViewModel.applyTemplate CopiesTemplateBody Test
 * 
 * Note: Testing at repository/DAO level since ViewModel uses ServiceLocator
 */
@RunWith(JUnit4.class)
public class ApplyTemplateTest {

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
     * TemplateViewModel.applyTemplate CopiesTemplateBody
     * 
     * Description: Test that getTemplateById returns template with correct fields
     * that can be used to populate a new note
     */
    @Test
    public void applyTemplate_populatesNewNoteFields() {
        // Create a template
        TemplateEntity template = new TemplateEntity();
        template.name = "Daily Standup";
        template.prefilledHtml = "<h2>Yesterday</h2><h2>Today</h2><h2>Blockers</h2>";
        template.pageColor = "#FFFFFF";
        template.isExample = false;
        
        long templateId = templateRepository.create(template);
        assertTrue("Template ID should be greater than 0", templateId > 0);
        
        // Get template (simulating what ViewModel.getTemplateById does)
        TemplateEntity result = templateRepository.getById(templateId);
        
        // Verify template fields are correct
        assertNotNull("Template should not be null", result);
        assertEquals("Template name should match", "Daily Standup", result.name);
        assertTrue("Template body should contain template structure", 
                result.prefilledHtml.contains("Yesterday"));
        assertTrue("Template body should contain template structure", 
                result.prefilledHtml.contains("Today"));
        assertTrue("Template body should contain template structure", 
                result.prefilledHtml.contains("Blockers"));
        
        // Simulate applying template to create a note
        long noteId = noteRepository.createOrUpdate(null, "New Note", result.prefilledHtml, null, null, false);
        NoteEntity createdNote = noteRepository.get(noteId);
        
        // Verify note contains template content
        assertNotNull("Note should be created", createdNote);
        assertTrue("Note body should contain template structure", 
                createdNote.bodyHtml.contains("Yesterday"));
    }
}

