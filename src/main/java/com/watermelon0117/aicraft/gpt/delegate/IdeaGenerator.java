package com.watermelon0117.aicraft.gpt.delegate;

import com.watermelon0117.aicraft.common.AICraftingTableCommonConfigs;
import com.watermelon0117.aicraft.common.ItemStackArray;
import com.watermelon0117.aicraft.gpt.ItemIdeas;
import com.watermelon0117.aicraft.gpt.ProxyIdeaGenerator;
import com.watermelon0117.aicraft.gpt.opanai.GPTIdeaGenerator;

import java.util.concurrent.CompletableFuture;

public class IdeaGenerator {
    GPTIdeaGenerator gptIdeaGenerator = new GPTIdeaGenerator();
    ProxyIdeaGenerator proxyIdeaGenerator = new ProxyIdeaGenerator();

    public CompletableFuture<ItemIdeas> generate(ItemStackArray recipe, String lang, String user) {
        CompletableFuture<ItemIdeas> generated = AICraftingTableCommonConfigs.useOpenAI
                ? gptIdeaGenerator.generate(recipe, lang, user)
                : proxyIdeaGenerator.generate(recipe, lang, user);
        return generated.thenApply(ItemIdeas::sanitizeIds);
    }
}
