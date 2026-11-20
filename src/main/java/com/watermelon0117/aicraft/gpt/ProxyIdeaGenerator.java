package com.watermelon0117.aicraft.gpt;

import com.watermelon0117.aicraft.common.ItemStackArray;

import java.util.concurrent.CompletableFuture;

public class ProxyIdeaGenerator {
    JsonHttpClient<IdeaRequest, ItemIdeas> client = new JsonHttpClient<>(JsonHttpClient.apiBase() + "/ideas", ItemIdeas.class);

    public CompletableFuture<ItemIdeas> generate(ItemStackArray recipe, String lang, String user) {
        IdeaRequest request = new IdeaRequest(recipe.getDisplayNames(), lang, new String[]{}, true);
        return client.send(request, user);
    }

    /** @param blockShapes also hint at slab, stairs, fence and wall shapes; only a mod that can place those blocks sends it */
    public record IdeaRequest(String[] craftingGrid, String lang, String[] banList, boolean blockShapes) {
    }
}
