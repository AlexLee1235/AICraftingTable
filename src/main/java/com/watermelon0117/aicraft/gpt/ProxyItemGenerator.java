package com.watermelon0117.aicraft.gpt;

import com.watermelon0117.aicraft.common.ItemStackArray;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public class ProxyItemGenerator {
    /** The server judges before it draws, for up to 60 seconds. */
    private static final Duration TIMEOUT = Duration.ofSeconds(180);

    /** Answers an item or a block of any shape. Released mods call /textures, which answers only items. */
    private final JsonHttpClient<ItemRequest, GeneratedResult.Response> client =
            new JsonHttpClient<>(JsonHttpClient.apiBase() + "/textures/auto", GeneratedResult.Response.class, TIMEOUT);
    /** An armor piece's worn picture is a second image after the item's, about 17 seconds more. */
    private final JsonHttpClient<ItemRequest, GeneratedResult.Response> itemClient =
            new JsonHttpClient<>(JsonHttpClient.apiBase() + "/textures", GeneratedResult.Response.class, TIMEOUT);

    public CompletableFuture<GeneratedResult> generate(String id, String name, ItemStackArray recipe, String user) {
        return client.send(new ItemRequest(recipe.getDisplayNames(), id, name, true), user)
                .thenApply(response -> GeneratedResult.fromResponse(response, id, name));
    }

    /** An item whatever the name sounds like, with no judge to wait for or pay for. */
    public CompletableFuture<GeneratedResult> generateItem(String id, String name, ItemStackArray recipe, String user) {
        return itemClient.send(new ItemRequest(recipe.getDisplayNames(), id, name, true), user)
                .thenApply(response -> GeneratedItem.fromResponse(response, id, name));
    }

    /** @param armor this mod wears armor: the server then asks for the slot and draws the worn picture */
    public record ItemRequest(String[] craftingGrid, String id, String name, boolean armor) {
    }
}
