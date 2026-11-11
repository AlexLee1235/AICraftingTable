package com.watermelon0117.aicraft.gpt.openai;

import com.watermelon0117.aicraft.common.ItemStackArray;
import com.watermelon0117.aicraft.gpt.ItemIdeas;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/** The texture server's {@code /ideas} prompt ({@code ideas.js}) for a mod that can place blocks, tested on real recipes: change both copies, and test it again. */
public class GPTIdeaGenerator {
    private static final String ITEM_LINES = "Given a crafting grid filled with material in a shape, generate three plausible item names.\n"
            + "You should infer what the item is from ingredients and layout.\n";
    private static final String BLOCK_LINES = "Given a crafting grid filled with material in a shape, generate three plausible names for what it makes.\n"
            + "Each can be an item, or a block that is placed in the world.\n"
            + "You should infer what it is from ingredients and layout.\n"
            + "Choose a block only when the ingredients and layout suggest one, and make its name clearly read as a block.\n";
    private static final String RULES = "Do not output names that already exist in Minecraft.\n"
            + "Keep names simple and direct.\n"
            + "Output analysis first, then list 3 names.\n";
    /** How each block shape reads in the middle of a sentence; stairs is already plural. */
    private static final Map<String, String> BLOCK_PHRASES = Map.of("Slab", "a slab", "Stairs", "stairs", "Wall", "a wall", "Fence", "a fence");

    BaseGPTIdeaGeneratorwParser generator = new BaseGPTIdeaGeneratorwParser("You are a Minecraft crafting recipe solver", "gpt-4.1", 0.0);

    /** A tool or armor recipe skips the block judge, so it is asked for items. */
    static String buildPrompt(String[] grid) {
        RecipeShapeMatcher.Match toolShape = RecipeShapeMatcher.match(grid);
        boolean asksForBlocks = toolShape == null;
        RecipeShapeMatcher.Match blockShape = asksForBlocks ? RecipeShapeMatcher.matchBlockShape(grid) : null;
        boolean isSquare = asksForBlocks && RecipeShapeMatcher.isSquare(grid);
        String prompt = (asksForBlocks ? BLOCK_LINES : ITEM_LINES) + RULES + "\n"
                + "Recipe(3x3 grid): \n"
                + "Top Left: " + grid[0] + "\n"
                + "Top Middle: " + grid[1] + "\n"
                + "Top Right: " + grid[2] + "\n"
                + "Middle Left: " + grid[3] + "\n"
                + "Center: " + grid[4] + "\n"
                + "Middle Right: " + grid[5] + "\n"
                + "Bottom Left: " + grid[6] + "\n"
                + "Bottom Middle: " + grid[7] + "\n"
                + "Bottom Right: " + grid[8] + "\n";
        if (toolShape != null) {
            prompt += "This has the same shape as a vanilla Minecraft " + toolShape.shapeName() + ". If it makes sense, you may suggest one " + toolShape.shapeName() + " in your answer.";
        }
        // It names the recipe, not the result, since three of something in a row is also bread or paper.
        if (blockShape != null) {
            String shape = blockShape.shapeName();
            prompt += "This recipe has the same layout as the vanilla Minecraft " + shape.toLowerCase(Locale.ROOT) + " recipe. "
                    + "If the material is one you build with, one of the names may be " + BLOCK_PHRASES.get(shape) + " made of it, with \"" + shape + "\" in the name.";
        }
        // It says the layout outright: not told, gpt-4.1 called all 8 tested 2x2 recipes an L, a zig-zag or a line.
        if (isSquare) {
            prompt += "The ingredients are arranged in a 2x2 square. If the material is one you build with, you may suggest one Tile or Brick in your answer.";
        }
        return prompt;
    }

    public CompletableFuture<ItemIdeas> generate(ItemStackArray recipe, String lang, String user) {
        String prompt = buildPrompt(recipe.getDisplayNames());
        System.out.println(prompt);
        return generator.generate(prompt, lang, user);
    }
}
