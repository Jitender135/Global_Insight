package com.example.global_insights.model;

import java.util.List;

public class NewsResponse {
    private String status;
    private int totalResults;
    private List<Article> articles;
    private int radiusKm;
    private boolean isExpanded;
    private String expansionReason;
    private String area;
    private String geoLevel; // "village", "tehsil", "district", "state"
    private String geoLabel; // e.g. "Kharkhari", "Farrukhnagar", "Gurugram"

    public String getStatus() {
        return status;
    }

    public int getTotalResults() {
        return totalResults;
    }

    public List<Article> getArticles() {
        return articles;
    }

    public int getRadiusKm() {
        return radiusKm;
    }

    public boolean isExpanded() {
        return isExpanded;
    }

    public String getExpansionReason() {
        return expansionReason;
    }

    public String getArea() {
        return area;
    }

    public String getGeoLevel() {
        return geoLevel;
    }

    public String getGeoLabel() {
        return geoLabel;
    }
}
