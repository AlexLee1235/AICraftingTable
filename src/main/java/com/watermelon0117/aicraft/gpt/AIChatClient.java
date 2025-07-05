package com.watermelon0117.aicraft.gpt;

import java.util.concurrent.CompletableFuture;

public class AIChatClient {
    public static boolean useOpenAI;
    ProxyChatClient proxyClient;
    OpenAIChatClient openAIClient;

    public AIChatClient(String model, double temperature, int maxTokens, String systemMessage, String response_format) {
        if (useOpenAI)
            openAIClient = new OpenAIChatClient(model, temperature, maxTokens, systemMessage, response_format);
        else
            proxyClient = new ProxyChatClient(model, temperature, maxTokens, systemMessage, response_format);
    }

    public CompletableFuture<String> chat(String message, String metadata, String user) {
        if (useOpenAI)
            return openAIClient.chat(message);
        else
            return proxyClient.chat(message, metadata, user);
    }
}
