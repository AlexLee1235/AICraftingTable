package com.watermelon0117.aicraft.gpt;

import java.util.concurrent.CompletableFuture;

public class AIChatClient {
    ProxyChatClient client;
    public AIChatClient(String model, double temperature, int maxTokens, String systemMessage, String response_format){
        client=new ProxyChatClient(model,temperature,maxTokens,systemMessage,response_format);
    }
    public CompletableFuture<String> chat(String message) {
        return client.chat(message);
    }
}
