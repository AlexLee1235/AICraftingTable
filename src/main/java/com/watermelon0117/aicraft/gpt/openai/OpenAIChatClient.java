package com.watermelon0117.aicraft.gpt.openai;

import com.google.gson.*;
import com.google.gson.annotations.SerializedName;
import com.watermelon0117.aicraft.common.AICraftingTableCommonConfigs;
import com.watermelon0117.aicraft.gpt.GenerationException;
import net.minecraft.network.chat.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class OpenAIChatClient {
    private static final URI CHAT_URI = URI.create(apiBase() + "/v1/chat/completions");
    private static final Duration CHAT_TIMEOUT = Duration.ofSeconds(30);
    public static String apiKey;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final Gson gson = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();
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
    }

    /** AICRAFT_OPENAI_BASE points a development client at a local stand-in. */
    static String apiBase() {
        String base = System.getenv("AICRAFT_OPENAI_BASE");
        return base == null || base.isBlank() ? "https://api.openai.com" : base;
    }

    public CompletableFuture<String> chat(String message, String type, String user) {
        ArrayList<Message> list=new ArrayList<>();
        list.add(new Message("system", systemMessage));
        list.add(new Message("user", message));
        return chat(list);
    }

    /**
     * For a model that reasons first and answers in a strict schema, so it takes no temperature or system message and counts its
     * reasoning toward {@link #maxTokens}.
     *
     * @return the answer as a JSON string
     */
    public CompletableFuture<String> chatStructured(String schemaName, JsonObject schema, String message, Duration timeout) {
        JsonObject jsonSchema = new JsonObject();
        jsonSchema.addProperty("name", schemaName);
        jsonSchema.addProperty("strict", true);
        jsonSchema.add("schema", schema);
        JsonObject format = new JsonObject();
        format.addProperty("type", "json_schema");
        format.add("json_schema", jsonSchema);
        JsonObject body = new JsonObject();
        body.addProperty("model", model);
        body.addProperty("max_completion_tokens", maxTokens);
        body.add("messages", gson.toJsonTree(List.of(new Message("user", message))));
        body.add("response_format", format);
        return post(gson.toJson(body), timeout).thenApply(chat -> {
            ChatResponse.ChatMessage answer = chat.choices.get(0).message;
            if (answer.content == null) {
                throw new GenerationException(answer.refusal == null ? Component.translatable("error.aicraft.openai_no_answer") : Component.translatable("error.aicraft.openai_error", answer.refusal));
            }
            return answer.content;
        });
    }

    private CompletableFuture<String> chat(List<Message> messages) {
        ChatRequest requestBody = new ChatRequest(model, temperature, maxTokens, messages, response_format);
        return post(gson.toJson(requestBody), CHAT_TIMEOUT).thenApply(chat -> chat.choices.get(0).message.content);
    }

    private CompletableFuture<ChatResponse> post(String json, Duration timeout) {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(CHAT_URI)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .timeout(timeout)
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        return http.sendAsync(req, HttpResponse.BodyHandlers.ofString())
                .exceptionally(error -> {
                    throw GenerationException.openAINoAnswer(error);
                })
                .thenCompose(resp -> {
                    if (resp.statusCode() != 200) {
                        return CompletableFuture.failedFuture(GenerationException.fromOpenAI(resp.statusCode(), resp.body()));
                    }
                    return CompletableFuture.completedFuture(gson.fromJson(resp.body(), ChatResponse.class));
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
            String refusal;
        }
    }
}
