package com.example.global_insights;

import android.content.Context;
import android.content.SharedPreferences;
import com.example.global_insights.model.Article;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class BookmarkManager {

    private static final String PREF_NAME = "global_insights_bookmarks";
    private static final String KEY_BOOKMARKS = "bookmarked_articles";
    private static final Gson gson = new Gson();

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static List<Article> getBookmarks(Context context) {
        String json = getPrefs(context).getString(KEY_BOOKMARKS, null);
        if (json == null || json.isEmpty()) {
            return new ArrayList<>();
        }
        Type type = new TypeToken<ArrayList<Article>>() {}.getType();
        List<Article> list = gson.fromJson(json, type);
        return list != null ? list : new ArrayList<>();
    }

    public static boolean isBookmarked(Context context, Article article) {
        if (article == null || article.getUrl() == null) return false;
        List<Article> bookmarks = getBookmarks(context);
        for (Article a : bookmarks) {
            if (article.getUrl().equals(a.getUrl())) {
                return true;
            }
        }
        return false;
    }

    public static boolean toggleBookmark(Context context, Article article) {
        if (article == null || article.getUrl() == null) return false;
        List<Article> bookmarks = getBookmarks(context);
        boolean wasBookmarked = false;
        Article toRemove = null;

        for (Article a : bookmarks) {
            if (article.getUrl().equals(a.getUrl())) {
                wasBookmarked = true;
                toRemove = a;
                break;
            }
        }

        if (wasBookmarked) {
            bookmarks.remove(toRemove);
        } else {
            bookmarks.add(article);
        }

        String json = gson.toJson(bookmarks);
        getPrefs(context).edit().putString(KEY_BOOKMARKS, json).apply();
        return !wasBookmarked; // Returns true if now bookmarked, false if unbookmarked
    }
}
