package com.watermelon0117.aicraft.gpt;

import com.google.gson.*;
import com.google.gson.annotations.SerializedName;
import com.watermelon0117.aicraft.common.AICraftingTableCommonConfigs;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class OpenAIChatClient {
    private static final URI CHAT_URI = URI.create("https://api.openai.com/v1/chat/completions");
    public static String apiKey;
    private final HttpClient http;
    private final Gson gson;
    public final String model;
    public final double temperature;
    public final int maxTokens;
    public final String systemMessage;
    public final String response_format;
    public OpenAIChatClient(String model, double temperature, int maxTokens, String systemMessage, String response_format) {
        this.model = model;
        this.temperature = temperature;
        this.maxTokens = maxTokens;
        this.systemMessage = systemMessage;
        this.response_format = response_format;

        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        this.gson = new GsonBuilder()
                // map Java camelCase ↔︎ JSON snake_case automatically
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .create();
    }

    public CompletableFuture<String> chat(String message) {
        ArrayList<Message> list=new ArrayList<>();
        list.add(new Message("system", systemMessage));
        list.add(new Message("user", message));
        return chat(list);
    }
    private CompletableFuture<String> chat(List<Message> messages) {
        ChatRequest requestBody = new ChatRequest(model, temperature, maxTokens, messages, response_format);

        String json  = gson.toJson(requestBody);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(CHAT_URI)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        return http.sendAsync(req, HttpResponse.BodyHandlers.ofString())
                .thenCompose(resp -> {
                    if (resp.statusCode() != 200) {
                        return CompletableFuture.failedFuture(
                                new RuntimeException("OpenAI error " +
                                        resp.statusCode() + ": " +
                                        resp.body()));
                    }

                    ChatResponse chat = gson.fromJson(resp.body(), ChatResponse.class);
                    return CompletableFuture.completedFuture(chat.choices.get(0).message.content);
                });
    }

    /* ----------  Request / Response POJOs ---------- */

    public static final class Message {
        public final String role;     // "system" | "user" | "assistant"
        public final String content;

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }

    private static final class ChatRequest {
        final String model;
        final double temperature;
        @SerializedName("max_tokens") final int maxTokens;
        final List<Message> messages;
        final ResponseFormat response_format;

        ChatRequest(String model, double temperature, int maxTokens, List<Message> messages, String response_format) {
            this.model = model;
            this.temperature = temperature;
            this.maxTokens = maxTokens;
            this.messages = messages;
            this.response_format=new ResponseFormat(response_format);
        }
    }
    private static final class ResponseFormat{
        String type;
        ResponseFormat(String type){
            this.type=type;
        }
    }
    private static final class ChatResponse {
        List<Choice> choices;

        static final class Choice {
            ChatMessage message;
        }
        static final class ChatMessage {
            String role;
            String content;
        }
    }
}
