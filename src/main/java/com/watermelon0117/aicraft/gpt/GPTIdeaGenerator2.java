package com.watermelon0117.aicraft.gpt;

public class GPTIdeaGenerator2 {  //normal naming style
    /*String inst="Given a crafting grid filled with [Material] forming a [Shape], generate three item names.\n" +
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
    BaseGPTIdeaGeneratorwParser generator=new BaseGPTIdeaGeneratorwParser("You are a Minecraft crafting recipe solver");
    protected String buildPrompt(Recipe recipe) {
        String[] input = recipe.getDisplayNames();
        StringBuilder prompt = new StringBuilder(inst);
        prompt.append("Filled Slots:\n");
        for (int i = 0; i < 9; i++) {
            if (!input[i].contentEquals("empty"))
                prompt.append(String.format("- (%d,%d): %s\n", i / 3, i % 3, input[i]));
        }
        for (int i = 0; i < 9; i++) {
            prompt.append(input[i].contentEquals("empty") ? "[]" : String.format("[%s]", input[i]));
            if (i % 3 == 2)
                prompt.append("\n");
        }
        return prompt.toString();
    }
    protected ItemIdeas postProcess(Recipe recipe, ItemIdeas items) {
        var match = RecipeShapeMatcher.match(recipe.items);
        if (match != null) {
            String type = match.shapeName();
            if (!items.id[0].contains(type) && !items.id[1].contains(type) && !items.id[2].contains(type)) {
                items.id[2] = match.materialName() + " " + type;
            }
        }
        return items;
    }
    public CompletableFuture<ItemIdeas> generate(Recipe recipe){
        return generator.generate(buildPrompt(recipe)).thenApply(items-> postProcess(recipe, items));
    }*/
}
