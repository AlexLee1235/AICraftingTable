package com.watermelon0117.aicraft.gpt;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.watermelon0117.aicraft.menu.Recipe;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class GPTItemGenerator3 {  //the only worked shape recognizing
    String inst="You are a Minecraft item designer.\n" +
            "\n" +
            "Given a crafting grid filled with [Material] forming a [Shape], generate three unique Minecraft-style item names. The names should reflect the item's potential function as a [Item Category], be practical in gameplay, and not overlap with existing game items.\n" +
            "\n" +
            "Follow these steps:\n" +
            "1. Reconstruct the crafting layout mentally using the coordinates.  \n" +
            "2. Identify what the pattern resembles\n" +
            "3. Based on that shape and the material, infer the likely **function** of the item.  \n" +
            "4. Invent 3 original Minecraft-style item names that:\n" +
            "   - Reflect the shape and function\n" +
            "   - Sound plausible and useful in gameplay\n" +
            "   - Be simple and intuitive\n" +
            "   - Not vague or broad\n" +
            "   - Avoid complex name, useless item or item that repeat function with existed one.\n" +
            "\n" +
            "Input format:\n" +
            "Filled Slots:\n" +
            "- (row, col): Item name\n";
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
    private final OpenAIHttpClient client = new OpenAIHttpClient("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",
            "gpt-4o",  //gpt-4o gpt-4.1
            0.0,
            2048,
            inst,
            "text");
    private final OpenAIHttpClient extractor = new OpenAIHttpClient("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",
            "gpt-4.1",  //gpt-4o gpt-4.1
            0.0,
            1024,
            "You are a helpful assistant that extracts structured data from text.",
            "json_object");
    public GPTItemGenerator3(){
        this.gson = new GsonBuilder()
                // map Java camelCase ↔︎ JSON snake_case automatically
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .create();
    }
    public CompletableFuture<String[]> generate(Recipe recipe) throws IOException, InterruptedException{
        return generate(recipe.getDisplayNames());
    }
    public CompletableFuture<String[]> generate(String[] input) throws IOException, InterruptedException {
        StringBuilder prompt = new StringBuilder("Filled Slots:\n");
        for (int i = 0; i < 9; i++) {
            if (!input[i].contentEquals("empty"))
                prompt.append(String.format("- (%d,%d): %s\n", i / 3, i % 3, input[i]));
        }
        for (int i = 0; i < 9; i++) {
            prompt.append(input[i].contentEquals("empty") ? "[]" : String.format("[%s]", input[i]));
            if(i%3==2)
                prompt.append("\n");
        }
        System.out.println(prompt);
        return client.chat(prompt.toString()).thenCompose(rawResult->{
            System.out.println(rawResult);
            try {
                return extractor.chat(inst2+rawResult);
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }).thenCompose(jsonResult->{
            System.out.println(jsonResult);
            ItemResult result = gson.fromJson(jsonResult, ItemResult.class);
            return CompletableFuture.completedFuture(new String[]{result.items.get(0), result.items.get(1), result.items.get(2)});
        });
    }
    private static final class ItemResult{
        boolean error;
        List<String> items;
    }
}
