package com.watermelon0117.aicraft.gpt;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.watermelon0117.aicraft.Recipe;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public abstract class BaseGPTItemGenerator {
    private final Gson gson;
    private final OpenAIHttpClient client;
    public BaseGPTItemGenerator(String sysMsg){
        this.gson = new GsonBuilder()
                // map Java camelCase ↔︎ JSON snake_case automatically
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .create();
        client = new OpenAIHttpClient("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",
                "gpt-4o",  //gpt-4o gpt-4.1
                0.0,
                1024,
                sysMsg,
                "json_object");
    }
    public CompletableFuture<String[]> generate(Recipe recipe) {
        return generate(buildPrompt(recipe));
    }
    protected abstract String buildPrompt(Recipe recipe);
    public CompletableFuture<String[]> generate(String prompt) {
        return client.chat(prompt).thenCompose(rawResult->{
            //System.out.println(rawResult);
            ItemResult result = gson.fromJson(rawResult, ItemResult.class);
            //System.out.println(result.reasoning);
            return CompletableFuture.completedFuture(new String[]{result.items.get(0).name, result.items.get(1).name, result.items.get(2).name});
        });
    }
    private static final class ItemResult{
        String reasoning;
        List<ItemResult.MyItems> items;

        static final class MyItems {
            String name;
        }
    }
}
