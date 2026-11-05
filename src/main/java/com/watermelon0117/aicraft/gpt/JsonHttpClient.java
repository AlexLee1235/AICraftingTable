package com.watermelon0117.aicraft.gpt;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.watermelon0117.aicraft.gpt.opanai.OpenAIChatClient;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class JsonHttpClient<T, U> {
    /** AICRAFT_API_KEY lets a development client use a local gateway, which has its own key. */
    private static final String API_KEY = System.getenv().getOrDefault("AICRAFT_API_KEY", "MY_SUPER_SECRET_KEY");
    private static final String DEFAULT_API_BASE = "https://mod.aicraftingtable.com";
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(100);

    private final URI endpoint;
    private final Class<U> responseClass; // store Class<U> for deserialization
    private final Duration timeout;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final Gson gson = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.IDENTITY).create();

    public JsonHttpClient(String uri, Class<U> responseClass) {
        this(uri, responseClass, DEFAULT_TIMEOUT);
    }

    public JsonHttpClient(String uri, Class<U> responseClass, Duration timeout) {
        this.endpoint = URI.create(uri);
        this.responseClass = responseClass;
        this.timeout = timeout;
    }

    /** AICRAFT_API_BASE points a development client at a local gateway. */
    public static String apiBase() {
        String base = System.getenv("AICRAFT_API_BASE");
        return base == null || base.isBlank() ? DEFAULT_API_BASE : base;
    }

    public CompletableFuture<U> send(T request, String user) {
        String json = gson.toJson(request);
        HttpRequest req = HttpRequest.newBuilder()
                .uri(endpoint)
                .header("Authorization", "Bearer " + API_KEY)
                .header("X-User-Id", user)
                .header("Content-Type", "application/json")
                .timeout(timeout)
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        return http.sendAsync(req, HttpResponse.BodyHandlers.ofString())
                .thenCompose(resp -> {
                    if (resp.statusCode() != 200) {
                        String msg = resp.body().replace("\\n", "\n").replace("\\", "");
                        if (msg.isEmpty()) msg = "Unknown server error";
                        return CompletableFuture.failedFuture(new RuntimeException(msg));
                    }

                    U chat = gson.fromJson(resp.body(), responseClass);
                    return CompletableFuture.completedFuture(chat);
                });
    }
}