package com.watermelon0117.aicraft.gpt;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class BaseGPTIdeaGeneratorwParser {
    String inst2 = "You will be given a block of text that includes 3 original Minecraft-style item names, possibly along with descriptions or reasoning.\n" +
            "\n" +
            "Your job is to:\n" +
            "1. Extract just the item names (no descriptions, numbering, or extra formatting).\n" +
            "2. Translate each extracted name into [TARGET LANGUAGE].\n" +
            "3. Return the results in JSON format with two arrays:\n" +
            "\n" +
            "{\n" +
            "  \"items\": [\"original1\", \"original2\", \"original3\"],\n" +
            "  \"translated\": [\"translated1\", \"translated2\", \"translated3\"],\n" +
            "  \"error\": false\n" +
            "}\n" +
            "\n" +
            "If there are no items in the text, set \"error\" to true and leave both arrays as empty.\n" +
            "\n" +
            "Example input:\n" +
            "\"This pattern forms a U-shape. Here are three item ideas:\n" +
            "1. Iron Crucible – Used for smelting alloys.\n" +
            "2. Ore Hopper – Collects ores.\n" +
            "3. Molten Basin – Stores molten metal.\"\n" +
            "\n" +
            "Example output (translated to Chinese):\n" +
            "\n" +
            "{\n" +
            "  \"items\": [\"Iron Crucible\", \"Ore Hopper\", \"Molten Basin\"],\n" +
            "  \"translated\": [\"鐵坩堝\", \"礦石漏斗\", \"熔融盆\"],\n" +
            "  \"error\": false\n" +
            "}\n" +
            "\n" +
            "Now extract and translate from this:\n";
    private final Gson gson;
    private final AIChatClient client, extractor;

    public BaseGPTIdeaGeneratorwParser(String sysMsg, String model, double temp) {
        this.gson = new GsonBuilder()
                // map Java camelCase ↔︎ JSON snake_case automatically
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .create();
        client = new AIChatClient(
                model,  //gpt-4o gpt-4.1
                temp,
                2048,
                sysMsg,
                "text");
        extractor = new AIChatClient(
                "gpt-4.1",  //gpt-4o gpt-4.1
                0.0,
                1024,
                "You are a helpful assistant that extracts structured data and translate.",
                "json_object");
    }

    public CompletableFuture<ItemIdeas> generate(String prompt, String lang) {
        return client.chat(prompt).thenCompose(rawResult -> {
            System.out.println(rawResult);
            return extractor.chat(inst2 + "Target language: " + lang + "\n" + rawResult);
        }).thenApply(jsonResult -> {
            System.out.println(jsonResult);
            ItemResult result = gson.fromJson(jsonResult, ItemResult.class);
            if (result.error)
                throw new IllegalStateException("Json extractor says input has error");
            return new ItemIdeas(result);
        });
    }

    public static final class ItemResult {
        boolean error;
        List<String> items;
        List<String> translated;
    }
}
