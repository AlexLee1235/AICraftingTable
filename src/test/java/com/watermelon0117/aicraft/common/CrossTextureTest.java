package com.watermelon0117.aicraft.common;

import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrossTextureTest {
    private static final int LEAF = 0xFF2E8B2E, STEM = 0xFF5A3A1A;

    /** A blob of leaves on a stem one cell wide, inside a transparent sprite of the given size. */
    private static BufferedImage plant(int spriteSide, int leafSide, int stemHeight) {
        BufferedImage sprite = new BufferedImage(spriteSide, spriteSide, BufferedImage.TYPE_INT_ARGB);
        int left = (spriteSide - leafSide) / 2, top = (spriteSide - leafSide - stemHeight) / 2;
        for (int y = 0; y < leafSide; y++) {
            for (int x = 0; x < leafSide; x++) {
                sprite.setRGB(left + x, top + y, LEAF);
            }
        }
        for (int y = 0; y < stemHeight; y++) {
            sprite.setRGB(spriteSide / 2, top + leafSide + y, STEM);
        }
        return sprite;
    }

    private static int[] box(BufferedImage image) {
        return LegacyModEncoder.contentBox(image);
    }

    @Test
    void aSmallPlantKeepsItsPixelsAndStandsOnTheBottomRow() {
        BufferedImage texture = CrossTexture.fromSprite(plant(16, 6, 4));

        assertEquals(16, texture.getWidth());
        assertEquals(16, texture.getHeight());
        int[] plant = box(texture);
        assertEquals(6, plant[2]);
        assertEquals(10, plant[3]);
        assertEquals(15, plant[1] + plant[3] - 1, "the last row of the plant is the last row of the texture");
        assertEquals(5, plant[0], "centered");
        assertEquals(LEAF, texture.getRGB(5, 6));
        assertEquals(STEM, texture.getRGB(8, 15));
        assertEquals(0, texture.getRGB(5, 15));
    }

    @Test
    void somethingThatHangsKeepsItsPixelsAndStartsOnTheTopRow() {
        BufferedImage texture = CrossTexture.fromSprite(plant(16, 6, 4), true);

        int[] plant = box(texture);
        assertEquals(0, plant[1], "the first row of what hangs is the first row of the texture");
        assertEquals(6, plant[2]);
        assertEquals(10, plant[3]);
        assertEquals(5, plant[0], "centered");
        assertEquals(LEAF, texture.getRGB(5, 0));
        assertEquals(STEM, texture.getRGB(8, 9));
        assertEquals(0, texture.getRGB(8, 10));
    }

    @Test
    void aLargeSpriteShrinksToSixteenAndKeepsAStemOneCellWide() {
        BufferedImage texture = CrossTexture.fromSprite(plant(32, 18, 12));

        int[] plant = box(texture);
        assertTrue(plant[2] <= 16 && plant[3] <= 16, "fits: " + plant[2] + "x" + plant[3]);
        assertEquals(15, plant[1] + plant[3] - 1);
        boolean stemAtTheBottom = false;
        for (int x = 0; x < 16; x++) {
            stemAtTheBottom |= texture.getRGB(x, 15) == STEM;
        }
        assertTrue(stemAtTheBottom, "the stem survives, or the leaves would float");
    }

    @Test
    void everyPixelIsOpaqueOrEmpty() {
        BufferedImage texture = CrossTexture.fromSprite(plant(24, 12, 8));

        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                int alpha = texture.getRGB(x, y) >>> 24;
                assertTrue(alpha == 0 || alpha == 255, "alpha " + alpha + " at " + x + "," + y);
            }
        }
    }

    @Test
    void aWideSpriteKeepsItsProportions() {
        BufferedImage sprite = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        for (int y = 12; y < 20; y++) {
            for (int x = 0; x < 32; x++) {
                sprite.setRGB(x, y, LEAF);
            }
        }

        int[] plant = box(CrossTexture.fromSprite(sprite));

        assertEquals(16, plant[2]);
        assertEquals(4, plant[3]);
        assertEquals(12, plant[1]);
    }

    @Test
    void anUnreadableImageFailsWithTheReason() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> CrossTexture.process(new byte[]{1, 2, 3}));
        assertTrue(error.getMessage().contains("unreadable"), error.getMessage());
    }

    @Test
    void aBlankImageFailsWithTheReason() {
        byte[] blank = LegacyModEncoder.toPng(new BufferedImage(1024, 1024, BufferedImage.TYPE_INT_ARGB));
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> CrossTexture.process(blank));
        assertTrue(error.getMessage().contains("No plant"), error.getMessage());
    }

    @Test
    void aModelLikeImageGoesThroughTheWholeStep() throws Exception {
        BufferedImage model = SyntheticSprites.modelImage(SyntheticSprites.sprite(SyntheticSprites.diamond(12), 7), 32, 300, 300, true);

        BufferedImage texture = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(CrossTexture.process(LegacyModEncoder.toPng(model))));

        assertEquals(16, texture.getWidth());
        int[] plant = box(texture);
        assertEquals(15, plant[1] + plant[3] - 1);
    }
}
