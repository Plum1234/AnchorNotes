package com.example.anchornotes.model;

import com.example.anchornotes.data.db.TemplateEntity;

/**
 * Model class that wraps a TemplateEntity with proximity information
 * Used for sorting templates by distance from current location
 */
public class TemplateWithProximity {
    public final TemplateEntity template;
    public final double distance; // Distance in meters from current location
    public final boolean isNearby; // true if within template's geofenceRadius

    public TemplateWithProximity(TemplateEntity template, double distance, boolean isNearby) {
        this.template = template;
        this.distance = distance;
        this.isNearby = isNearby;
    }
}
