package com.example.global_insights.model;

public class NewsModel {
    private String title;
    private String description;
    private String url;
    private String source;

    public NewsModel(String title, String description, String url, String source) {
        this.title = title;
        this.description = description;
        this.url = url;
        this.source = source;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getUrl() {
        return url;
    }

    public String getSource() {
        return source;
    }
}
