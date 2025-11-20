package white_tests;

import android.content.Context;
import com.example.anchornotes.data.db.TemplateEntity;
import com.example.anchornotes.data.repo.TemplateRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
public class TemplateRepositoryTest {
    
    private TemplateRepository repository;
    private Context context;
    
    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
        repository = new TemplateRepository(context);
    }
    
    @Test
    public void testCreateTemplate() {
        TemplateEntity template = new TemplateEntity(
            "Test Template",
            "#E3F2FD",
            "<p>Test content</p>",
            null,
            null,
            false
        );
        
        long id = repository.create(template);
        assertTrue("Template should be created with valid ID", id > 0);
        
        TemplateEntity retrieved = repository.getById(id);
        assertNotNull("Template should be retrievable", retrieved);
        assertEquals("Template name should match", "Test Template", retrieved.name);
        assertEquals("Template color should match", "#E3F2FD", retrieved.pageColor);
    }
    
    @Test
    public void testUpdateTemplate() {
        TemplateEntity template = new TemplateEntity(
            "Original Name",
            "#E3F2FD",
            "<p>Original</p>",
            null,
            null,
            false
        );
        
        long id = repository.create(template);
        template.id = id;
        template.name = "Updated Name";
        template.prefilledHtml = "<p>Updated</p>";
        
        repository.update(template);
        
        TemplateEntity updated = repository.getById(id);
        assertEquals("Name should be updated", "Updated Name", updated.name);
        assertEquals("Content should be updated", "<p>Updated</p>", updated.prefilledHtml);
    }
    
    @Test
    public void testDeleteTemplate() {
        TemplateEntity template = new TemplateEntity(
            "To Delete",
            "#E3F2FD",
            "<p>Delete me</p>",
            null,
            null,
            false
        );
        
        long id = repository.create(template);
        template.id = id;
        
        repository.delete(template);
        
        TemplateEntity deleted = repository.getById(id);
        assertNull("Template should be deleted", deleted);
    }
    
    @Test
    public void testSerializeAndParseTagIds() {
        List<Long> tagIds = new ArrayList<>();
        tagIds.add(1L);
        tagIds.add(2L);
        tagIds.add(3L);
        
        String serialized = repository.serializeTagIds(tagIds);
        assertNotNull("Serialized tag IDs should not be null", serialized);
        assertTrue("Serialized should be JSON array", serialized.startsWith("["));
        
        List<Long> parsed = repository.parseAssociatedTagIds(serialized);
        assertEquals("Parsed tag IDs should match", 3, parsed.size());
        assertEquals("First tag ID should match", Long.valueOf(1L), parsed.get(0));
        assertEquals("Second tag ID should match", Long.valueOf(2L), parsed.get(1));
        assertEquals("Third tag ID should match", Long.valueOf(3L), parsed.get(2));
    }
    
    @Test
    public void testGetTemplatesForSelectionWithActiveGeofence() {
        // Create templates with and without geofence associations
        TemplateEntity geofenceTemplate = new TemplateEntity(
            "Office Template",
            "#E3F2FD",
            "<p>Office</p>",
            null,
            "office",
            false
        );
        repository.create(geofenceTemplate);
        
        TemplateEntity normalTemplate = new TemplateEntity(
            "Normal Template",
            "#E8F5E8",
            "<p>Normal</p>",
            null,
            null,
            false
        );
        repository.create(normalTemplate);
        
        // Get templates for selection
        List<TemplateEntity> templates = repository.getTemplatesForSelection();
        
        assertNotNull("Templates list should not be null", templates);
        assertTrue("Should return templates", templates.size() >= 2);
    }
    
    @Test
    public void testIsNameUnique() {
        TemplateEntity template1 = new TemplateEntity(
            "Unique Name",
            "#E3F2FD",
            "<p>Content</p>",
            null,
            null,
            false
        );
        long id1 = repository.create(template1);
        
        assertTrue("New name should be unique", repository.isNameUnique("Different Name", 0));
        assertFalse("Existing name should not be unique", repository.isNameUnique("Unique Name", 0));
        assertTrue("Same name for same template should be unique", repository.isNameUnique("Unique Name", id1));
    }
}

