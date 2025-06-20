package com.watermelon0117.aicraft.gpt;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.watermelon0117.aicraft.Recipe;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public abstract class BaseGPTItemGeneratorwParser {
    String inst2="You will be given a block of text that includes 3 original Minecraft-style item names, possibly along with descriptions or reasoning.\n" +
            "\n" +
            "Your job is to extract just the item names and return them as a JSON format:\n" +
            "{\"items\":[\"item1\",\"item2\",\"item3\"],\"error\":false}\n" +
            "if there's no items in text, set error to true\n" +
            "\n" +
            "Only include the names. Do not include descriptions, numbering, or extra formatting.\n" +
            "\n" +
            "Example input:\n" +
            "\"This pattern forms a U-shape. Here are three item ideas:\n" +
            "1. Iron Crucible – Used for smelting alloys.\n" +
            "2. Ore Hopper – Collects ores.\n" +
            "3. Molten Basin – Stores molten metal.\"\n" +
            "\n" +
            "Output:\n" +
            "{\"items\":[\"Iron Crucible\", \"Ore Hopper\", \"Molten Basin\"],\"error\":false}\n" +
            "\n" +
            "Now extract from this:\n";
    private final Gson gson;
    private final OpenAIHttpClient client;
    private final OpenAIHttpClient extractor = new OpenAIHttpClient("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",
            "gpt-4.1",  //gpt-4o gpt-4.1
            0.0,
            1024,
            "You are a helpful assistant that extracts structured data from text.",
            "json_object");
    public BaseGPTItemGeneratorwParser(String sysMsg){
        this.gson = new GsonBuilder()
                // map Java camelCase ↔︎ JSON snake_case automatically
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .create();
        client=new OpenAIHttpClient("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",
                "gpt-4o",  //gpt-4o gpt-4.1
                0.0,
                2048,
                sysMsg,
                "text");
    }
    public CompletableFuture<String[]> generate(Recipe recipe) {
        return generate(buildPrompt(recipe));
    }
    protected abstract String buildPrompt(Recipe recipe);
    public CompletableFuture<String[]> generate(String prompt) {
        return client.chat(prompt.toString()).thenCompose(rawResult->{
            System.out.println(rawResult);
            return extractor.chat(inst2+rawResult);
        }).thenCompose(jsonResult->{
            System.out.println(jsonResult);
            ItemResult result = gson.fromJson(jsonResult, ItemResult.class);
            if(result.error)
                throw new CompletionException(new IllegalStateException("Json extractor returned error"));
            return CompletableFuture.completedFuture(new String[]{result.items.get(0), result.items.get(1), result.items.get(2)});
        });
    }
    private static final class ItemResult{
        boolean error;
        List<String> items;
    }
}
