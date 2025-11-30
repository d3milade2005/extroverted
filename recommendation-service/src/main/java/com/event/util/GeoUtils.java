package com.event.util;

import com.event.dto.Location;

public class GeoUtils {
    private static final double EARTH_RADIUS_KM = 6371.0;

    /**
     * Calculate distance between two locations using Haversine formula
     *
     * @param loc1 First location
     * @param loc2 Second location
     * @return Distance in kilometers
     */
    public static double calculateDistance(Location loc1, Location loc2) {
        if (loc1 == null || loc2 == null || !loc1.isValid() || !loc2.isValid()) {
            return Double.MAX_VALUE;
        }

        double lat1 = Math.toRadians(loc1.getLatitude());
        double lon1 = Math.toRadians(loc1.getLongitude());
        double lat2 = Math.toRadians(loc2.getLatitude());
        double lon2 = Math.toRadians(loc2.getLongitude());

        double dLat = lat2 - lat1;
        double dLon = lon2 - lon1;

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(lat1) * Math.cos(lat2) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_KM * c;
    }

    /**
     * Calculate geo score based on distance
     * Closer events get higher scores
     *
     * @param distance Distance in kilometers
     * @return Score between 0.0 and 1.0
     */
    public static double calculateGeoScore(double distance) {
        if (distance < 0) {
            return 0.0;
        }

        // Scoring tiers
        if (distance <= 5) {
            return 1.0;  // Within 5km: perfect score
        } else if (distance <= 10) {
            return 0.8;  // 5-10km: high score
        } else if (distance <= 20) {
            return 0.5;  // 10-20km: medium score
        } else if (distance <= 50) {
            return 0.2;  // 20-50km: low score
        } else {
            return 0.0;  // >50km: no score
        }
    }

    /**
     * Check if location is within radius
     *
     * @param center Center location
     * @param point Point to check
     * @param radiusKm Radius in kilometers
     * @return true if point is within radius
     */
    public static boolean isWithinRadius(Location center, Location point, double radiusKm) {
        if (center == null || point == null || !center.isValid() || !point.isValid()) {
            return false;
        }

        double distance = calculateDistance(center, point);
        return distance <= radiusKm;
    }

    /**
     * Get human-readable distance string
     *
     * @param distanceKm Distance in kilometers
     * @return Formatted string (e.g., "2.5 km", "500 m")
     */
    public static String formatDistance(double distanceKm) {
        if (distanceKm < 1.0) {
            return String.format("%.0f m", distanceKm * 1000);
        } else {
            return String.format("%.1f km", distanceKm);
        }
    }
}
