package com.watermelon0117.aicraft.common;

import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpritePixelizerTest {

    @Test
    void recoversTheDrawnSpriteFromATransparentModelImage() {
        BufferedImage sprite = SyntheticSprites.sprite(SyntheticSprites.diamond(13), 1);
        BufferedImage model = SyntheticSprites.modelImage(sprite, 37, 301, 279, true);

        SpritePixelizer.Result result = SpritePixelizer.pixelize(model).orElseThrow();

        assertEquals("source_alpha", result.maskMethod());
        assertEquals(37.0, result.step(), 0.5);
        assertEquals(13, result.contentWidth());
        assertEquals(16, result.sprite().getWidth());
        assertFalse(result.capped());
        assertTrue(SyntheticSprites.sameContent(sprite, result.sprite()));
    }

    @Test
    void recoversTheDrawnSpriteFromAnOpaqueMagentaBackground() {
        BufferedImage sprite = SyntheticSprites.sprite(SyntheticSprites.diamond(13), 2);
        BufferedImage model = SyntheticSprites.modelImage(sprite, 40, 250, 260, false);

        SpritePixelizer.Result result = SpritePixelizer.pixelize(model).orElseThrow();

        assertEquals("chroma_key", result.maskMethod());
        assertTrue(SyntheticSprites.sameContent(sprite, result.sprite()));
    }

    @Test
    void refitsFineGridsToACoarserGridOfAtMost32() {
        BufferedImage sprite = SyntheticSprites.sprite(SyntheticSprites.diamond(45), 3);
        BufferedImage model = SyntheticSprites.modelImage(sprite, 13, 220, 230, true);

        SpritePixelizer.Result result = SpritePixelizer.pixelize(model).orElseThrow();

        assertTrue(result.capped());
        assertTrue(result.sprite().getWidth() <= SpritePixelizer.MAX_SIDE, "side " + result.sprite().getWidth());
    }

    @Test
    void rejectsAnEmptyImage() {
        BufferedImage empty = new BufferedImage(1024, 1024, BufferedImage.TYPE_INT_ARGB);

        assertEquals(Optional.empty(), SpritePixelizer.pixelize(empty));
    }

    @Test
    void findPeaksMatchesScipyPlateausDistanceAndProminence() {
        double[] x = {0, 5, 5, 0, 1, 9, 0, 8, 0, 0, 3, 0};
        // plateau 1..2 -> midpoint 1; peak 7 is within distance 3 of the higher peak 5; peak 10 has prominence 3
        assertEquals(java.util.List.of(1, 5), java.util.Arrays.stream(SpritePixelizer.findPeaks(x, 4, 3)).boxed().toList());
    }
}
