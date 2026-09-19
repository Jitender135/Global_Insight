package com.example.global_insights;

import com.example.global_insights.model.Article;
import com.example.global_insights.model.SavedLocation;
import com.example.global_insights.model.Source;

import org.junit.Test;
import static org.junit.Assert.*;

public class StoryCacheAndModelTest {

    @Test
    public void testSavedLocationInitialization() {
        SavedLocation loc = new SavedLocation(
                "Home", "🏠", "Palam Vihar, Gurugram", "Palam Vihar",
                "Gurugram", "122017", 28.5039, 77.0270
        );

        assertNotNull(loc.getId());
        assertEquals("Home", loc.getLabel());
        assertEquals("🏠", loc.getTagIcon());
        assertEquals("Palam Vihar, Gurugram", loc.getAddressLine());
        assertEquals(28.5039, loc.getLatitude(), 0.0001);
        assertEquals(77.0270, loc.getLongitude(), 0.0001);
    }

    @Test
    public void testArticleModelGettersAndSetters() {
        Source source = new Source("tech-crunch", "TechCrunch");
        Article article = new Article();
        article.setTitle("AI Breakthrough 2026");
        article.setDescription("A massive advance in AI efficiency.");
        article.setUrl("https://techcrunch.com/article/ai-breakthrough");
        article.setUrlToImage("https://techcrunch.com/image.jpg");
        article.setPublishedAt("2026-09-19T10:00:00Z");
        article.setSource(source);

        assertEquals("AI Breakthrough 2026", article.getTitle());
        assertEquals("A massive advance in AI efficiency.", article.getDescription());
        assertEquals("https://techcrunch.com/article/ai-breakthrough", article.getUrl());
        assertNotNull(article.getSource());
        assertEquals("TechCrunch", article.getSource().getName());
    }

    @Test
    public void testSavedLocationDefaultIcon() {
        SavedLocation loc = new SavedLocation();
        assertEquals("📍", loc.getTagIcon());
        assertNotNull(loc.getId());
    }
}
