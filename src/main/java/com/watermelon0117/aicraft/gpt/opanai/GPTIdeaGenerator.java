package com.watermelon0117.aicraft.gpt.opanai;

import com.watermelon0117.aicraft.common.ItemStackArray;
import com.watermelon0117.aicraft.gpt.ItemIdeas;

import java.util.concurrent.CompletableFuture;

public class GPTIdeaGenerator {
    String inst = "Given a crafting grid filled with [Material] forming a [Shape], generate three item names.\n" +
            "You are identifying the most natural, direct, and commonly accepted name for an item crafted in Minecraft, based on a 3×3 recipe.\n" +
            "You are not inventing a new name.\n" +
            "You are recognizing what the item *is*, based on the ingredients and layout.\n" +
            "If the recipe clearly represents a known real-world item (like a salad, soup, sandwich, etc), return that **standard name**.\n" +
            "Do not reword it, do not embellish, and do not add variation.  \n" +
            "\n" +
            "Follow these steps:\n" +
            "First, analyze the spatial pattern without guessing what the item is.\n" +
            "Describe:\n" +
            "    1. Which rows and columns are filled?\n" +
            "    2. The horizontal and vertical extent.\n" +
            "    3. The overall shape in words (e.g. “3x2 rectangle”, “vertical line”, “cross shape”).\n" +
            "Second, Based on that shape and the material, infer the likely **function** of the item, make few different guesses." +
            "Then, Generate 3 item names that:\n" +
            "   - Not already in Minecraft\n" +
            "   - Sound plausible and useful in gameplay\n" +
            "   - Be simple and intuitive\n" +
            "   - Avoid names that are vague, overly magical(except when input item is magical), or poetic.\n" +
            "   - Is simple, direct object names (e.g. “Hamburger,” “Pickaxe,” “Bucket”)." +
            "   - Do not include adjectives" +
            "   - If answer is tool or weapon, follow Minecraft naming style(e.g. Iron Hammer)\n" +
            "Output your analysis step by step before listing item names.\n";
    BaseGPTIdeaGeneratorwParser generator = new BaseGPTIdeaGeneratorwParser("You are a Minecraft crafting recipe solver", "gpt-4.1", 0.0);

    private String buildPrompt(ItemStackArray recipe) {
        String[] in = recipe.getDisplayNames();
        String prompt = String.format("%s\n" +
                        "Recipe(3x3 grid): \n" +
                        "Top Left: %s\n" +
                        "Top Middle: %s\n" +
                        "Top Right: %s\n" +
                        "Middle Left: %s\n" +
                        "Center: %s\n" +
                        "Middle Right: %s\n" +
                        "Bottom Left: %s\n" +
                        "Bottom Middle: %s\n" +
                        "Bottom Right: %s\n",
                inst, in[0], in[1], in[2], in[3], in[4], in[5], in[6], in[7], in[8]
        );
        var match = RecipeShapeMatcher.match(in);
        if (match != null) {
            prompt += String.format(
                    "This has the same shape as a vanilla Minecraft %s. If it makes sense, you may suggest a %s as an idea, but you can NOT suggest more than one %s in your answer",
                    match.shapeName(), match.shapeName(), match.shapeName()
            );
        }
        System.out.println(prompt);
        return prompt;
    }

    public CompletableFuture<ItemIdeas> generate(ItemStackArray recipe, String lang, String user) {
        return generator.generate(buildPrompt(recipe), lang, user);
    }
}
