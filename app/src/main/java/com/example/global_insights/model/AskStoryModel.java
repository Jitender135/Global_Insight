package com.example.global_insights.model;

import java.util.ArrayList;
import java.util.List;

public class AskStoryModel {

    public static class ChatMessage {
        public static final int ROLE_USER = 0;
        public static final int ROLE_AI = 1;

        private int role;
        private String text;
        private long timestamp;
        private List<Citation> citations;
        private boolean isThinking;

        public ChatMessage(int role, String text) {
            this.role = role;
            this.text = text;
            this.timestamp = System.currentTimeMillis();
            this.citations = new ArrayList<>();
            this.isThinking = false;
        }

        public int getRole() {
            return role;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public List<Citation> getCitations() {
            return citations;
        }

        public void setCitations(List<Citation> citations) {
            this.citations = citations;
        }

        public boolean isThinking() {
            return isThinking;
        }

        public void setThinking(boolean thinking) {
            isThinking = thinking;
        }
    }

    public static class Citation {
        private String sourceName;
        private String url;
        private String snippet;
        private String tier; // e.g. "Primary Source", "Established News"

        public Citation(String sourceName, String url, String snippet, String tier) {
            this.sourceName = sourceName;
            this.url = url;
            this.snippet = snippet;
            this.tier = tier;
        }

        public String getSourceName() {
            return sourceName;
        }

        public String getUrl() {
            return url;
        }

        public String getSnippet() {
            return snippet;
        }

        public String getTier() {
            return tier;
        }
    }
}
