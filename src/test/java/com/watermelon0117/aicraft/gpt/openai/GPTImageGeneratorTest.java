package com.watermelon0117.aicraft.gpt.openai;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The item sheet prompt is the texture server's buildItemSheetPrompt, word for word; the server checks it the same way
 * (test/units.test.js).
 */
class GPTImageGeneratorTest {

    /**
     * How test/item_sheet_3x/make_prompts.mjs made the round B sheet prompt from the item prompt. The two must not drift
     * apart: a change to the item prompt that this no longer finds needs a new test of the sheet before it is copied there.
     */
    private static String sheetPromptFromItemPrompt(String name, String description) {
        List<String[]> replacements = List.of(
                new String[]{"Create a Minecraft inventory sprite of " + name + ".\n",
                        "Create a Minecraft inventory sprite sheet of " + name + ": three drawings of the same item side by side in one image.\n"},
                new String[]{"Description: " + description + "\n", "Description: " + description + "\n"
                        + "The image is exactly three equal squares in a row, with no gaps, labels or dividers between them. "
                        + "Each square holds one complete item, centered in its own square; no part of an item reaches into a neighboring square. "
                        + "All three are the same item made of the same materials, with the same color palette, as the description says; "
                        + "they differ only in small details of shape. Never turn a copy into another material, tier or color scheme.\n"},
                new String[]{"The whole image is a native 16x16 bitmap enlarged exactly 64 times by nearest neighbor. ",
                        "Each square is a native 16x16 bitmap enlarged exactly 32 times by nearest neighbor. "},
                new String[]{"Make the item SMALL: its entire silhouette fits in the central 8 columns and 8 rows, ",
                        "Make each item SMALL: its entire silhouette fits in the central 8 columns and 8 rows of its square, "},
                new String[]{"occupying no more than half the image width or height. ", "occupying no more than half the square's width or height. "},
                new String[]{"Leave the outer four pixel rows and columns fully transparent (alpha 0). ",
                        "Leave the outer four pixel rows and columns of every square fully transparent (alpha 0). "},
                new String[]{"Every native pixel is a solid 64x64 square on the same grid. ", "Every native pixel is a solid 32x32 square on the same grid. "},
                new String[]{"Draw it as a flat 2D front-facing inventory icon", "Draw each one as a flat 2D front-facing inventory icon"},
                new String[]{"with only the item visible. ", "with only the three items visible. "},
                new String[]{"Keep the item opaque;", "Keep the items opaque;"},
                new String[]{"Keep the item fully visible and recognizable. ", "Keep every item fully visible and recognizable. "});
        String prompt = GPTImageGenerator.buildPrompt(name, description);
        for (String[] replacement : replacements) {
            int at = prompt.indexOf(replacement[0]);
            assertEquals(-1, prompt.indexOf(replacement[0], at + 1), "the item prompt says this more than once: " + replacement[0]);
            if (at < 0) {
                throw new AssertionError("the item prompt no longer says: " + replacement[0]);
            }
            prompt = prompt.substring(0, at) + replacement[1] + prompt.substring(at + replacement[0].length());
        }
        return prompt;
    }

    @Test
    void theItemSheetPromptIsTheItemPromptChangedOnlyWhereThreeSquaresNeedIt() {
        for (String[] item : new String[][]{{"Test Item", "A small test item."}, {"Netherite Battle Minigun", "A dark gray gun with $& in it."}}) {
            assertEquals(sheetPromptFromItemPrompt(item[0], item[1]), GPTImageGenerator.buildItemSheetPrompt(item[0], item[1]));
        }
    }
}
