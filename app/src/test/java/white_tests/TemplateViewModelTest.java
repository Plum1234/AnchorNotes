package white_tests;

import android.app.Application;
import com.example.anchornotes.data.db.TemplateEntity;
import com.example.anchornotes.data.repo.TemplateRepository;
import com.example.anchornotes.viewmodel.TemplateViewModel;
import com.example.anchornotes.data.ServiceLocator;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
public class TemplateViewModelTest {
    
    private TemplateViewModel viewModel;
    private Application application;
    
    @Before
    public void setUp() {
        application = RuntimeEnvironment.getApplication();
        viewModel = new TemplateViewModel(application);
    }
    
    @Test
    public void testCreateTemplate() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        final boolean[] success = {false};
        
        viewModel.getSuccessMessage().observeForever(message -> {
            if (message != null && message.contains("created")) {
                success[0] = true;
                latch.countDown();
            }
        });
        
        viewModel.createTemplate(
            "Test Template",
            "#E3F2FD",
            "<p>Test content</p>",
            null,
            null
        );
        
        assertTrue("Should complete within timeout", latch.await(5, TimeUnit.SECONDS));
        assertTrue("Template creation should succeed", success[0]);
    }
    
    @Test
    public void testUpdateTemplate() throws InterruptedException {
        // First create a template
        TemplateRepository repo = ServiceLocator.templateRepository(application);
        TemplateEntity template = new TemplateEntity(
            "Original",
            "#E3F2FD",
            "<p>Original</p>",
            null,
            null,
            false
        );
        long id = repo.create(template);
        
        CountDownLatch latch = new CountDownLatch(1);
        final boolean[] success = {false};
        
        viewModel.getSuccessMessage().observeForever(message -> {
            if (message != null && message.contains("updated")) {
                success[0] = true;
                latch.countDown();
            }
        });
        
        viewModel.updateTemplate(
            id,
            "Updated",
            "#E8F5E8",
            "<p>Updated</p>",
            null,
            null
        );
        
        assertTrue("Should complete within timeout", latch.await(5, TimeUnit.SECONDS));
        assertTrue("Template update should succeed", success[0]);
    }
    
    @Test
    public void testDeleteTemplate() throws InterruptedException {
        // Create a template to delete
        TemplateRepository repo = ServiceLocator.templateRepository(application);
        TemplateEntity template = new TemplateEntity(
            "To Delete",
            "#E3F2FD",
            "<p>Delete</p>",
            null,
            null,
            false
        );
        repo.create(template);
        
        CountDownLatch latch = new CountDownLatch(1);
        final boolean[] success = {false};
        
        viewModel.getSuccessMessage().observeForever(message -> {
            if (message != null && message.contains("deleted")) {
                success[0] = true;
                latch.countDown();
            }
        });
        
        viewModel.deleteTemplate(template);
        
        assertTrue("Should complete within timeout", latch.await(5, TimeUnit.SECONDS));
        assertTrue("Template deletion should succeed", success[0]);
    }
    
    @Test
    public void testDuplicateTemplate() throws InterruptedException {
        TemplateRepository repo = ServiceLocator.templateRepository(application);
        TemplateEntity original = new TemplateEntity(
            "Original",
            "#E3F2FD",
            "<p>Content</p>",
            null,
            null,
            false
        );
        repo.create(original);
        
        CountDownLatch latch = new CountDownLatch(1);
        final boolean[] success = {false};
        
        viewModel.getSuccessMessage().observeForever(message -> {
            if (message != null && message.contains("duplicated")) {
                success[0] = true;
                latch.countDown();
            }
        });
        
        viewModel.duplicateTemplate(original);
        
        assertTrue("Should complete within timeout", latch.await(5, TimeUnit.SECONDS));
        assertTrue("Template duplication should succeed", success[0]);
    }
    
    @Test
    public void testLoadAllTemplates() throws InterruptedException {
        // Create some test templates
        TemplateRepository repo = ServiceLocator.templateRepository(application);
        for (int i = 0; i < 3; i++) {
            TemplateEntity template = new TemplateEntity(
                "Template " + i,
                "#E3F2FD",
                "<p>Content " + i + "</p>",
                null,
                null,
                false
            );
            repo.create(template);
        }
        
        CountDownLatch latch = new CountDownLatch(1);
        final List<TemplateEntity>[] loadedTemplates = new List[]{null};
        
        viewModel.getAllTemplates().observeForever(templates -> {
            if (templates != null && templates.size() >= 3) {
                loadedTemplates[0] = templates;
                latch.countDown();
            }
        });
        
        viewModel.loadAllTemplates();
        
        assertTrue("Should complete within timeout", latch.await(5, TimeUnit.SECONDS));
        assertNotNull("Templates should be loaded", loadedTemplates[0]);
        assertTrue("Should load at least 3 templates", loadedTemplates[0].size() >= 3);
    }
}

