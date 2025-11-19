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
public class TemplateEditingBehaviorTest {
    
    private TemplateRepository repository;
    private Context context;
    
    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        repository = new TemplateRepository(context);
    }
    
    @After
    public void tearDown() {
        // Clean up test templates
        List<TemplateEntity> allTemplates = repository.getAll();
        for (TemplateEntity template : allTemplates) {
            if (!template.isExample && template.name.startsWith("Edit Test")) {
                repository.delete(template);
            }
        }
    }
    
    @Test
    public void testUpdateTemplateName() {
        TemplateEntity template = new TemplateEntity(
            "Edit Test Original",
            "#E3F2FD",
            "<p>Original content</p>",
            null,
            null,
            false
        );
        long id = repository.create(template);
        template.id = id;
        
        template.name = "Edit Test Updated";
        repository.update(template);
        
        TemplateEntity updated = repository.getById(id);
        assertEquals("Name should be updated", "Edit Test Updated", updated.name);
        assertEquals("Content should remain unchanged", "<p>Original content</p>", updated.prefilledHtml);
    }
    
    @Test
    public void testUpdateTemplateContent() {
        TemplateEntity template = new TemplateEntity(
            "Edit Test Content",
            "#E3F2FD",
            "<p>Original</p>",
            null,
            null,
            false
        );
        long id = repository.create(template);
        template.id = id;
        
        template.prefilledHtml = "<h2>Updated Content</h2><p>New body</p>";
        repository.update(template);
        
        TemplateEntity updated = repository.getById(id);
        assertEquals("Content should be updated", "<h2>Updated Content</h2><p>New body</p>", updated.prefilledHtml);
    }
    
    @Test
    public void testUpdateTemplateColor() {
        TemplateEntity template = new TemplateEntity(
            "Edit Test Color",
            "#E3F2FD",
            "<p>Content</p>",
            null,
            null,
            false
        );
        long id = repository.create(template);
        template.id = id;
        
        template.pageColor = "#E8F5E8";
        repository.update(template);
        
        TemplateEntity updated = repository.getById(id);
        assertEquals("Color should be updated", "#E8F5E8", updated.pageColor);
    }
    
    @Test
    public void testUpdateTemplatePreservesId() {
        TemplateEntity template = new TemplateEntity(
            "Edit Test Preserve",
            "#E3F2FD",
            "<p>Content</p>",
            null,
            null,
            false
        );
        long id = repository.create(template);
        template.id = id;
        
        template.name = "Edit Test Updated Name";
        repository.update(template);
        
        TemplateEntity updated = repository.getById(id);
        assertEquals("ID should be preserved", id, updated.id);
    }
}

