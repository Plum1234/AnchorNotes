package com.example.anchornotes.data.db;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

/**
 * Entity representing a geofence associated with a template.
 * Stores the geographical coordinates and details for template-based location reminders.
 */
@Entity(tableName = "template_geofences")
public class TemplateGeofenceEntity {
    @PrimaryKey
    @NonNull
    public String geofenceId;  // e.g., "template-office", "template-home"

    public double latitude;
    public double longitude;
    public float radiusMeters;
    public String label;  // Human-readable name like "Office", "Home"

    public TemplateGeofenceEntity() {
        // Required by Room
        this.geofenceId = "";
    }

    @Ignore
    public TemplateGeofenceEntity(@NonNull String geofenceId, double latitude, double longitude,
                                  float radiusMeters, String label) {
        this.geofenceId = geofenceId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.radiusMeters = radiusMeters;
        this.label = label;
    }
}
