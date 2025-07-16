package com.watermelon0117.aicraft.gpt.opanai;

import java.util.concurrent.CompletableFuture;

public class GPTImageGenerator {
    OpenAIImageClient imgClient = new OpenAIImageClient();
    public CompletableFuture<String> generateItem(String name, String desc) {
        String prompt = "A 8x8 iconic 2D pixel art of a Minecraft " + name + " item with clearly separated background color. No blocks. " + desc;
        return imgClient.generateAsync(prompt, "1024x1024", "opaque", "low", "low");
    }
}
