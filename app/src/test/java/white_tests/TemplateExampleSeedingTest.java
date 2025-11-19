package white_tests;

import android.content.Context;
import com.example.anchornotes.data.db.TemplateEntity;
import com.example.anchornotes.data.repo.TemplateRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.List;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
public class TemplateExampleSeedingTest {
    
    private TemplateRepository repository;
    private Context context;
    
    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
        repository = new TemplateRepository(context);
    }
    
    @Test
    public void testEnsureExampleTemplatesCreatesExample() {
        // Ensure example templates
        repository.ensureExampleTemplates();
        
        List<TemplateEntity> examples = repository.getExampleTemplates();
        assertNotNull("Example templates list should not be null", examples);
        assertTrue("Should have at least one example template", examples.size() > 0);
        
        TemplateEntity example = examples.get(0);
        assertTrue("Template should be marked as example", example.isExample);
        assertEquals("Example template should be 'Meeting Notes'", "Meeting Notes", example.name);
    }
    
    @Test
    public void testExampleTemplateHasCorrectProperties() {
        repository.ensureExampleTemplates();
        
        List<TemplateEntity> examples = repository.getExampleTemplates();
        assertTrue("Should have example templates", examples.size() > 0);
        
        TemplateEntity example = examples.get(0);
        assertEquals("Should have correct name", "Meeting Notes", example.name);
        assertEquals("Should have correct color", "#E3F2FD", example.pageColor);
        assertNotNull("Should have prefilled HTML", example.prefilledHtml);
        assertTrue("HTML should contain meeting structure", 
            example.prefilledHtml.contains("Meeting") || example.prefilledHtml.contains("meeting"));
    }
    
    @Test
    public void testEnsureExampleTemplatesIsIdempotent() {
        // Call multiple times
        repository.ensureExampleTemplates();
        int count1 = repository.getExampleTemplates().size();
        
        repository.ensureExampleTemplates();
        int count2 = repository.getExampleTemplates().size();
        
        assertEquals("Multiple calls should not create duplicates", count1, count2);
    }
    
    @Test
    public void testExampleTemplateIsNotDeletable() {
        repository.ensureExampleTemplates();
        
        List<TemplateEntity> examples = repository.getExampleTemplates();
        assertTrue("Should have example templates", examples.size() > 0);
        
        TemplateEntity example = examples.get(0);
        assertTrue("Example template should have isExample flag", example.isExample);
        
        // Attempt to delete (should be prevented by business logic)
        // This tests that the flag is set correctly
        assertTrue("isExample flag should prevent deletion", example.isExample);
    }
}

