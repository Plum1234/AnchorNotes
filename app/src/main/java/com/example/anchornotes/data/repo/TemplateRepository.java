package com.example.anchornotes.data.repo;

import android.content.Context;
import android.text.TextUtils;

import com.example.anchornotes.context.GeofenceManager;
import com.example.anchornotes.data.ServiceLocator;
import com.example.anchornotes.data.db.AppDatabase;
import com.example.anchornotes.data.db.TemplateDao;
import com.example.anchornotes.data.db.TemplateEntity;
import com.example.anchornotes.model.TemplateWithProximity;
import com.example.anchornotes.util.LocationUtils;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TemplateRepository {
    private final TemplateDao dao;
    private final Context context;

    public TemplateRepository(Context context) {
        this.context = context.getApplicationContext();
        this.dao = AppDatabase.get(context).templateDao();
    }

    public long create(TemplateEntity template) {
        template.updatedAt = System.currentTimeMillis();
        if (template.createdAt == 0) {
            template.createdAt = template.updatedAt;
        }
        return dao.insert(template);
    }

    public void update(TemplateEntity template) {
        template.updatedAt = System.currentTimeMillis();
        dao.update(template);
    }

    public void delete(TemplateEntity template) {
        dao.delete(template);
    }

    public TemplateEntity getById(long id) {
        return dao.getById(id);
    }

    public List<TemplateEntity> getAll() {
        return dao.getAll();
    }

    public List<TemplateEntity> search(String query) {
        if (TextUtils.isEmpty(query)) {
            return getAll();
        }
        return dao.search(query);
    }

    /**
     * Get templates for selection, prioritized by current geofence location
     * @deprecated Use getTemplatesWithProximity instead
     */
    public List<TemplateEntity> getTemplatesForSelection() {
        try {
            GeofenceManager geofenceManager = ServiceLocator.geofenceManager(context);
            List<String> currentGeofenceIds = geofenceManager.getCurrentActiveGeofenceIds();

            if (currentGeofenceIds != null && !currentGeofenceIds.isEmpty()) {
                return dao.getTemplatesOrderedByGeofence(currentGeofenceIds);
            } else {
                return dao.getTemplatesNonGeofenceFirst();
            }
        } catch (Exception e) {
            // Fallback to normal ordering if geofence logic fails
            return dao.getAll();
        }
    }

    /**
     * Get templates with proximity information based on current location
     *
     * @param currentLat Current latitude
     * @param currentLon Current longitude
     * @return List of templates sorted by proximity (nearby first, then by update time)
     */
    public List<TemplateWithProximity> getTemplatesWithProximity(double currentLat, double currentLon) {
        List<TemplateEntity> withLocation = dao.getTemplatesWithLocation();
        List<TemplateEntity> withoutLocation = dao.getTemplatesWithoutLocation();
        List<TemplateWithProximity> result = new ArrayList<>();

        // Calculate distance for templates with location
        for (TemplateEntity t : withLocation) {
            if (t.latitude == null || t.longitude == null) continue;

            float radius = t.geofenceRadius != null ? t.geofenceRadius : 175.0f;
            double distance = LocationUtils.calculateDistance(currentLat, currentLon, t.latitude, t.longitude);
            boolean isNearby = distance <= radius;

            result.add(new TemplateWithProximity(t, distance, isNearby));
        }

        // Sort: nearby first (by distance), then others (by updatedAt)
        Collections.sort(result, (a, b) -> {
            if (a.isNearby && !b.isNearby) return -1;
            if (!a.isNearby && b.isNearby) return 1;
            if (a.isNearby && b.isNearby) return Double.compare(a.distance, b.distance);
            return Long.compare(b.template.updatedAt, a.template.updatedAt);
        });

        // Add non-location templates at end
        for (TemplateEntity t : withoutLocation) {
            result.add(new TemplateWithProximity(t, Double.MAX_VALUE, false));
        }

        return result;
    }

    public List<TemplateEntity> getExampleTemplates() {
        return dao.getExampleTemplates();
    }

    public boolean isNameUnique(String name, long excludeId) {
        if (excludeId > 0) {
            return dao.countByNameExcluding(name, excludeId) == 0;
        } else {
            return dao.countByName(name) == 0;
        }
    }

    // Helper methods for tag association handling

    /**
     * Parse JSON string of tag IDs into List
     */
    public List<Long> parseAssociatedTagIds(String tagIdsJson) {
        List<Long> tagIds = new ArrayList<>();
        if (TextUtils.isEmpty(tagIdsJson)) {
            return tagIds;
        }

        try {
            JSONArray jsonArray = new JSONArray(tagIdsJson);
            for (int i = 0; i < jsonArray.length(); i++) {
                tagIds.add(jsonArray.getLong(i));
            }
        } catch (JSONException e) {
            // Return empty list if JSON parsing fails
        }

        return tagIds;
    }

    /**
     * Convert List of tag IDs to JSON string
     */
    public String serializeTagIds(List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return null;
        }

        try {
            JSONArray jsonArray = new JSONArray();
            for (Long tagId : tagIds) {
                jsonArray.put(tagId);
            }
            return jsonArray.toString();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Ensure example templates are present (called on app startup)
     */
    public void ensureExampleTemplates() {
        if (dao.getExampleTemplateCount() == 0) {
            // Create example template
            String exampleHtml = "<h2>Meeting: [Title]</h2>" +
                    "<p><strong>Date:</strong> [Date]</p>" +
                    "<p><strong>Attendees:</strong></p>" +
                    "<ul><li>☐ Participant 1</li><li>☐ Participant 2</li></ul>" +
                    "<p><strong>Agenda:</strong></p>" +
                    "<p><strong>Notes:</strong></p>" +
                    "<p><strong>Action Items:</strong></p>" +
                    "<ul><li>☐ Task 1</li><li>☐ Task 2</li></ul>";

            TemplateEntity example = new TemplateEntity(
                    "Meeting Notes",
                    "#E3F2FD",
                    exampleHtml,
                    null,
                    null,
                    true
            );
            create(example);
        }
    }
}