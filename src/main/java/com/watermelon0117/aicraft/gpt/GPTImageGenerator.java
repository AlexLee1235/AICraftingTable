package com.watermelon0117.aicraft.gpt;

import java.util.concurrent.CompletableFuture;

public class GPTImageGenerator {
    OpenAIImageClient imgClient = new OpenAIImageClient("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");
    public CompletableFuture<byte[]> generateItem(String name, String[] recipe) {
        //A 16x16 pixel art depiction of a " + name + " with clearly separated background color
        return imgClient.generateAsync("A 8x8 iconic 2D pixel art of a Minecraft " + name + " item with clearly separated background color",
                "1024x1024", "opaque", "low", "medium");
    }
}
