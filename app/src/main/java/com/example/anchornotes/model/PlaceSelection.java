package com.example.anchornotes.model;

/**
 * Represents a place selection for geofence reminders.
 */
public class PlaceSelection {
    public final double latitude;
    public final double longitude;
    public final float radiusMeters;
    public final String label;

    public PlaceSelection(double latitude, double longitude, float radiusMeters, String label) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.radiusMeters = radiusMeters;
        this.label = label;
    }
}
