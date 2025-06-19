package com.watermelon0117.aicraft;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class GPTItemGenerator2 {  //normal naming style
    String inst="Given a crafting grid filled with [Material] forming a [Shape], generate three item names.\n" +
            "You are identifying the most natural, direct, and commonly accepted name for an item crafted in Minecraft, based on a 3×3 recipe.\n" +
            "You are not inventing a new name.\n" +
            "You are recognizing what the item *is*, based on the ingredients and layout.\n" +
            "If the recipe clearly represents a known real-world item (like a salad, soup, sandwich, etc), return that **standard name**.\n" +
            "Do not reword it, do not embellish, and do not add variation.  \n" +
            "\n" +
            "\n" +
            "Follow these steps:\n" +
            "1. Reconstruct the crafting layout mentally using the coordinates.  \n" +
            "2. Identify what the pattern resembles\n" +
            "3. Based on that shape and the material, infer the likely **function** of the item, make few different guesses.\n" +
            "4. Generate 3 item names that:\n" +
            "   - Reflect the shape and material\n" +
            "   - Sound plausible and useful in gameplay\n" +
            "   - Be simple and intuitive\n" +
            "   - Avoid names that are vague, overly magical, or poetic.\n" +
            "   - Use common object names when appropriate, like \"Bucket\", \"Pickaxe\", or \"Hamburger\". Do not overcomplicate the names.\n" +
            "   - Avoid unnecessarily complex or overly descriptive names. If a normal word fits (e.g. Hamburger, Shovel, Pot), use it.\n" +
            "\n" +
            "Input format:\n" +
            "Filled Slots:\n" +
            "- (row, col): Item name\n\n";
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
            "You are a Minecraft crafting recipe solver",
            "text");
    private final OpenAIHttpClient extractor = new OpenAIHttpClient("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",
            "gpt-4.1",  //gpt-4o gpt-4.1
            0.0,
            1024,
            "You are a helpful assistant that extracts structured data from text.",
            "json_object");
    public GPTItemGenerator2(){
        this.gson = new GsonBuilder()
                // map Java camelCase ↔︎ JSON snake_case automatically
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .create();
    }
    public CompletableFuture<String[]> generate(Recipe recipe) throws CompletionException{
        return generate(recipe.getDisplayNames());
    }
    public CompletableFuture<String[]> generate(String[] input) throws CompletionException {
        StringBuilder prompt = new StringBuilder(inst);
        prompt.append("Filled Slots:\n");
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
        try {
            return client.chat(prompt.toString()).thenCompose(rawResult->{
                System.out.println(rawResult);
                try {
                    return extractor.chat(inst2+rawResult);
                } catch (IOException | InterruptedException e) {
                    throw new CompletionException(e);
                }
            }).thenCompose(jsonResult->{
                System.out.println(jsonResult);
                ItemResult result = gson.fromJson(jsonResult, ItemResult.class);
                if(result.error)
                    throw new CompletionException(new IllegalStateException("Json extractor returned error"));
                return CompletableFuture.completedFuture(new String[]{result.items.get(0), result.items.get(1), result.items.get(2)});
            });
        } catch (IOException | InterruptedException e) {
            throw new CompletionException(e);
        }
    }
    private static final class ItemResult{
        boolean error;
        List<String> items;
    }
}
