package com.watermelon0117.aicraft.gpt.delegate;

import com.watermelon0117.aicraft.common.AICraftingTableCommonConfigs;
import com.watermelon0117.aicraft.common.ItemStackArray;
import com.watermelon0117.aicraft.gpt.GeneratedItem;
import com.watermelon0117.aicraft.gpt.GeneratedResult;
import com.watermelon0117.aicraft.gpt.ProxyItemGenerator;
import com.watermelon0117.aicraft.gpt.opanai.GPTItemGenerator;

import java.util.Base64;
import java.util.concurrent.CompletableFuture;

public class ItemGenerator {
    GPTItemGenerator gptItemGenerator = new GPTItemGenerator();
    ProxyItemGenerator proxyItemGenerator = new ProxyItemGenerator();

    /** Only the texture server can judge a name to be a block, so an own OpenAI key always makes an item. */
    public CompletableFuture<GeneratedResult> generate(String id, String name, ItemStackArray recipe, String user) {
        if (AICraftingTableCommonConfigs.useOpenAI) {
            return gptItemGenerator.generate(id, recipe.getDisplayNames(), user).thenApply(res ->
                    GeneratedItem.fromJson(res.json(), id, name, Base64.getDecoder().decode(res.b64Tex())));
        } else
            return proxyItemGenerator.generate(id, name, recipe, user);
    }
}
