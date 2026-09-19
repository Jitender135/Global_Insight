package com.example.global_insights;

import org.junit.Test;
import static org.junit.Assert.*;

public class LocationHelperTest {

    // Haversine formula distance calculation in kilometers
    public static double calculateDistanceKm(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Earth radius in km
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    @Test
    public void testDistanceCalculation_SamePoint_ReturnsZero() {
        double distance = calculateDistanceKm(28.4595, 77.0266, 28.4595, 77.0266);
        assertEquals(0.0, distance, 0.001);
    }

    @Test
    public void testDistanceCalculation_Within10KmRadius() {
        // Palam Vihar (28.5039, 77.0270) to Sector 14 Gurugram (28.4750, 77.0420) ~ 3.5 km
        double distance = calculateDistanceKm(28.5039, 77.0270, 28.4750, 77.0420);
        assertTrue("Distance should be within 10 km radius", distance < 10.0);
        assertTrue("Distance should be greater than 0", distance > 0.0);
    }

    @Test
    public void testDistanceCalculation_Exceeds10KmRadius() {
        // Gurugram (28.4595, 77.0266) to Connaught Place Delhi (28.6304, 77.2177) ~ 26 km
        double distance = calculateDistanceKm(28.4595, 77.0266, 28.6304, 77.2177);
        assertTrue("Distance should exceed 10 km radius", distance > 10.0);
    }
}
