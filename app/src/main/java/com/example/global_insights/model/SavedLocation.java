package com.example.global_insights.model;

import java.io.Serializable;
import java.util.UUID;

/**
 * Model representing a user's saved location (e.g. Home in Palam Vihar, College at BMU Kapriwas).
 */
public class SavedLocation implements Serializable {

    private String id;
    private String label;        // e.g. "Home", "College", "Work", "Hostel"
    private String tagIcon;      // "🏠", "🎓", "💼", "📍"
    private String addressLine;  // e.g. "Palam Vihar, Gurugram (122017)"
    private String subLocality;  // "Palam Vihar"
    private String locality;     // "Gurugram"
    private String postalCode;   // "122017"
    private double latitude;
    private double longitude;
    private long createdAt;

    // Transient or display-only field for distance from user's current GPS in km
    private double distanceFromCurrentKm = -1;

    public SavedLocation() {
        this.id = UUID.randomUUID().toString();
        this.createdAt = System.currentTimeMillis();
        this.tagIcon = "📍";
    }

    public SavedLocation(String label, String tagIcon, String addressLine, String subLocality,
                         String locality, String postalCode, double latitude, double longitude) {
        this.id = UUID.randomUUID().toString();
        this.label = label;
        this.tagIcon = tagIcon != null && !tagIcon.isEmpty() ? tagIcon : "📍";
        this.addressLine = addressLine;
        this.subLocality = subLocality;
        this.locality = locality;
        this.postalCode = postalCode;
        this.latitude = latitude;
        this.longitude = longitude;
        this.createdAt = System.currentTimeMillis();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLabel() {
        return label != null ? label : "Saved Place";
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getTagIcon() {
        return tagIcon != null ? tagIcon : "📍";
    }

    public void setTagIcon(String tagIcon) {
        this.tagIcon = tagIcon;
    }

    public String getAddressLine() {
        return addressLine != null ? addressLine : "";
    }

    public void setAddressLine(String addressLine) {
        this.addressLine = addressLine;
    }

    public String getSubLocality() {
        return subLocality;
    }

    public void setSubLocality(String subLocality) {
        this.subLocality = subLocality;
    }

    public String getLocality() {
        return locality;
    }

    public void setLocality(String locality) {
        this.locality = locality;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public double getDistanceFromCurrentKm() {
        return distanceFromCurrentKm;
    }

    public void setDistanceFromCurrentKm(double distanceFromCurrentKm) {
        this.distanceFromCurrentKm = distanceFromCurrentKm;
    }

    /**
     * Clean readable display query for news fetching (e.g. "Palam Vihar, Gurugram" or "Kapriwas")
     */
    public String getNewsSearchQuery() {
        if (subLocality != null && !subLocality.trim().isEmpty()) {
            if (locality != null && !locality.trim().isEmpty() && !subLocality.equalsIgnoreCase(locality)) {
                return subLocality.trim() + " " + locality.trim();
            }
            return subLocality.trim();
        }
        if (locality != null && !locality.trim().isEmpty()) {
            return locality.trim();
        }
        return "local news";
    }

    /**
     * Primary area keyword (e.g. "Palam Vihar" or "Kapriwas")
     */
    public String getPrimaryAreaName() {
        if (subLocality != null && !subLocality.trim().isEmpty()) {
            return subLocality.trim();
        }
        if (locality != null && !locality.trim().isEmpty()) {
            return locality.trim();
        }
        return getLabel();
    }
}
