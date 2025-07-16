package com.watermelon0117.aicraft.gpt.delegate;

import com.watermelon0117.aicraft.common.AICraftingTableCommonConfigs;
import com.watermelon0117.aicraft.common.ItemStackArray;
import com.watermelon0117.aicraft.gpt.GeneratedItem;
import com.watermelon0117.aicraft.gpt.ItemIdeas;
import com.watermelon0117.aicraft.gpt.ProxyIdeaGenerator;
import com.watermelon0117.aicraft.gpt.ProxyItemGenerator;
import com.watermelon0117.aicraft.gpt.opanai.GPTIdeaGenerator;
import com.watermelon0117.aicraft.gpt.opanai.GPTItemGenerator4;

import java.util.concurrent.CompletableFuture;

public class ItemGenerator {
    GPTItemGenerator4 gptItemGenerator = new GPTItemGenerator4();
    ProxyItemGenerator proxyItemGenerator = new ProxyItemGenerator();

    public CompletableFuture<GeneratedItem> generate(String id, String name, ItemStackArray recipe, String user)  {
        if (AICraftingTableCommonConfigs.useOpenAI)
            return gptItemGenerator.generate(id, recipe.getDisplayNames(), user);
        else
            return proxyItemGenerator.generate(id, name, recipe, user);
    }
}
