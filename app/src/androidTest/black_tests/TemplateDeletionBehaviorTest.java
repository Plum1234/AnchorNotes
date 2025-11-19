package black_tests;

import android.content.Context;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.example.anchornotes.data.db.TemplateEntity;
import com.example.anchornotes.data.repo.TemplateRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class TemplateDeletionBehaviorTest {
    
    private TemplateRepository repository;
    private Context context;
    
    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        repository = new TemplateRepository(context);
    }
    
    @Test
    public void testDeleteTemplateRemovesFromDatabase() {
        TemplateEntity template = new TemplateEntity(
            "Delete Test Template",
            "#E3F2FD",
            "<p>To be deleted</p>",
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
    public void testDeleteTemplateRemovesFromList() {
        TemplateEntity template = new TemplateEntity(
            "Delete Test List Template",
            "#E3F2FD",
            "<p>Content</p>",
            null,
            null,
            false
        );
        long id = repository.create(template);
        template.id = id;
        
        // Verify it's in the list
        List<TemplateEntity> beforeDelete = repository.getAll();
        boolean foundBefore = false;
        for (TemplateEntity t : beforeDelete) {
            if (t.id == id) {
                foundBefore = true;
                break;
            }
        }
        assertTrue("Template should be in list before deletion", foundBefore);
        
        repository.delete(template);
        
        // Verify it's not in the list
        List<TemplateEntity> afterDelete = repository.getAll();
        boolean foundAfter = false;
        for (TemplateEntity t : afterDelete) {
            if (t.id == id) {
                foundAfter = true;
                break;
            }
        }
        assertFalse("Template should not be in list after deletion", foundAfter);
    }
    
    @Test
    public void testDeleteNonExistentTemplateDoesNotCrash() {
        TemplateEntity nonExistent = new TemplateEntity(
            "Non Existent",
            "#E3F2FD",
            "<p>Content</p>",
            null,
            null,
            false
        );
        nonExistent.id = 99999L; // Non-existent ID
        
        // Should not throw exception
        try {
            repository.delete(nonExistent);
            // If we get here, deletion didn't crash
            assertTrue("Deletion should not crash", true);
        } catch (Exception e) {
            fail("Deletion should not throw exception: " + e.getMessage());
        }
    }
    
    @Test
    public void testDeleteMultipleTemplates() {
        // Create multiple templates
        List<Long> ids = new java.util.ArrayList<>();
        for (int i = 0; i < 3; i++) {
            TemplateEntity template = new TemplateEntity(
                "Delete Test Multiple " + i,
                "#E3F2FD",
                "<p>Content " + i + "</p>",
                null,
                null,
                false
            );
            long id = repository.create(template);
            ids.add(id);
        }
        
        // Delete all of them
        for (Long id : ids) {
            TemplateEntity template = repository.getById(id);
            if (template != null) {
                repository.delete(template);
            }
        }
        
        // Verify all are deleted
        for (Long id : ids) {
            TemplateEntity deleted = repository.getById(id);
            assertNull("Template " + id + " should be deleted", deleted);
        }
    }
}

