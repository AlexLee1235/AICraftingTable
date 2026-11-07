package com.watermelon0117.aicraft.blocks;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** How a vanilla block's own numbers turn into the mod's levels. The vanilla numbers are those of 1.20.1. */
class VanillaPropertyTest {
    @Test
    void destroyTimesGoToTheNearestHardness() {
        assertEquals(AIBlockHardness.UNBREAKABLE, AIBlockHardness.nearest(-1.0F)); // bedrock
        assertEquals(AIBlockHardness.INSTANT, AIBlockHardness.nearest(0.0F)); // TNT, slime
        assertEquals(AIBlockHardness.SOFT, AIBlockHardness.nearest(0.3F)); // glass, glowstone
        assertEquals(AIBlockHardness.SOFT, AIBlockHardness.nearest(0.6F)); // grass block, gravel
        assertEquals(AIBlockHardness.SOFT, AIBlockHardness.nearest(0.75F)); // calcite
        assertEquals(AIBlockHardness.NORMAL, AIBlockHardness.nearest(1.5F)); // stone
        assertEquals(AIBlockHardness.NORMAL, AIBlockHardness.nearest(2.0F)); // logs, cobblestone
        assertEquals(AIBlockHardness.HARD, AIBlockHardness.nearest(3.0F)); // deepslate, ores
        assertEquals(AIBlockHardness.HARD, AIBlockHardness.nearest(3.5F)); // furnace
        assertEquals(AIBlockHardness.HARD, AIBlockHardness.nearest(50.0F)); // obsidian
    }

    /** The lines are the geometric means, sqrt(0.5 * 1.5) = 0.866 and sqrt(1.5 * 5) = 2.739. */
    @Test
    void theLinesBetweenHardnessesAreTheirGeometricMeans() {
        assertEquals(AIBlockHardness.SOFT, AIBlockHardness.nearest(0.866F));
        assertEquals(AIBlockHardness.NORMAL, AIBlockHardness.nearest(0.867F));
        assertEquals(AIBlockHardness.NORMAL, AIBlockHardness.nearest(2.738F));
        assertEquals(AIBlockHardness.HARD, AIBlockHardness.nearest(2.74F));
        assertEquals(AIBlockHardness.SOFT, AIBlockHardness.nearest(Float.MIN_VALUE));
    }

    @Test
    void anyLightIsAtLeastDimAndTenUpIsBright() {
        assertEquals(AIBlockLight.NONE, AIBlockLight.nearest(0));
        assertEquals(AIBlockLight.DIM, AIBlockLight.nearest(1));
        assertEquals(AIBlockLight.DIM, AIBlockLight.nearest(3)); // magma block
        assertEquals(AIBlockLight.DIM, AIBlockLight.nearest(9));
        assertEquals(AIBlockLight.BRIGHT, AIBlockLight.nearest(10)); // crying obsidian
        assertEquals(AIBlockLight.BRIGHT, AIBlockLight.nearest(15)); // glowstone
    }
}
