package black_tests;

import android.content.Context;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.example.anchornotes.data.db.NoteEntity;
import com.example.anchornotes.data.db.TemplateEntity;
import com.example.anchornotes.data.repo.NoteRepository;
import com.example.anchornotes.data.repo.TemplateRepository;
import com.example.anchornotes.data.ServiceLocator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class TemplateSelectionAndApplicationTest {
    
    private TemplateRepository templateRepository;
    private NoteRepository noteRepository;
    private Context context;
    
    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        templateRepository = new TemplateRepository(context);
        noteRepository = ServiceLocator.noteRepository(context);
    }
    
    @After
    public void tearDown() {
        // Clean up test data
        // Note: In a real scenario, you'd clean up created notes and templates
    }
    
    @Test
    public void testSelectTemplateAndCreateNote() {
        // Create a test template
        TemplateEntity template = new TemplateEntity(
            "Test Selection Template",
            "#E3F2FD",
            "<h2>Test Title</h2><p>Test body content</p>",
            null,
            null,
            false
        );
        long templateId = templateRepository.create(template);
        
        // Verify template exists
        TemplateEntity retrievedTemplate = templateRepository.getById(templateId);
        assertNotNull("Template should exist", retrievedTemplate);
        
        // Simulate template application: create note with template content
        String title = "Note from Template";
        String bodyHtml = retrievedTemplate.prefilledHtml;
        
        long noteId = noteRepository.createOrUpdate(
            null,
            title,
            bodyHtml,
            null,
            null,
            false
        );
        
        // Verify note was created with template content
        NoteEntity note = noteRepository.get(noteId);
        assertNotNull("Note should be created", note);
        assertEquals("Note title should match", title, note.title);
        assertTrue("Note body should contain template content", 
            note.bodyHtml.contains("Test Title") || note.bodyHtml.contains("Test body"));
    }
    
    @Test
    public void testTemplateContentIsAppliedCorrectly() {
        TemplateEntity template = new TemplateEntity(
            "Content Test Template",
            "#E8F5E8",
            "<h2>Meeting: Project Review</h2><p><strong>Date:</strong> Today</p>",
            null,
            null,
            false
        );
        long templateId = templateRepository.create(template);
        
        TemplateEntity retrieved = templateRepository.getById(templateId);
        
        // Verify template has expected content
        assertNotNull("Template should exist", retrieved);
        assertTrue("Template should contain meeting structure", 
            retrieved.prefilledHtml.contains("Meeting"));
        assertTrue("Template should contain date field", 
            retrieved.prefilledHtml.contains("Date"));
    }
    
    @Test
    public void testGetTemplatesForSelectionReturnsTemplates() {
        // Create multiple templates
        for (int i = 0; i < 3; i++) {
            TemplateEntity template = new TemplateEntity(
                "Selection Test " + i,
                "#E3F2FD",
                "<p>Content " + i + "</p>",
                null,
                null,
                false
            );
            templateRepository.create(template);
        }
        
        // Get templates for selection
        java.util.List<TemplateEntity> templates = templateRepository.getTemplatesForSelection();
        
        assertNotNull("Templates list should not be null", templates);
        assertTrue("Should return templates", templates.size() >= 3);
    }
}

