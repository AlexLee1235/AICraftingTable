package com.watermelon0117.aicraft.gpt;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.watermelon0117.aicraft.menu.Recipe;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class GPTItemGenerator {
    private static String inst1="### Goal  \n" +
            "From the 3 × 3 crafting recipe below, invent **one to three item names** that could reasonably be produced by that pattern.  \n" +
            "*Key requirement*: in your `\"reasoning\"` sentence(s) you **must reference at least one concrete geometric feature** you observe (e.g. “horizontal row of three ingots”, “L-shape of planks”, “vertical shaft of sticks”).  \n" +
            "Keep it simple and making sense.\n" +
            "\n" +
            "### Recipe (0-based coordinates of NON-empty slots)\n" +
            "[\n";
    private static String inst2=   "]\n" +
            "\n" +
            "### Output format  \n" +
            "Return a single JSON object exactly matching:\n" +
            "\n" +
            "```json\n" +
            "{\n" +
            "  \"reasoning\": \"string (≤ 2 sentences, must cite a shape element + at least one ingredient)\",\n" +
            "  \"items\": [\n" +
            "    { \"name\": \"string\" },\n" +
            "    { \"name\": \"string\" },\n" +
            "    { \"name\": \"string\" }\n" +
            "  ]\n" +
            "}";
    private final Gson gson;
    private final OpenAIHttpClient client = new OpenAIHttpClient("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",
            "gpt-4o",  //gpt-4o gpt-4.1
            0.0,
            1024,
            "You are an expert Minecraft item designer.",
            "json_object");
    public GPTItemGenerator(){
        this.gson = new GsonBuilder()
                // map Java camelCase ↔︎ JSON snake_case automatically
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .create();
    }
    public CompletableFuture<String[]> generate(Recipe recipe) throws IOException, InterruptedException{
        return generate(recipe.getDisplayNames());
    }
    public CompletableFuture<String[]> generate(String[] input) throws IOException, InterruptedException {
        StringBuilder prompt = new StringBuilder(inst1);
        for (int i = 0; i < 9; i++) {
            if (!input[i].contentEquals("empty"))
                prompt.append(String.format("{ \"r\":%d, \"c\":%d, \"item\":\"%s\" },\n", i / 3, i % 3, input[i]));
        }
        prompt.append(inst2);
        //System.out.println(prompt);
        return client.chat(prompt.toString()).thenCompose(rawResult->{
            //System.out.println(rawResult);
            ItemResult result = gson.fromJson(rawResult, ItemResult.class);
            //System.out.println(result.reasoning);
            return CompletableFuture.completedFuture(new String[]{result.items.get(0).name, result.items.get(1).name, result.items.get(2).name});
        });
    }
    private static final class ItemResult{
        String reasoning;
        List<MyItems> items;

        static final class MyItems {
            String name;
        }
    }
}
