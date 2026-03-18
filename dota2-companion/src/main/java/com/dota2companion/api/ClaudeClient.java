package com.dota2companion.api;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import okhttp3.*;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public class ClaudeClient {

    private static final String BASE_URL = "https://api.anthropic.com/v1/messages";
    private static final String ANTHROPIC_VERSION = "2023-06-01";

    private final OkHttpClient client = new OkHttpClient();
    private final Gson gson = new Gson();

    private final String systemPrompt = """
            You are an expert Dota 2 coach. Analyze the match data and give direct,
            specific feedback: 3–5 mistakes with context, what the player did well,
            and one clear priority for the next game. Reference actual numbers.
            Отвечай строго на русском языке.
            """;

    public CompletableFuture<String> analyzeMatch(String model, String matchSummaryJson) {
        CompletableFuture<String> future = new CompletableFuture<>();

        String apiKey = System.getenv("ANTHROPIC_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            future.completeExceptionally(new IllegalStateException("ANTHROPIC_API_KEY is not set"));
            return future;
        }

        JsonObject body = new JsonObject();
        body.addProperty("model", model);
        body.addProperty("max_tokens", 700);
        body.addProperty("system", systemPrompt);

        JsonArray messages = new JsonArray();
        JsonObject userMessage = new JsonObject();
        userMessage.addProperty("role", "user");

        JsonArray contentArray = new JsonArray();
        JsonObject contentItem = new JsonObject();
        contentItem.addProperty("type", "text");
        contentItem.addProperty("text", "Вот данные матча в формате JSON:\n" + matchSummaryJson);
        contentArray.add(contentItem);

        userMessage.add("content", contentArray);
        messages.add(userMessage);
        body.add("messages", messages);

        RequestBody requestBody = RequestBody.create(
                gson.toJson(body),
                MediaType.parse("application/json")
        );

        Request request = new Request.Builder()
                .url(BASE_URL)
                .post(requestBody)
                .addHeader("x-api-key", apiKey)
                .addHeader("anthropic-version", ANTHROPIC_VERSION)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                future.completeExceptionally(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    future.completeExceptionally(new IOException("HTTP " + response.code()));
                    return;
                }
                String bodyString = response.body().string();
                JsonObject json = gson.fromJson(bodyString, JsonObject.class);
                JsonArray content = json.getAsJsonArray("content");
                if (content != null && content.size() > 0) {
                    JsonObject first = content.get(0).getAsJsonObject();
                    String text = first.get("text").getAsString();
                    future.complete(text);
                } else {
                    future.completeExceptionally(new IOException("Empty Claude response"));
                }
            }
        });

        return future;
    }
}

