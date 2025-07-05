package com.watermelon0117.aicraft.gpt;

import com.watermelon0117.aicraft.common.ItemStackArray;
import com.watermelon0117.aicraft.common.RecipeShapeMatcher;

import java.util.concurrent.CompletableFuture;

public class GPTIdeaGenerator4 {  //normal naming style
    /*String inst="Given a crafting grid filled with [Material] forming a [Shape], generate three item names.\n" +
            "You are identifying the most natural, direct, and commonly accepted name for an item crafted in Minecraft, based on a 3×3 recipe.\n" +
            "You are not inventing a new name.\n" +
            "You are recognizing what the item *is*, based on the ingredients and layout.\n" +
            "If the recipe clearly represents a known real-world item (like a salad, soup, sandwich, etc), return that **standard name**.\n" +
            "Do not reword it, do not embellish, and do not add variation.  \n" +
            "\n" +
            "\n" +
            "Follow these steps:\n" +
            "1. Identify what the pattern resembles\n" +
            "2. Based on that shape and the material, infer the likely **function** of the item, make few different guesses.\n" +
            "3. Generate 3 item names that:\n" +
            "   - Not already in Minecraft\n" +
            "   - Reflect the shape and material\n" +
            "   - Sound plausible and useful in gameplay\n" +
            "   - Be simple and intuitive\n" +
            "   - Avoid names that are vague, overly magical, or poetic.\n" +
            "   - Use common object names when appropriate, like \"Bucket\", \"Pickaxe\", or \"Hamburger\". Do not overcomplicate the names.\n" +
            "   - Avoid unnecessarily complex or overly descriptive names. If a normal word fits (e.g. Hamburger, Shovel, Pot), use it.\n";
    BaseGPTIdeaGeneratorwParser generator=new BaseGPTIdeaGeneratorwParser("You are a Minecraft crafting recipe solver", "gpt-4o", 0);
    protected String buildPrompt(ItemStackArray recipe) {
        String[] in = recipe.getDisplayNames();
        String prompt = String.format(
                "%sTop Left: %s\n" +
                        "Top: %s\n" +
                        "Top Right: %s\n" +
                        "Left: %s\n" +
                        "Center: %s\n" +
                        "Right: %s\n" +
                        "Bottom Left: %s\n" +
                        "Bottom: %s\n" +
                        "Bottom Right: %s\n",
                inst, in[0], in[1], in[2], in[3], in[4], in[5], in[6], in[7], in[8]
        );
        var match = RecipeShapeMatcher.match(recipe);
        if (match != null) {
            prompt += String.format(
                    "This has the same shape as a vanilla Minecraft %s. If it makes sense, you may suggest a %s as an idea, but you can NOT suggest more than one %s in your answer",
                    match.shapeName(), match.shapeName(), match.shapeName()
            );
        }
        System.out.println(prompt);
        return prompt;
    }


    public CompletableFuture<ItemIdeas> generate(ItemStackArray recipe, String lang){
        return generator.generate(buildPrompt(recipe), lang);
    }*/
}

