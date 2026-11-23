package com.watermelon0117.aicraft.gpt;

import com.watermelon0117.aicraft.common.ItemStackArray;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ProxyItemGenerator {
    /** The server judges before it draws, for up to 60 seconds; an armor piece's worn picture is a second image, about 17 seconds more. */
    private static final Duration TIMEOUT = Duration.ofSeconds(180);

    /** Contract 3. Released mods call /textures/auto and /textures, which stay as they knew them. */
    private final JsonHttpClient<GenerateRequest, GeneratedResult.Response> client =
            new JsonHttpClient<>(JsonHttpClient.apiBase() + "/v3/generate", GeneratedResult.Response.class, TIMEOUT);

    /** An item or a block of any shape, as the judge decides. */
    public CompletableFuture<GeneratedResult> generate(String id, String name, ItemStackArray recipe, String user) {
        return client.send(new GenerateRequest(recipe.getDisplayNames(), id, "auto", JsonHttpClient.FEATURES), user)
                .thenApply(response -> GeneratedResult.fromResponse(response, id, name));
    }

    /** An item whatever the name sounds like, with no judge to wait for or pay for. */
    public CompletableFuture<GeneratedResult> generateItem(String id, String name, ItemStackArray recipe, String user) {
        return client.send(new GenerateRequest(recipe.getDisplayNames(), id, "item", JsonHttpClient.FEATURES), user)
                .thenApply(response -> GeneratedItem.fromResponse(response, id, name));
    }

    /** @param mode "auto" lets the server's judge decide between item and block, "item" makes an item */
    public record GenerateRequest(String[] craftingGrid, String id, String mode, List<String> features) {
    }
}
