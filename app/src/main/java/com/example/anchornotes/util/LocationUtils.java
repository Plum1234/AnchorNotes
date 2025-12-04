package com.example.anchornotes.util;

/**
 * Utility class for location and distance calculations
 */
public class LocationUtils {

    /**
     * Calculate distance between two geographic coordinates using the Haversine formula
     *
     * @param lat1 Latitude of first point
     * @param lon1 Longitude of first point
     * @param lat2 Latitude of second point
     * @param lon2 Longitude of second point
     * @return Distance in meters
     */
    public static double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371000; // Earth radius in meters

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c;
    }

    /**
     * Check if a location is nearby based on radius
     *
     * @param currentLat Current latitude
     * @param currentLon Current longitude
     * @param templateLat Template location latitude
     * @param templateLon Template location longitude
     * @param radiusMeters Radius threshold in meters
     * @return true if distance is within radius
     */
    public static boolean isNearby(double currentLat, double currentLon,
                                   double templateLat, double templateLon,
                                   float radiusMeters) {
        return calculateDistance(currentLat, currentLon, templateLat, templateLon) <= radiusMeters;
    }
}
