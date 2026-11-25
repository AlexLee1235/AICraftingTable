package com.watermelon0117.aicraft.common;

import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.util.Base64;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemSheetTest {
    private static final int CELL = 32;

    @Test
    void usesTheLeftSquareWhenItGivesASprite() {
        BufferedImage left = SyntheticSprites.sprite(SyntheticSprites.diamond(10), 1);
        BufferedImage sheet = emptySheet(true);
        draw(sheet, left, 96, 96);
        draw(sheet, SyntheticSprites.sprite(SyntheticSprites.filled(8, 9), 2), 512 + 128, 112);
        draw(sheet, SyntheticSprites.sprite(SyntheticSprites.diamond(9), 3), 1024 + 112, 112);

        ItemSheet.Result result = ItemSheet.process(LegacyModEncoder.toPng(sheet));

        assertEquals(0, result.square());
        assertEquals(SpritePipeline.Route.SPRITE, result.output().route());
        BufferedImage produced = ImageGridProcessor.tryProcess(LegacyModEncoder.readLikeMod(
                Base64.getDecoder().decode(result.output().modImageB64())));
        assertTrue(LegacyModEncoder.sameTexture(produced, LegacyModEncoder.readLikeMod(result.output().playerTexture())));
        assertTrue(SyntheticSprites.sameContent(left, produced));
        // The square goes through the pipeline exactly as a 512x512 image of its own would.
        SpritePipeline.Output alone = SpritePipeline.process(Base64.getEncoder().encodeToString(result.squarePng()));
        assertEquals(alone.modImageB64(), result.output().modImageB64());
        assertArrayEquals(alone.playerTexture(), result.output().playerTexture());
    }

    @Test
    void theSquareHoldsExactlyTheSheetsPixelsHiddenColorsAndHazeIncluded() {
        BufferedImage sheet = emptySheet(true);
        Random random = new Random(5);
        for (int y = 0; y < 512; y++) {
            for (int x = 0; x < 1536; x++) {
                if (random.nextInt(4) == 0) {
                    sheet.setRGB(x, y, 0x01000000 | random.nextInt(0x1000000));
                }
            }
        }
        draw(sheet, SyntheticSprites.sprite(SyntheticSprites.diamond(10), 1), 96, 96);
        byte[] png = LegacyModEncoder.toPng(sheet);
        BufferedImage read = LegacyModEncoder.readLikeMod(png);

        ItemSheet.Result result = ItemSheet.process(png);

        BufferedImage square = LegacyModEncoder.readLikeMod(result.squarePng());
        assertEquals(512, square.getWidth());
        assertEquals(512, square.getHeight());
        for (int y = 0; y < 512; y++) {
            for (int x = 0; x < 512; x++) {
                assertEquals(read.getRGB(x + 512 * result.square(), y), square.getRGB(x, y));
            }
        }
    }

    @Test
    void anEmptyLeftSquareGivesWayToTheMiddleOne() {
        BufferedImage middle = SyntheticSprites.sprite(SyntheticSprites.diamond(10), 2);
        BufferedImage sheet = emptySheet(true);
        draw(sheet, middle, 512 + 96, 96);
        draw(sheet, SyntheticSprites.sprite(SyntheticSprites.diamond(9), 3), 1024 + 112, 112);

        ItemSheet.Result result = ItemSheet.process(LegacyModEncoder.toPng(sheet));

        assertEquals(1, result.square());
        assertEquals(SpritePipeline.Route.SPRITE, result.output().route());
        assertTrue(SyntheticSprites.sameContent(middle, LegacyModEncoder.readLikeMod(result.output().playerTexture())));
    }

    @Test
    void squaresWhoseDrawingACutRunsThroughAreSkipped() {
        BufferedImage right = SyntheticSprites.sprite(SyntheticSprites.diamond(9), 3);
        BufferedImage sheet = emptySheet(true);
        // From x = 384 to 704: the cut at 512 runs through it, so neither the left nor the middle square has it whole.
        draw(sheet, SyntheticSprites.sprite(SyntheticSprites.filled(10, 8), 1), 384, 128);
        draw(sheet, right, 1024 + 112, 112);

        ItemSheet.Result result = ItemSheet.process(LegacyModEncoder.toPng(sheet));

        assertEquals(2, result.square());
        assertTrue(SyntheticSprites.sameContent(right, LegacyModEncoder.readLikeMod(result.output().playerTexture())));
    }

    @Test
    void anItemTouchingTheTopOfItsSquareIsCutToo() {
        BufferedImage middle = SyntheticSprites.sprite(SyntheticSprites.diamond(10), 2);
        BufferedImage sheet = emptySheet(true);
        draw(sheet, SyntheticSprites.sprite(SyntheticSprites.filled(8, 10), 1), 128, 0);
        draw(sheet, middle, 512 + 96, 96);

        assertEquals(1, ItemSheet.process(LegacyModEncoder.toPng(sheet)).square());
    }

    @Test
    void withoutTransparencyEverySquareIsTriedFromTheLeft() {
        BufferedImage left = SyntheticSprites.sprite(SyntheticSprites.diamond(10), 1);
        BufferedImage sheet = emptySheet(false);
        draw(sheet, left, 96, 96);
        draw(sheet, SyntheticSprites.sprite(SyntheticSprites.diamond(9), 3), 1024 + 112, 112);

        ItemSheet.Result result = ItemSheet.process(LegacyModEncoder.toPng(sheet));

        assertEquals(0, result.square());
        assertEquals(SpritePipeline.Route.SPRITE, result.output().route());
        assertTrue(SyntheticSprites.sameContent(left, LegacyModEncoder.readLikeMod(result.output().playerTexture())));
    }

    @Test
    void withNoSpriteAnywhereTheLeftSquareGivesTheSingleImageFallback() {
        ItemSheet.Result result = ItemSheet.process(LegacyModEncoder.toPng(emptySheet(true)));

        assertEquals(0, result.square());
        assertEquals(SpritePipeline.Route.FALLBACK_NO_SPRITE, result.output().route());
        BufferedImage returned = LegacyModEncoder.readLikeMod(Base64.getDecoder().decode(result.output().modImageB64()));
        assertEquals(512, returned.getWidth());
        assertEquals(512, returned.getHeight());
    }

    @Test
    void refusesAnImageOfAnotherSize() {
        byte[] square = LegacyModEncoder.toPng(new BufferedImage(1024, 1024, BufferedImage.TYPE_INT_ARGB));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> ItemSheet.process(square));

        assertEquals("Expected a 1536x512 item sheet, got 1024x1024", error.getMessage());
    }

    @Test
    void refusesAnUnreadableImage() {
        assertThrows(IllegalArgumentException.class, () -> ItemSheet.process(new byte[]{1, 2, 3, 4}));
    }

    /** Like a Sunburst sheet: alpha 0 with random hidden colors, or for an opaque one a flat magenta background. */
    private static BufferedImage emptySheet(boolean transparent) {
        Random random = new Random(7);
        BufferedImage sheet = new BufferedImage(1536, 512, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 512; y++) {
            for (int x = 0; x < 1536; x++) {
                sheet.setRGB(x, y, transparent ? random.nextInt(0x1000000) : 0xFFFF00FF);
            }
        }
        return sheet;
    }

    /** Each cell of the sprite as a 32-pixel square, as the prompt asks, from (left, top). */
    private static void draw(BufferedImage sheet, BufferedImage sprite, int left, int top) {
        for (int y = 0; y < sprite.getHeight() * CELL; y++) {
            for (int x = 0; x < sprite.getWidth() * CELL; x++) {
                int argb = sprite.getRGB(x / CELL, y / CELL);
                if ((argb >>> 24) != 0) {
                    sheet.setRGB(left + x, top + y, argb);
                }
            }
        }
    }
}
