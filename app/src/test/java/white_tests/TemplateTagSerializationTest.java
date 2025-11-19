package white_tests;

import android.content.Context;
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
public class TemplateTagSerializationTest {
    
    private TemplateRepository repository;
    private Context context;
    
    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
        repository = new TemplateRepository(context);
    }
    
    @Test
    public void testSerializeEmptyTagList() {
        List<Long> emptyList = new ArrayList<>();
        String result = repository.serializeTagIds(emptyList);
        assertNull("Empty list should serialize to null", result);
    }
    
    @Test
    public void testSerializeNullTagList() {
        String result = repository.serializeTagIds(null);
        assertNull("Null list should serialize to null", result);
    }
    
    @Test
    public void testSerializeSingleTag() {
        List<Long> tags = new ArrayList<>();
        tags.add(5L);
        
        String serialized = repository.serializeTagIds(tags);
        assertNotNull("Single tag should serialize", serialized);
        assertEquals("Should be valid JSON array", "[5]", serialized);
    }
    
    @Test
    public void testSerializeMultipleTags() {
        List<Long> tags = new ArrayList<>();
        tags.add(1L);
        tags.add(2L);
        tags.add(3L);
        
        String serialized = repository.serializeTagIds(tags);
        assertNotNull("Multiple tags should serialize", serialized);
        assertTrue("Should contain all tag IDs", 
            serialized.contains("1") && serialized.contains("2") && serialized.contains("3"));
    }
    
    @Test
    public void testParseEmptyJsonArray() {
        String emptyJson = "[]";
        List<Long> parsed = repository.parseAssociatedTagIds(emptyJson);
        assertNotNull("Parsed list should not be null", parsed);
        assertEquals("Empty JSON should parse to empty list", 0, parsed.size());
    }
    
    @Test
    public void testParseNullJson() {
        List<Long> parsed = repository.parseAssociatedTagIds(null);
        assertNotNull("Parsed list should not be null", parsed);
        assertEquals("Null should parse to empty list", 0, parsed.size());
    }
    
    @Test
    public void testParseInvalidJson() {
        String invalidJson = "not valid json";
        List<Long> parsed = repository.parseAssociatedTagIds(invalidJson);
        assertNotNull("Parsed list should not be null", parsed);
        assertEquals("Invalid JSON should parse to empty list", 0, parsed.size());
    }
    
    @Test
    public void testRoundTripSerialization() {
        List<Long> original = new ArrayList<>();
        original.add(10L);
        original.add(20L);
        original.add(30L);
        
        String serialized = repository.serializeTagIds(original);
        List<Long> parsed = repository.parseAssociatedTagIds(serialized);
        
        assertEquals("Round trip should preserve size", original.size(), parsed.size());
        for (int i = 0; i < original.size(); i++) {
            assertEquals("Round trip should preserve tag IDs", original.get(i), parsed.get(i));
        }
    }
}

