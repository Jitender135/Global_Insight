package com.example.global_insights.model;

import java.util.UUID;

public class Story {
    private String storyId;
    private String title;
    private String summary;
    private String content;
    private String source;
    private String sourceUrl;
    private String category;
    private String publishedDate;

    public Story(String storyId, String title, String summary, String content, String source, String sourceUrl, String category, String publishedDate) {
        this.storyId = storyId != null ? storyId : UUID.randomUUID().toString();
        this.title = title;
        this.summary = summary;
        this.content = content;
        this.source = source;
        this.sourceUrl = sourceUrl;
        this.category = category;
        this.publishedDate = publishedDate;
    }

    public static Story fromArticle(Article article) {
        if (article == null) return null;
        String sName = article.getSource() != null && article.getSource().getName() != null ? article.getSource().getName() : "Global News";
        String sId = article.getId() != null && !article.getId().isEmpty() ? article.getId() : String.valueOf(article.getTitle() != null ? article.getTitle().hashCode() : UUID.randomUUID().hashCode());

        return new Story(
                sId,
                article.getTitle(),
                article.getDescription(),
                article.getDescription(),
                sName,
                article.getUrl(),
                "General",
                article.getPublishedAt()
        );
    }

    public String getStoryId() {
        return storyId;
    }

    public String getTitle() {
        return title;
    }

    public String getSummary() {
        return summary;
    }

    public String getContent() {
        return content;
    }

    public String getSource() {
        return source;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public String getCategory() {
        return category;
    }

    public String getPublishedDate() {
        return publishedDate;
    }
}
