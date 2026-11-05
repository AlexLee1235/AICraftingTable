package com.watermelon0117.aicraft.gpt;

import java.util.concurrent.CompletableFuture;

public class ProxyBlockGenerator {
    private static final String DEFAULT_API_BASE = "https://mod.aicraftingtable.com";

    /** The route that can answer every model. If it judges a name to be an item, it has already paid for that item's image by the time the answer is refused here. */
    private final JsonHttpClient<BlockRequest, GeneratedBlock.Response> client =
            new JsonHttpClient<>(apiBase() + "/textures/auto", GeneratedBlock.Response.class);

    /** AICRAFT_API_BASE points a development client at a local gateway. */
    private static String apiBase() {
        String base = System.getenv("AICRAFT_API_BASE");
        return base == null || base.isBlank() ? DEFAULT_API_BASE : base;
    }

    public CompletableFuture<GeneratedBlock> generate(String id, String[] craftingGrid, String user) {
        return client.send(new BlockRequest(id, craftingGrid), user).thenApply(GeneratedBlock::fromResponse);
    }

    public record BlockRequest(String id, String[] craftingGrid) {
    }
}
