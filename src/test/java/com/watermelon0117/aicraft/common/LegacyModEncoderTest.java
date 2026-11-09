package com.watermelon0117.aicraft.common;

import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegacyModEncoderTest {

    @Test
    void theModReproducesTheSpriteExactly() {
        assertReproduced(SyntheticSprites.sprite(SyntheticSprites.diamond(15), 11), 16);
    }

    @Test
    void handlesLargeSingleColorAreasHolesAndSeparateParts() {
        boolean[][] block = SyntheticSprites.filled(10, 10);
        BufferedImage plain = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 10; y++) {
            for (int x = 0; x < 10; x++) {
                plain.setRGB(x, y, block[y][x] ? 0xFF3A7D2C : 0);
            }
        }
        assertReproduced(plain, 16);

        boolean[][] ring = SyntheticSprites.filled(12, 12);
        for (int y = 4; y < 8; y++) {
            for (int x = 4; x < 8; x++) {
                ring[y][x] = false;
            }
        }
        assertReproduced(SyntheticSprites.sprite(ring, 12), 16);

        boolean[][] parts = new boolean[9][20];
        for (int y = 0; y < 9; y++) {
            for (int x = 0; x < 20; x++) {
                parts[y][x] = x < 7 || x > 12;
            }
        }
        assertReproduced(SyntheticSprites.sprite(parts, 13), 24);
    }

    @Test
    void neverUsesASpriteColorAsTheBackground() {
        BufferedImage sprite = new BufferedImage(3, 3, BufferedImage.TYPE_INT_ARGB);
        sprite.setRGB(1, 1, 0xFFFF00FF);

        assertTrue(LegacyModEncoder.rankBackgrounds(sprite, new int[]{0, 0, 3, 3}).stream().noneMatch(bg -> bg == 0xFF00FF));
    }

    private static void assertReproduced(BufferedImage sprite, int expectedSide) {
        LegacyModEncoder.Encoded encoded = LegacyModEncoder.encode(sprite).orElseThrow();
        BufferedImage produced = ImageGridProcessor.tryProcess(LegacyModEncoder.readLikeMod(encoded.png()));

        assertEquals(expectedSide, produced.getWidth());
        assertTrue(LegacyModEncoder.sameTexture(produced, encoded.playerTexture()));
        assertTrue(SyntheticSprites.sameContent(sprite, produced));
    }
}
