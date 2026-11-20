package com.watermelon0117.aicraft.gpt.openai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecipeShapeMatcherTest {
    /** 'I' is Iron, 'S' a Stick and ' ' empty, three cells a row. */
    private static String[] grid(String cells) {
        String[] grid = new String[9];
        for (int i = 0; i < 9; i++) {
            grid[i] = switch (cells.charAt(i)) {
                case 'I' -> "Iron";
                case 'S' -> "Stick";
                case 'G' -> "Gold";
                default -> "empty";
            };
        }
        return grid;
    }

    @Test
    void toolsAndArmorAreFound() {
        assertEquals("Pickaxe", RecipeShapeMatcher.match(grid("III S  S ")).shapeName());
        assertEquals("Boots", RecipeShapeMatcher.match(grid("   I II I")).shapeName());
        assertNull(RecipeShapeMatcher.match(grid("III      ")));
    }

    @Test
    void blockShapesAreFoundWhereverTheySit() {
        assertEquals("Slab", RecipeShapeMatcher.matchBlockShape(grid("      III")).shapeName());
        assertEquals("Stairs", RecipeShapeMatcher.matchBlockShape(grid("I  II III")).shapeName());
        assertEquals("Stairs", RecipeShapeMatcher.matchBlockShape(grid("  I IIIII")).shapeName());
        assertEquals("Wall", RecipeShapeMatcher.matchBlockShape(grid("   IIIIII")).shapeName());
        assertEquals("Fence", RecipeShapeMatcher.matchBlockShape(grid("   ISIISI")).shapeName());
    }

    @Test
    void aBlockShapeNeedsOneMaterial() {
        assertNull(RecipeShapeMatcher.matchBlockShape(grid("IGI      ")));
        assertNull(RecipeShapeMatcher.matchBlockShape(grid("I        ")));
    }

    @Test
    void aSquareOfFourIsFoundInAnyCorner() {
        assertTrue(RecipeShapeMatcher.isSquare(grid("II II    ")));
        assertTrue(RecipeShapeMatcher.isSquare(grid("    II II")));
        assertFalse(RecipeShapeMatcher.isSquare(grid("IG II    ")));
        assertFalse(RecipeShapeMatcher.isSquare(grid("III      ")));
    }
}
