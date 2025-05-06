package com.example.global_insights.model;

public class Report {
    private String email;
    private String fullName;
    private String reason;
    private String explanation;

    public Report() {
        // Required for Firebase
    }

    public Report(String email, String fullName, String reason, String explanation) {
        this.email = email;
        this.fullName = fullName;
        this.reason = reason;
        this.explanation = explanation;
    }

    public String getEmail() {
        return email;
    }

    public String getFullName() {
        return fullName;
    }

    public String getReason() {
        return reason;
    }

    public String getExplanation() {
        return explanation;
    }
}
