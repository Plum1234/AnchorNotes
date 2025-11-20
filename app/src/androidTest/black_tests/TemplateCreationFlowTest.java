package black_tests;

import android.content.Context;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.example.anchornotes.data.db.TemplateEntity;
import com.example.anchornotes.data.repo.TemplateRepository;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class TemplateCreationFlowTest {
    
    private TemplateRepository repository;
    private Context context;
    
    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        repository = new TemplateRepository(context);
    }
    
    @After
    public void tearDown() {
        // Clean up test data
        List<TemplateEntity> allTemplates = repository.getAll();
        for (TemplateEntity template : allTemplates) {
            if (!template.isExample && template.name.startsWith("Test ")) {
                repository.delete(template);
            }
        }
    }
    
    @Test
    public void testCreateTemplateWithAllFields() {
        TemplateEntity template = new TemplateEntity(
            "Test Meeting Template",
            "#E3F2FD",
            "<h2>Meeting Notes</h2><p>Agenda items here</p>",
            "[1,2,3]",
            "office",
            false
        );
        
        long id = repository.create(template);
        assertTrue("Template should be created with valid ID", id > 0);
        
        TemplateEntity retrieved = repository.getById(id);
        assertNotNull("Template should be retrievable", retrieved);
        assertEquals("Name should match", "Test Meeting Template", retrieved.name);
        assertEquals("Color should match", "#E3F2FD", retrieved.pageColor);
        assertEquals("HTML should match", "<h2>Meeting Notes</h2><p>Agenda items here</p>", retrieved.prefilledHtml);
        assertEquals("Tag IDs should match", "[1,2,3]", retrieved.associatedTagIds);
        assertEquals("Geofence ID should match", "office", retrieved.associatedGeofenceId);
    }
    
    @Test
    public void testCreateTemplateAppearsInList() {
        TemplateEntity template = new TemplateEntity(
            "Test List Template",
            "#E8F5E8",
            "<p>Test content</p>",
            null,
            null,
            false
        );
        
        long id = repository.create(template);
        
        List<TemplateEntity> allTemplates = repository.getAll();
        boolean found = false;
        for (TemplateEntity t : allTemplates) {
            if (t.id == id && t.name.equals("Test List Template")) {
                found = true;
                break;
            }
        }
        
        assertTrue("Created template should appear in list", found);
    }
    
    @Test
    public void testCreateMultipleTemplates() {
        for (int i = 0; i < 5; i++) {
            TemplateEntity template = new TemplateEntity(
                "Test Template " + i,
                "#E3F2FD",
                "<p>Content " + i + "</p>",
                null,
                null,
                false
            );
            repository.create(template);
        }
        
        List<TemplateEntity> allTemplates = repository.getAll();
        int testTemplateCount = 0;
        for (TemplateEntity t : allTemplates) {
            if (t.name.startsWith("Test Template ")) {
                testTemplateCount++;
            }
        }
        
        assertEquals("Should have created 5 templates", 5, testTemplateCount);
    }
}

