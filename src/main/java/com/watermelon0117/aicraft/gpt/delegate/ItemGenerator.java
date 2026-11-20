package com.watermelon0117.aicraft.gpt.delegate;

import com.watermelon0117.aicraft.common.AICraftingTableCommonConfigs;
import com.watermelon0117.aicraft.common.ItemStackArray;
import com.watermelon0117.aicraft.gpt.GeneratedItem;
import com.watermelon0117.aicraft.gpt.GeneratedResult;
import com.watermelon0117.aicraft.gpt.ProxyItemGenerator;
import com.watermelon0117.aicraft.gpt.openai.GPTBlockGenerator;
import com.watermelon0117.aicraft.gpt.openai.GPTItemGenerator;

import java.util.Base64;
import java.util.concurrent.CompletableFuture;

public class ItemGenerator {
    GPTItemGenerator gptItemGenerator = new GPTItemGenerator();
    GPTBlockGenerator gptBlockGenerator = new GPTBlockGenerator();
    ProxyItemGenerator proxyItemGenerator = new ProxyItemGenerator();

    /** An item or a block of any shape, whichever the judge says the name is. */
    public CompletableFuture<GeneratedResult> generate(String id, String name, ItemStackArray recipe, String user) {
        return AICraftingTableCommonConfigs.useOpenAI ? blockOrItemWithOwnKey(id, name, recipe, user) : proxyItemGenerator.generate(id, name, recipe, user);
    }

    /** An item whatever the name sounds like. */
    public CompletableFuture<GeneratedResult> generateItem(String id, String name, ItemStackArray recipe, String user) {
        return AICraftingTableCommonConfigs.useOpenAI ? generateWithOwnKey(id, name, recipe, user) : proxyItemGenerator.generateItem(id, name, recipe, user);
    }

    private CompletableFuture<GeneratedResult> blockOrItemWithOwnKey(String id, String name, ItemStackArray recipe, String user) {
        return gptBlockGenerator.generate(id, recipe.getDisplayNames()).thenCompose(block -> block.isPresent()
                ? CompletableFuture.<GeneratedResult>completedFuture(block.get())
                : generateWithOwnKey(id, name, recipe, user));
    }

    private CompletableFuture<GeneratedResult> generateWithOwnKey(String id, String name, ItemStackArray recipe, String user) {
        return gptItemGenerator.generate(id, recipe.getDisplayNames(), user).thenApply(res ->
                GeneratedItem.fromJson(res.json(), id, name, Base64.getDecoder().decode(res.b64Tex())));
    }
}
