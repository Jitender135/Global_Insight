package com.example.global_insights.Network;

import android.os.Handler;
import android.os.Looper;

import com.example.global_insights.model.Article;
import com.example.global_insights.model.AskStoryModel;
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

public class GeminiStoryService {

    // You can set your API key here or load it from config
    private static String GEMINI_API_KEY = ""; 
    private static final String GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=";

    private static final ExecutorService executor = Executors.newFixedThreadPool(4);
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());
    private static final Gson gson = new Gson();

    public static void setApiKey(String apiKey) {
        GEMINI_API_KEY = apiKey;
    }

    public static String getApiKey() {
        return GEMINI_API_KEY;
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
     * Dynamically generates 4-5 contextual questions specifically for the given Article.
     */
    public static void generateSuggestedQuestions(Article article, SuggestedQuestionsCallback callback) {
        executor.execute(() -> {
            try {
                String prompt = "You are Global Insight AI. Analyze the following news story and generate 5 short, highly relevant questions that a reader would want to ask to understand this specific story deeply.\n" +
                        "Category/Domain appropriate questions (e.g., 'What actually changed?', 'Who is affected?', 'Why did this happen?', 'Explain simply', 'What happens next?').\n" +
                        "Story Title: " + article.getTitle() + "\n" +
                        "Story Content: " + (article.getDescription() != null ? article.getDescription() : "") + "\n" +
                        "Output ONLY a raw JSON array of 5 string questions, like: [\"Question 1\", \"Question 2\", \"Question 3\", \"Question 4\", \"Question 5\"]";

                String responseJson = callGeminiApi(prompt);
                List<String> questions = parseSuggestedQuestions(responseJson);

                mainHandler.post(() -> {
                    if (questions != null && !questions.isEmpty()) {
                        callback.onSuccess(questions);
                    } else {
                        // Default fallback chips
                        List<String> fallbacks = new ArrayList<>();
                        fallbacks.add("What actually happened?");
                        fallbacks.add("Explain this simply");
                        fallbacks.add("Who is affected?");
                        fallbacks.add("Why does this matter?");
                        fallbacks.add("What happens next?");
                        callback.onSuccess(fallbacks);
                    }
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    List<String> fallbacks = new ArrayList<>();
                    fallbacks.add("What actually happened?");
                    fallbacks.add("Explain this simply");
                    fallbacks.add("Who is affected?");
                    fallbacks.add("Why does this matter?");
                    fallbacks.add("What happens next?");
                    callback.onSuccess(fallbacks);
                });
            }
        });
    }

    /**
     * Answers a user question scoped strictly to the story's context.
     */
    public static void askStoryQuestion(Article article, String userQuestion, List<AskStoryModel.ChatMessage> history, AskStoryCallback callback) {
        executor.execute(() -> {
            try {
                StringBuilder historyContext = new StringBuilder();
                if (history != null) {
                    for (AskStoryModel.ChatMessage msg : history) {
                        String role = msg.getRole() == AskStoryModel.ChatMessage.ROLE_USER ? "User: " : "AI: ";
                        historyContext.append(role).append(msg.getText()).append("\n");
                    }
                }

                String sourceName = article.getSource() != null && article.getSource().getName() != null ? article.getSource().getName() : "Global News";
                String sourceUrl = article.getUrl() != null ? article.getUrl() : "";

                String systemInstruction = "System Rules for Global Insight 'Ask the Story':\n" +
                        "1. You are an evidence-grounded AI news assistant for Global Insight.\n" +
                        "2. Scope your knowledge to the given story below. Do NOT hallucinate unverified details.\n" +
                        "3. Distinguish FACT from REPORTED CLAIM and PREDICTION. Use language like 'The report states...', 'According to " + sourceName + "...'.\n" +
                        "4. If the question is outside the scope of this news story, politely say: \"That is outside the scope of this news story, but I can help you explore details related to this event.\"\n" +
                        "5. Format your response cleanly with bullet points if helpful.\n" +
                        "6. Include source citation references where applicable using [1], [2].\n\n" +
                        "STORY CONTEXT:\n" +
                        "Title: " + article.getTitle() + "\n" +
                        "Source: " + sourceName + "\n" +
                        "URL: " + sourceUrl + "\n" +
                        "Content/Summary: " + (article.getDescription() != null ? article.getDescription() : "") + "\n\n" +
                        "CONVERSATION HISTORY:\n" + historyContext.toString() + "\n" +
                        "USER QUESTION: " + userQuestion;

                String responseText = callGeminiApi(systemInstruction);
                String cleanAnswer = parseGeminiResponseText(responseText);

                List<AskStoryModel.Citation> citations = new ArrayList<>();
                if (sourceUrl != null && !sourceUrl.isEmpty()) {
                    citations.add(new AskStoryModel.Citation(sourceName, sourceUrl, article.getTitle(), "Primary Source"));
                }

                List<String> followUps = new ArrayList<>();
                followUps.add("Does this affect consumers?");
                followUps.add("What are alternative perspectives?");

                mainHandler.post(() -> callback.onSuccess(cleanAnswer, citations, followUps));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("Unable to connect to AI server. Please check internet connection or API key."));
            }
        });
    }

    private static String callGeminiApi(String prompt) throws Exception {
        String urlString = GEMINI_URL + GEMINI_API_KEY;
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; utf-8");
        conn.setDoOutput(true);
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(20000);

        // Build Request JSON
        JsonObject requestBody = new JsonObject();
        JsonArray contents = new JsonArray();
        JsonObject contentObj = new JsonObject();
        JsonArray parts = new JsonArray();
        JsonObject partObj = new JsonObject();
        partObj.addProperty("text", prompt);
        parts.add(partObj);
        contentObj.add("parts", parts);
        contents.add(contentObj);
        requestBody.add("contents", contents);

        byte[] input = gson.toJson(requestBody).getBytes(StandardCharsets.UTF_8);
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
        String responseLine;
        while ((responseLine = br.readLine()) != null) {
            response.append(responseLine.trim());
        }
        return response.toString();
    }

    private static String parseGeminiResponseText(String responseJson) {
        try {
            JsonObject jsonObject = new JsonParser().parse(responseJson).getAsJsonObject();
            JsonArray candidates = jsonObject.getAsJsonArray("candidates");
            if (candidates != null && candidates.size() > 0) {
                JsonObject content = candidates.get(0).getAsJsonObject().getAsJsonObject("content");
                JsonArray parts = content.getAsJsonArray("parts");
                if (parts != null && parts.size() > 0) {
                    return parts.get(0).getAsJsonObject().get("text").getAsString().trim();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "I couldn't process this story question right now. Please try again.";
    }

    private static List<String> parseSuggestedQuestions(String responseJson) {
        try {
            String rawText = parseGeminiResponseText(responseJson);
            if (rawText.contains("[") && rawText.contains("]")) {
                int start = rawText.indexOf("[");
                int end = rawText.lastIndexOf("]") + 1;
                String jsonSub = rawText.substring(start, end);
                JsonArray array = new JsonParser().parse(jsonSub).getAsJsonArray();
                List<String> list = new ArrayList<>();
                for (int i = 0; i < array.size(); i++) {
                    list.add(array.get(i).getAsString());
                }
                return list;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
