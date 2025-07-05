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
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ProxyImageClient {
    private static final URI ENDPOINT = URI.create("https://api.aicraftingtable.com/image");

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final Gson gson = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();

    public CompletableFuture<byte[]> generateAsync(String prompt, String size, String background, String moderation, String quality, String user) {
        Request body = new Request(prompt, "gpt-image-1", 1, size, background, moderation, quality, user);
        String json = gson.toJson(body);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(ENDPOINT)
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(100))
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        return http.sendAsync(req, HttpResponse.BodyHandlers.ofString())
                .thenCompose(resp -> {
                    if (resp.statusCode() != 200)
                        return CompletableFuture.failedFuture(
                                new RuntimeException("OpenAI error " + resp.statusCode() + ": " + resp.body()));

                    Response parsed = gson.fromJson(resp.body(), Response.class);
                    Data d = parsed.data.get(0);          // n == 1
                    byte[] bytes = Base64.getDecoder().decode(d.b64_json);
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
                           String quality,
                           String user) {
    }

    private static final class Response {
        List<Data> data;
    }

    private static final class Data {
        String b64_json;
    }
}
