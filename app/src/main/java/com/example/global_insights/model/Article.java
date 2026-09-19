package com.example.global_insights.model;

public class Article {
    private String id;
    private Source source;
    private String author;
    private String title;
    private String description;
    private String url;
    private String urlToImage;
    private String publishedAt;
    private boolean isVernacular;
    private String vernacularBadge;
    private String originalTitle;
    private String originalDescription;
    private String vernacularSource;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Source getSource() {
        return source;
    }

    public void setSource(Source source) {
        this.source = source;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUrlToImage() {
        return urlToImage;
    }

    public void setUrlToImage(String urlToImage) {
        this.urlToImage = urlToImage;
    }

    public String getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(String publishedAt) {
        this.publishedAt = publishedAt;
    }

    public boolean isVernacular() {
        return isVernacular;
    }

    public void setVernacular(boolean vernacular) {
        isVernacular = vernacular;
    }

    public String getVernacularBadge() {
        return vernacularBadge;
    }

    public void setVernacularBadge(String vernacularBadge) {
        this.vernacularBadge = vernacularBadge;
    }

    public String getOriginalTitle() {
        return originalTitle;
    }

    public void setOriginalTitle(String originalTitle) {
        this.originalTitle = originalTitle;
    }

    public String getOriginalDescription() {
        return originalDescription;
    }

    public void setOriginalDescription(String originalDescription) {
        this.originalDescription = originalDescription;
    }

    public String getVernacularSource() {
        return vernacularSource;
    }

    public void setVernacularSource(String vernacularSource) {
        this.vernacularSource = vernacularSource;
    }
}
