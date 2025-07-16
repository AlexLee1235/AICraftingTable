package com.watermelon0117.aicraft.gpt;

import com.watermelon0117.aicraft.common.ItemStackArray;
import com.watermelon0117.aicraft.gpt.opanai.GPTIdeaGenerator;

import java.util.concurrent.CompletableFuture;

public class LocalIdeaGenerator {
    GPTIdeaGenerator generator=new GPTIdeaGenerator();
    public CompletableFuture<ItemIdeas> generate(ItemStackArray recipe, String lang, String user) {
        return generator.generate(recipe, lang, user);
    }
}
