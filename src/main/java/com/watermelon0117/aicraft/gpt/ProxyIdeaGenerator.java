package com.watermelon0117.aicraft.gpt;

import com.watermelon0117.aicraft.common.ItemStackArray;

import java.util.concurrent.CompletableFuture;

public class ProxyIdeaGenerator {
    JsonHttpClient<IdeaRequest, ItemIdeas> client = new JsonHttpClient<>("localhost:8080/ideas", ItemIdeas.class);

    public CompletableFuture<ItemIdeas> generate(ItemStackArray recipe, String lang, String user) {
        IdeaRequest request = new IdeaRequest(recipe.getDisplayNames(), lang, new String[]{});
        return client.send(request);
    }

    public record IdeaRequest(String[] craftingGrid, String lang, String[] banList) {
    }
}
