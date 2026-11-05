package com.watermelon0117.aicraft.gpt;

import java.util.concurrent.CompletableFuture;

public class ProxyBlockGenerator {
    private static final String DEFAULT_API_BASE = "https://mod.aicraftingtable.com";

    private final JsonHttpClient<BlockRequest, BlockResponse> client =
            new JsonHttpClient<>(apiBase() + "/textures/block", BlockResponse.class);

    /** AICRAFT_API_BASE points a development client at a local gateway. */
    private static String apiBase() {
        String base = System.getenv("AICRAFT_API_BASE");
        return base == null || base.isBlank() ? DEFAULT_API_BASE : base;
    }

    public CompletableFuture<GeneratedBlock> generate(String id, String[] craftingGrid, String user) {
        return client.send(new BlockRequest(id, craftingGrid), user).thenApply(response ->
                GeneratedBlock.fromResponse(response.kind, response.tool, response.hardness, response.light, response.top, response.front, response.side));
    }

    public record BlockRequest(String id, String[] craftingGrid) {
    }

    public static class BlockResponse {
        String kind;
        String tool;
        String hardness;
        String light;
        String top;
        String front;
        String side;
    }
}
