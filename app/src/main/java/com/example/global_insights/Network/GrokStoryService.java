package com.example.global_insights.Network;

import android.os.Handler;
import android.os.Looper;

import com.example.global_insights.model.AskStoryModel;
import com.example.global_insights.model.Story;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GrokStoryService {

    // Default to FastAPI Backend running locally / deployed
    private static String BACKEND_BASE_URL = "http://10.0.2.2:8080";
    // Direct Grok API Backup
    private static String GROK_API_KEY = "";
    private static final String GROK_DIRECT_URL = "https://api.x.ai/v1/chat/completions";

    private static final ExecutorService executor = Executors.newFixedThreadPool(4);
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());
    private static final Gson gson = new Gson();

    public static void setBackendUrl(String url) {
        if (url != null && !url.isEmpty()) {
            BACKEND_BASE_URL = url;
        }
    }

    public static void setGrokApiKey(String apiKey) {
        GROK_API_KEY = apiKey;
    }

    public interface SuggestedQuestionsCallback {
        void onSuccess(List<String> questions);
        void onError(String errorMessage);
    }

    public interface AskStoryCallback {
        void onSuccess(String answerText, List<AskStoryModel.Citation> citations, List<String> followUps);
        void onError(String errorMessage);
    }

    /**
     * Retrieves dynamic question chips for a Story.
     */
    public static void generateSuggestedQuestions(Story story, SuggestedQuestionsCallback callback) {
        executor.execute(() -> {
            try {
                // Try FastAPI Backend First
                JsonObject req = new JsonObject();
                req.addProperty("story_id", story.getStoryId());
                req.addProperty("title", story.getTitle());
                req.addProperty("summary", story.getSummary());
                req.addProperty("source", story.getSource());

                String resStr = postJson(BACKEND_BASE_URL + "/api/suggest-questions", gson.toJson(req));
                JsonObject resObj = new JsonParser().parse(resStr).getAsJsonObject();
                JsonArray qArr = resObj.getAsJsonArray("questions");

                List<String> questions = new ArrayList<>();
                for (int i = 0; i < qArr.size(); i++) {
                    questions.add(qArr.get(i).getAsString());
                }

                mainHandler.post(() -> callback.onSuccess(questions));
            } catch (Exception backendEx) {
                // Fallback to direct Grok API or High-Utility Default Chips
                List<String> fallbacks = new ArrayList<>();
                fallbacks.add("What actually happened?");
                fallbacks.add("Explain this simply");
                fallbacks.add("Who is affected?");
                fallbacks.add("Why does this matter?");
                fallbacks.add("What happens next?");
                mainHandler.post(() -> callback.onSuccess(fallbacks));
            }
        });
    }

    /**
     * Answers a user query about a Story using Grok AI.
     */
    public static void askStoryQuestion(Story story, String userQuestion, List<AskStoryModel.ChatMessage> history, AskStoryCallback callback) {
        executor.execute(() -> {
            try {
                // Try FastAPI Backend First
                JsonObject req = new JsonObject();
                req.addProperty("story_id", story.getStoryId());
                req.addProperty("title", story.getTitle());
                req.addProperty("summary", story.getSummary());
                req.addProperty("source_name", story.getSource());
                req.addProperty("source_url", story.getSourceUrl());
                req.addProperty("user_query", userQuestion);

                JsonArray historyArr = new JsonArray();
                if (history != null) {
                    for (AskStoryModel.ChatMessage msg : history) {
                        JsonObject hObj = new JsonObject();
                        hObj.addProperty("role", msg.getRole() == AskStoryModel.ChatMessage.ROLE_USER ? "User" : "AI");
                        hObj.addProperty("text", msg.getText());
                        historyArr.add(hObj);
                    }
                }
                req.add("chat_history", historyArr);

                String resStr = postJson(BACKEND_BASE_URL + "/api/ask-story", gson.toJson(req));
                JsonObject resObj = new JsonParser().parse(resStr).getAsJsonObject();

                String answerText = resObj.has("answer_text") ? resObj.get("answer_text").getAsString() : "No answer returned.";

                List<AskStoryModel.Citation> citations = new ArrayList<>();
                if (resObj.has("sources")) {
                    JsonArray sArr = resObj.getAsJsonArray("sources");
                    for (int i = 0; i < sArr.size(); i++) {
                        JsonObject sObj = sArr.get(i).getAsJsonObject();
                        citations.add(new AskStoryModel.Citation(
                                sObj.get("sourceName").getAsString(),
                                sObj.has("url") ? sObj.get("url").getAsString() : "",
                                story.getTitle(),
                                sObj.has("tier") ? sObj.get("tier").getAsString() : "Primary Source"
                        ));
                    }
                }

                List<String> followUps = new ArrayList<>();
                if (resObj.has("follow_ups")) {
                    JsonArray fArr = resObj.getAsJsonArray("follow_ups");
                    for (int i = 0; i < fArr.size(); i++) {
                        followUps.add(fArr.get(i).getAsString());
                    }
                }

                mainHandler.post(() -> callback.onSuccess(answerText, citations, followUps));
            } catch (Exception backendEx) {
                // If Backend Offline, fallback to friendly message
                mainHandler.post(() -> callback.onError("Unable to connect to Global Insight FastAPI Backend. Please verify uvicorn server is running."));
            }
        });
    }

    private static String postJson(String urlString, String jsonBody) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; utf-8");
        conn.setDoOutput(true);
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(12000);

        byte[] input = jsonBody.getBytes(StandardCharsets.UTF_8);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(input, 0, input.length);
        }

        int code = conn.getResponseCode();
        BufferedReader br;
        if (code >= 200 && code < 300) {
            br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
        } else {
            br = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8));
        }

        StringBuilder response = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            response.append(line.trim());
        }
        return response.toString();
    }
}
