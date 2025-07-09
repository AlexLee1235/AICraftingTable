package com.watermelon0117.aicraft.gpt;

import java.util.concurrent.CompletableFuture;

public class GPTImageGenerator {
    AIImageClient imgClient = new AIImageClient();
    public CompletableFuture<byte[]> generateItem(String name, String[] recipe, String other, String user) {
        String prompt = "A 8x8 iconic 2D pixel art of a Minecraft " + name + " item with clearly separated background color. No blocks. " + other;
        System.out.println(prompt);
        return imgClient.generateAsync(prompt, "1024x1024", "opaque", "low", "low", user);
    }
}
