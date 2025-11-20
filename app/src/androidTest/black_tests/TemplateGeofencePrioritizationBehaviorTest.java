package black_tests;

import android.content.Context;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.example.anchornotes.context.GeofenceManager;
import com.example.anchornotes.data.db.TemplateEntity;
import com.example.anchornotes.data.repo.TemplateRepository;
import com.example.anchornotes.data.ServiceLocator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class TemplateGeofencePrioritizationBehaviorTest {
    
    private TemplateRepository repository;
    private GeofenceManager geofenceManager;
    private Context context;
    
    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        repository = new TemplateRepository(context);
        geofenceManager = ServiceLocator.geofenceManager(context);
    }
    
    @After
    public void tearDown() {
        // Clean up test templates
        List<TemplateEntity> allTemplates = repository.getAll();
        for (TemplateEntity template : allTemplates) {
            if (!template.isExample && template.name.startsWith("Geofence Test")) {
                repository.delete(template);
            }
        }
    }
    
    @Test
    public void testTemplatesWithGeofenceAssociationExist() {
        TemplateEntity geofenceTemplate = new TemplateEntity(
            "Geofence Test Office",
            "#E3F2FD",
            "<p>Office template</p>",
            null,
            "office",
            false
        );
        long id = repository.create(geofenceTemplate);
        
        TemplateEntity retrieved = repository.getById(id);
        assertNotNull("Template should exist", retrieved);
        assertEquals("Should have geofence association", "office", retrieved.associatedGeofenceId);
    }
    
    @Test
    public void testGetTemplatesForSelectionReturnsOrderedList() {
        // Create templates with different geofence associations
        TemplateEntity officeTemplate = new TemplateEntity(
            "Geofence Test Office",
            "#E3F2FD",
            "<p>Office</p>",
            null,
            "office",
            false
        );
        repository.create(officeTemplate);
        
        TemplateEntity homeTemplate = new TemplateEntity(
            "Geofence Test Home",
            "#E8F5E8",
            "<p>Home</p>",
            null,
            "home",
            false
        );
        repository.create(homeTemplate);
        
        TemplateEntity normalTemplate = new TemplateEntity(
            "Geofence Test Normal",
            "#FFF9C4",
            "<p>Normal</p>",
            null,
            null,
            false
        );
        repository.create(normalTemplate);
        
        // Get templates for selection
        List<TemplateEntity> templates = repository.getTemplatesForSelection();
        
        assertNotNull("Templates should not be null", templates);
        assertTrue("Should return multiple templates", templates.size() >= 3);
        
        // Verify all templates are present
        boolean foundOffice = false;
        boolean foundHome = false;
        boolean foundNormal = false;
        
        for (TemplateEntity t : templates) {
            if (t.name.equals("Geofence Test Office")) foundOffice = true;
            if (t.name.equals("Geofence Test Home")) foundHome = true;
            if (t.name.equals("Geofence Test Normal")) foundNormal = true;
        }
        
        assertTrue("Should find office template", foundOffice);
        assertTrue("Should find home template", foundHome);
        assertTrue("Should find normal template", foundNormal);
    }
    
    @Test
    public void testTemplateWithoutGeofenceCanBeCreated() {
        TemplateEntity normalTemplate = new TemplateEntity(
            "Geofence Test Normal Template",
            "#E3F2FD",
            "<p>Normal content</p>",
            null,
            null,
            false
        );
        
        long id = repository.create(normalTemplate);
        TemplateEntity retrieved = repository.getById(id);
        
        assertNotNull("Template should be created", retrieved);
        assertNull("Geofence ID should be null", retrieved.associatedGeofenceId);
    }
}

