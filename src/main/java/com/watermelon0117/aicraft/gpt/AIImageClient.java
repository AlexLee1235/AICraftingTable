package com.watermelon0117.aicraft.gpt;

import java.util.concurrent.CompletableFuture;

public class AIImageClient {
    ProxyImageClient client;
    public AIImageClient(){
        client=new ProxyImageClient();
    }
    public CompletableFuture<byte[]> generateAsync(String prompt, String size, String background, String moderation, String quality) {
        return client.generateAsync(prompt, size, background, moderation, quality);
    }
}
