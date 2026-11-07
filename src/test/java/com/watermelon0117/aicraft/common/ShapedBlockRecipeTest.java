package com.watermelon0117.aicraft.common;

import com.watermelon0117.aicraft.blocks.AIBlockShape;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShapedBlockRecipeTest {
    /** Rows of three, top first. */
    private static Optional<AIBlockShape> layout(String top, String middle, String bottom) {
        return ShapedBlockRecipe.layoutOf(top + middle + bottom);
    }

    @Test
    void aRowOfThreeIsASlabInAnyRow() {
        assertEquals(Optional.of(AIBlockShape.SLAB), layout("III", "   ", "   "));
        assertEquals(Optional.of(AIBlockShape.SLAB), layout("   ", "III", "   "));
        assertEquals(Optional.of(AIBlockShape.SLAB), layout("   ", "   ", "III"));
    }

    @Test
    void stairsGoEitherWay() {
        assertEquals(Optional.of(AIBlockShape.STAIRS), layout("I  ", "II ", "III"));
        assertEquals(Optional.of(AIBlockShape.STAIRS), layout("  I", " II", "III"));
    }

    @Test
    void twoRowsOfThreeAreAWallInEitherPlace() {
        assertEquals(Optional.of(AIBlockShape.WALL), layout("III", "III", "   "));
        assertEquals(Optional.of(AIBlockShape.WALL), layout("   ", "III", "III"));
    }

    @Test
    void sticksBetweenTheMaterialAreAFenceInEitherPlace() {
        assertEquals(Optional.of(AIBlockShape.FENCE), layout("ISI", "ISI", "   "));
        assertEquals(Optional.of(AIBlockShape.FENCE), layout("   ", "ISI", "ISI"));
    }

    @Test
    void otherLayoutsAreNone() {
        assertEquals(Optional.empty(), layout("I  ", "I  ", "I  "));
        assertEquals(Optional.empty(), layout("III", "III", "III"));
        assertEquals(Optional.empty(), layout("SIS", "SIS", "   "));
        assertEquals(Optional.empty(), layout("ISI", "   ", "   "));
        assertEquals(Optional.empty(), layout("II ", "   ", "   "));
        assertEquals(Optional.empty(), layout("III", "I  ", "   "));
        assertEquals(Optional.empty(), layout("I  ", "II ", "II "));
        assertEquals(Optional.empty(), layout("I  ", " I ", "  I"));
        assertEquals(Optional.empty(), layout("SSS", "   ", "   "));
        assertEquals(Optional.empty(), layout("   ", "   ", "   "));
        assertEquals(Optional.empty(), ShapedBlockRecipe.layoutOf("III"));
        assertEquals(Optional.empty(), ShapedBlockRecipe.layoutOf(null));
    }

    @Test
    void aNameMustSayTheShapeAsAWholeWord() {
        assertTrue(ShapedBlockRecipe.names(AIBlockShape.SLAB, "Calcite Slab"));
        assertTrue(ShapedBlockRecipe.names(AIBlockShape.SLAB, "Calcite Slabs"));
        assertTrue(ShapedBlockRecipe.names(AIBlockShape.SLAB, "calcite SLAB"));
        assertTrue(ShapedBlockRecipe.names(AIBlockShape.STAIRS, "Calcite Stairs"));
        assertTrue(ShapedBlockRecipe.names(AIBlockShape.STAIRS, "Calcite Staircase"));
        assertTrue(ShapedBlockRecipe.names(AIBlockShape.STAIRS, "Calcite Stairway"));
        assertTrue(ShapedBlockRecipe.names(AIBlockShape.STAIRS, "Stair of Calcite"));
        assertTrue(ShapedBlockRecipe.names(AIBlockShape.FENCE, "Calcite Fence"));
        assertTrue(ShapedBlockRecipe.names(AIBlockShape.WALL, "Calcite Wall"));
        assertTrue(ShapedBlockRecipe.names(AIBlockShape.WALL, "Calcite-Wall"));

        assertFalse(ShapedBlockRecipe.names(AIBlockShape.WALL, "Calcite Wallpaper"));
        assertFalse(ShapedBlockRecipe.names(AIBlockShape.FENCE, "Fenced Calcite"));
        assertFalse(ShapedBlockRecipe.names(AIBlockShape.SLAB, "Calcite Bench"));
        assertFalse(ShapedBlockRecipe.names(AIBlockShape.SLAB, "Calcite Wall"));
        assertFalse(ShapedBlockRecipe.names(AIBlockShape.STAIRS, "Upstairs Calcite"));
        assertFalse(ShapedBlockRecipe.names(AIBlockShape.CUBE, "Calcite Slab"));
        assertFalse(ShapedBlockRecipe.names(AIBlockShape.SLAB, null));
    }
}
