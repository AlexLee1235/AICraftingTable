package com.watermelon0117.aicraft.gpt;

import com.watermelon0117.aicraft.recipes.Recipe;
import com.watermelon0117.aicraft.recipes.RecipeShapeMatcher;

import java.util.concurrent.CompletableFuture;

public class GPTIdeaGenerator4 {  //normal naming style
    String inst="Given a crafting grid filled with [Material] forming a [Shape], generate three item names.\n" +
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
    BaseGPTIdeaGeneratorwParser generator=new BaseGPTIdeaGeneratorwParser("You are a Minecraft crafting recipe solver");
    protected String buildPrompt(Recipe recipe) {
        String[] in = recipe.getDisplayNames();
        String sb = inst +
                "Top Left: " + in[0] + '\n' +
                "Top: " + in[1] + '\n' +
                "Top Right: " + in[2] + '\n' +
                "Left: " + in[3] + '\n' +
                "Center: " + in[4] + '\n' +
                "Right: " + in[5] + '\n' +
                "Bottom Left: " + in[6] + '\n' +
                "Bottom : " + in[7] + '\n' +
                "Bottom Right: " + in[8];
        return sb;
    }
    protected String[] postProcess(Recipe recipe, String[] items) {
        var match = RecipeShapeMatcher.match(recipe.items);
        if (match != null) {
            String type = match.shapeName();
            if (!items[0].contains(type) && !items[1].contains(type) && !items[2].contains(type)) {
                items[2] = match.materialName() + " " + type;
            }
        }
        return items;
    }
    public CompletableFuture<String[]> generate(Recipe recipe){
        return generator.generate(buildPrompt(recipe)).thenApply(items-> postProcess(recipe, items));
    }
}

