package com.watermelon0117.aicraft.gpt;

import java.util.concurrent.CompletableFuture;

public class GPTImageGenerator2 {
    AIImageClient imgClient = new AIImageClient();
    public CompletableFuture<byte[]> generateItem(String name, String[] recipe, String other) {
        //A 16x16 pixel art depiction of a " + name + " with clearly separated background color
        return imgClient.generateAsync("A 8x8 iconic 2D pixel art of a Minecraft " + name + " item with clearly separated background color. "+other,
                "1024x1024", "opaque", "low", "low");
    }
}
