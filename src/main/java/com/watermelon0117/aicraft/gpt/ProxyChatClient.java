package com.watermelon0117.aicraft.gpt;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ProxyChatClient {
    private static final URI CHAT_URI = URI.create("https://aicraftingtableproxy-production.up.railway.app/chat");
    private static final URI TEST_URI = URI.create("https://aicraftingtableproxy-production.up.railway.app/");

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    ;
    private final Gson gson = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();
    ;
    public final String model;
    public final double temperature;
    public final int maxTokens;
    public final String systemMessage;
    public final String response_format;

    public ProxyChatClient(String model, double temperature, int maxTokens, String systemMessage, String response_format) {
        this.model = model;
        this.temperature = temperature;
        this.maxTokens = maxTokens;
        this.systemMessage = systemMessage;
        this.response_format = response_format;
    }

    public CompletableFuture<String> chat(String message, String metadata, String user) {
        ArrayList<Message> list = new ArrayList<>();
        list.add(new Message("system", systemMessage));
        list.add(new Message("user", message));
        return chat(list, metadata, user);
    }

    private CompletableFuture<String> chat(List<Message> messages, String metadata, String user) {
        ChatRequest requestBody = new ChatRequest(model, temperature, maxTokens, messages, response_format, metadata, user);
        String json = gson.toJson(requestBody);
        HttpRequest req = HttpRequest.newBuilder()
                .uri(CHAT_URI)
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

    public static CompletableFuture<String> testConnect() {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(TEST_URI)
                .GET()
                .build();
        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenCompose(response -> {
            int status = response.statusCode();
            if (status != 200)
                return CompletableFuture.failedFuture(new RuntimeException("HTTP error: " + status + ", body: " + response.body()));
            return CompletableFuture.completedFuture(response.body());
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
        final int max_tokens;
        final List<Message> messages;
        final ResponseFormat response_format;
        final MetaData metadata;
        final String user;

        ChatRequest(String model, double temperature, int max_tokens, List<Message> messages, String response_format, String metadata, String user) {
            this.model = model;
            this.temperature = temperature;
            this.max_tokens = max_tokens;
            this.messages = messages;
            this.response_format = new ResponseFormat(response_format);
            this.metadata = new MetaData(metadata);
            this.user = user;
        }
    }

    private static final class ResponseFormat {
        String type;

        ResponseFormat(String type) {
            this.type = type;
        }
    }

    private static final class MetaData {
        String data;

        MetaData(String data) {
            this.data = data;
        }
    }

    private static final class ChatResponse {
        List<ChatResponse.Choice> choices;

        static final class Choice {
            ChatResponse.ChatMessage message;
        }

        static final class ChatMessage {
            String role;
            String content;
        }
    }
}
