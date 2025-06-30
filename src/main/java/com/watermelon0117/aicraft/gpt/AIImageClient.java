package com.watermelon0117.aicraft.gpt;

import com.watermelon0117.aicraft.common.AICraftingTableCommonConfigs;

import java.util.concurrent.CompletableFuture;

public class AIImageClient {
    public static boolean useOpenAI;
    ProxyImageClient proxyClient;
    OpenAIImageClient openAIClient;

    public AIImageClient() {
        if (useOpenAI)
            openAIClient = new OpenAIImageClient();
        else
            proxyClient = new ProxyImageClient();
    }

    public CompletableFuture<byte[]> generateAsync(String prompt, String size, String background, String moderation, String quality) {
        if (useOpenAI)
            return openAIClient.generateAsync(prompt, size, background, moderation, quality);
        else
            return proxyClient.generateAsync(prompt, size, background, moderation, quality);
    }
}
