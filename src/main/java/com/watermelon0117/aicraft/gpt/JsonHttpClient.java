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
    private final URI endpoint;
    private final Class<U> responseClass; // store Class<U> for deserialization
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final Gson gson = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.IDENTITY).create();

    public JsonHttpClient(String uri, Class<U> responseClass) {
        this.endpoint = URI.create(uri);
        this.responseClass = responseClass;
    }

    public CompletableFuture<U> send(T request, String user) {
        String json = gson.toJson(request);
        HttpRequest req = HttpRequest.newBuilder()
                .uri(endpoint)
                .header("Authorization", "Bearer " + "MY_SUPER_SECRET_KEY")
                .header("X-User-Id", user)
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(100))
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        return http.sendAsync(req, HttpResponse.BodyHandlers.ofString())
                .thenCompose(resp -> {
                    if (resp.statusCode() != 200) {
                        String msg=resp.body().replace("\\n", "\n").replace("\\", "");
                        return CompletableFuture.failedFuture(new RuntimeException(msg));
                    }

                    U chat = gson.fromJson(resp.body(), responseClass);
                    return CompletableFuture.completedFuture(chat);
                });
    }
}