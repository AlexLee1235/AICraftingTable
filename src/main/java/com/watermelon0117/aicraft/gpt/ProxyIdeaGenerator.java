package com.watermelon0117.aicraft.gpt;

import com.watermelon0117.aicraft.common.ItemStackArray;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ProxyIdeaGenerator {
    /** Contract 3: the server asks for blocks too, since every mod that calls it can place them. */
    JsonHttpClient<IdeaRequest, ItemIdeas> client = new JsonHttpClient<>(JsonHttpClient.apiBase() + "/v3/ideas", ItemIdeas.class);

    public CompletableFuture<ItemIdeas> generate(ItemStackArray recipe, String lang, String user) {
        return client.send(new IdeaRequest(recipe.getDisplayNames(), lang, JsonHttpClient.FEATURES), user);
    }

    public record IdeaRequest(String[] craftingGrid, String lang, List<String> features) {
    }
}
