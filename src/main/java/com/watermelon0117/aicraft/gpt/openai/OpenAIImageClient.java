package com.watermelon0117.aicraft.gpt.openai;

import com.google.gson.*;
import com.google.gson.annotations.SerializedName;
import com.watermelon0117.aicraft.gpt.GenerationException;
import net.minecraft.network.chat.Component;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;


public final class OpenAIImageClient {
    private static final URI ENDPOINT = URI.create(OpenAIChatClient.apiBase() + "/v1/images/generations");
    private static final URI EDIT_ENDPOINT = URI.create(OpenAIChatClient.apiBase() + "/v1/images/edits");
    /** gpt-image-1 shuts down on 2026-10-23; the texture server uses the same model. */
    private static final String MODEL = "gpt-image-2.5-sunburst";
    public static String apiKey;
    private final HttpClient http;
    private final Gson gson;

    public OpenAIImageClient() {
        this.http   = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.gson   = new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .create();
    }

    /* ───────────────────────────────── PUBLIC API ───────────────────────────────── */


    public CompletableFuture<String> generateAsync(String prompt, String size, String background, String moderation, String quality, String outputFormat) {
        Request body = new Request(prompt, MODEL, 1, size, background, moderation, quality, outputFormat);
        String json  = gson.toJson(body);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(ENDPOINT)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(100))
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

                    Response parsed = gson.fromJson(resp.body(), Response.class);
                    Data     d      = parsed.data.get(0);          // n == 1
                    if (d.b64_json == null) {
                        return CompletableFuture.failedFuture(new GenerationException(Component.translatable("error.aicraft.openai_no_image")));
                    }
                    return CompletableFuture.completedFuture(d.b64_json);
                });
    }

    /**
     * The model painting on pictures it is given (the worn armor), as the texture server's editImage sends it. The body is
     * written by hand so the prompt's line breaks reach the model as they are, as in the armor experiment.
     *
     * @param images PNGs, sent in this order as image[]
     */
    public CompletableFuture<String> editAsync(String prompt, List<byte[]> images, String size, String background, String moderation, String quality, String outputFormat) {
        String boundary = "----aicraft" + Long.toHexString(System.nanoTime()) + Long.toHexString(Double.doubleToLongBits(Math.random()));
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("model", MODEL);
        fields.put("n", "1");
        fields.put("quality", quality);
        fields.put("background", background);
        fields.put("output_format", outputFormat);
        fields.put("moderation", moderation);
        fields.put("size", size);
        fields.put("prompt", prompt);
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        for (Map.Entry<String, String> field : fields.entrySet()) {
            body.writeBytes(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"" + field.getKey() + "\"\r\n\r\n"
                    + field.getValue() + "\r\n").getBytes(StandardCharsets.UTF_8));
        }
        for (int i = 0; i < images.size(); i++) {
            body.writeBytes(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"image[]\"; filename=\"image" + (i + 1)
                    + ".png\"\r\nContent-Type: image/png\r\n\r\n").getBytes(StandardCharsets.UTF_8));
            body.writeBytes(images.get(i));
            body.writeBytes("\r\n".getBytes(StandardCharsets.UTF_8));
        }
        body.writeBytes(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));

        HttpRequest req = HttpRequest.newBuilder()
                .uri(EDIT_ENDPOINT)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .timeout(Duration.ofSeconds(100))
                .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray()))
                .build();
        return http.sendAsync(req, HttpResponse.BodyHandlers.ofString())
                .exceptionally(error -> {
                    throw GenerationException.openAINoAnswer(error);
                })
                .thenCompose(this::firstImage);
    }

    private CompletableFuture<String> firstImage(HttpResponse<String> resp) {
        if (resp.statusCode() != 200) {
            return CompletableFuture.failedFuture(GenerationException.fromOpenAI(resp.statusCode(), resp.body()));
        }
        Response parsed = gson.fromJson(resp.body(), Response.class);
        Data d = parsed.data.get(0);          // n == 1
        if (d.b64_json == null) {
            return CompletableFuture.failedFuture(new GenerationException(Component.translatable("error.aicraft.openai_no_image")));
        }
        return CompletableFuture.completedFuture(d.b64_json);
    }

    /* ────────────────────────────── INTERNAL DTOs ─────────────────────────────── */

    private record Request(String prompt,
                           String model,
                           int n,
                           String size,
                           String background,
                           String moderation,
                           String quality,
                           String output_format) {}

    private static final class Response { List<Data> data; }
    private static final class Data     { String b64_json; }
}
