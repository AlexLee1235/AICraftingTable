package com.watermelon0117.aicraft.gpt;

import com.watermelon0117.aicraft.recipes.Recipe;

public class GPTIdeaGenerator3 extends BaseGPTIdeaGeneratorwParser {  //the only worked shape recognizing, maybe not
    private static final String inst="You are a Minecraft item designer.\n" +
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
    public GPTIdeaGenerator3(){
        super(inst);
    }
    @Override
    protected String buildPrompt(Recipe recipe) {
        String[] input=recipe.getDisplayNames();
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
        return prompt.toString();
    }
}
