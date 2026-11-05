package com.watermelon0117.aicraft.gpt;

import com.watermelon0117.aicraft.common.ItemStackArray;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public class ProxyItemGenerator {
    /** The server judges before it draws: up to 60 seconds, and a failed judge still goes on to make an item. */
    private static final Duration TIMEOUT = Duration.ofSeconds(180);

    /** Answers an item or a block of any shape. Released mods call /textures, which answers only items. */
    private final JsonHttpClient<ItemRequest, GeneratedResult.Response> client =
            new JsonHttpClient<>(JsonHttpClient.apiBase() + "/textures/auto", GeneratedResult.Response.class, TIMEOUT);

    public CompletableFuture<GeneratedResult> generate(String id, String name, ItemStackArray recipe, String user) {
        return client.send(new ItemRequest(recipe.getDisplayNames(), id, name), user)
                .thenApply(response -> GeneratedResult.fromResponse(response, id, name));
    }

    public record ItemRequest(String[] craftingGrid, String id, String name) {
    }
}
