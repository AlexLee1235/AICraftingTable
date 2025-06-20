package com.watermelon0117.aicraft.gpt;

import com.watermelon0117.aicraft.Recipe;

public class GPTItemGenerator extends BaseGPTItemGenerator{
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
    public GPTItemGenerator(){
        super("You are an expert Minecraft item designer.");
    }
    @Override
    protected String buildPrompt(Recipe recipe) {
        String[] input = recipe.getDisplayNames();
        StringBuilder prompt = new StringBuilder(inst1);
        for (int i = 0; i < 9; i++) {
            if (!input[i].contentEquals("empty"))
                prompt.append(String.format("{ \"r\":%d, \"c\":%d, \"item\":\"%s\" },\n", i / 3, i % 3, input[i]));
        }
        prompt.append(inst2);
        return prompt.toString();
    }
}
