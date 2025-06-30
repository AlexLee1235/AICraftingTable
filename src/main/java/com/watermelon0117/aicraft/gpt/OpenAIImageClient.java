package com.watermelon0117.aicraft.gpt;

import com.google.gson.*;
import com.google.gson.annotations.SerializedName;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CompletableFuture;


public final class OpenAIImageClient {
    private static final URI ENDPOINT = URI.create("https://api.openai.com/v1/images/generations");
    public static String apiKey;
    private final HttpClient http;
    private final Gson gson;

    public OpenAIImageClient() {
        this.http   = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(100))
                .build();
        this.gson   = new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .create();
    }

    /* ───────────────────────────────── PUBLIC API ───────────────────────────────── */


    public CompletableFuture<byte[]> generateAsync(String prompt, String size, String background, String moderation, String quality) {
        Request body = new Request(prompt, "gpt-image-1", 1, size, background, moderation, quality);
        String json  = gson.toJson(body);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(ENDPOINT)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(100))
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

                    Response parsed = gson.fromJson(resp.body(), Response.class);
                    Data     d      = parsed.data.get(0);          // n == 1
                    byte[]   bytes  = Base64.getDecoder().decode(d.b64Json);
                    return CompletableFuture.completedFuture(bytes);
                });
    }

    /* ────────────────────────────── INTERNAL DTOs ─────────────────────────────── */

    private record Request(String prompt,
                           String model,
                           int n,
                           String size,
                           String background,
                           String moderation,
                           String quality) {}

    private static final class Response { List<Data> data; }
    private static final class Data     { @SerializedName("b64_json") String b64Json; }
}
