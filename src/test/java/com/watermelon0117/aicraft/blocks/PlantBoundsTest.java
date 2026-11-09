package com.watermelon0117.aicraft.blocks;

import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PlantBoundsTest {
    /** A clear picture with pixels of this alpha from (left, top) to just before (right, bottom). */
    private static BufferedImage drawn(int left, int top, int right, int bottom, int alpha) {
        BufferedImage picture = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        for (int y = top; y < bottom; y++) {
            for (int x = left; x < right; x++) {
                picture.setRGB(x, y, alpha << 24 | 0x336622);
            }
        }
        return picture;
    }

    @Test
    void onlyPixelsTheCutoutShaderKeepsCount() {
        BufferedImage picture = drawn(0, 0, 16, 16, 25);
        for (int y = 4; y < 12; y++) {
            for (int x = 5; x < 11; x++) {
                picture.setRGB(x, y, 0xFF336622);
            }
        }
        assertEquals(new PlantBounds(5, 4, 11, 12), PlantBounds.of(picture));
        assertEquals(new PlantBounds(3, 2, 4, 3), PlantBounds.of(drawn(3, 2, 4, 3, 26)));
        assertNull(PlantBounds.of(drawn(0, 0, 16, 16, 25)));
        assertNull(PlantBounds.of(drawn(0, 0, 0, 0, 0)));
    }

    @Test
    void aPictureNot16x16IsRefused() {
        assertThrows(IllegalArgumentException.class, () -> PlantBounds.of(new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB)));
    }

    /** The oak sapling's own picture is drawn from column 1 to 14 and row 1 to 15; vanilla's hand-made outline is 2..14 wide and 12 tall. */
    @Test
    void theOakSaplingsPictureGivesItsWidthAndItsFullHeight() {
        assertArrayEquals(new int[]{2, 0, 2, 14, 15, 14}, new PlantBounds(1, 1, 15, 16).box());
    }

    @Test
    void aFullPictureReachesThePlanesEnds() {
        assertArrayEquals(new int[]{1, 0, 1, 15, 16, 15}, new PlantBounds(0, 0, 16, 16).box());
    }

    @Test
    void onePixelGivesTheSmallestOutline() {
        assertArrayEquals(new int[]{7, 0, 7, 9, 1, 9}, new PlantBounds(7, 15, 8, 16).box());
        assertArrayEquals(new int[]{7, 15, 7, 9, 16, 9}, new PlantBounds(8, 0, 9, 1).box());
    }

    /** Like vanilla's hanging roots, whose picture is drawn in rows 0 to 10. */
    @Test
    void aHangingPictureHangsFromTheTop() {
        int[] box = new PlantBounds(0, 0, 16, 11).box();
        assertEquals(5, box[1]);
        assertEquals(16, box[4]);
    }

    @Test
    void aPictureOnTheBottomRowMovesUpToTheTopKeepingItsSize() {
        PlantBounds standing = new PlantBounds(6, 6, 10, 16);
        PlantBounds hanging = standing.atTop();
        assertEquals(new PlantBounds(6, 0, 10, 10), hanging);
        assertArrayEquals(new int[]{standing.box()[0], 6, standing.box()[2], standing.box()[3], 16, standing.box()[5]}, hanging.box());
    }

    @Test
    void aPictureAlreadyOnTheTopRowStaysAndAFullOneFillsBothEnds() {
        PlantBounds onTop = new PlantBounds(2, 0, 14, 7);
        assertEquals(onTop, onTop.atTop());
        PlantBounds full = new PlantBounds(7, 0, 9, 16);
        assertEquals(full, full.atTop());
    }

    /** A picture floating in the middle has clear rows on both ends; only the ones above it count. */
    @Test
    void aPictureInTheMiddleMovesUpByTheRowsAboveIt() {
        assertEquals(new PlantBounds(3, 0, 13, 8), new PlantBounds(3, 5, 13, 13).atTop());
    }

    /** A column covers 0.9 of a sixteenth, rounded half up: 0.9, 1.8, 2.7, 3.6, 4.5, 5.4, 6.3, 7.2. */
    @Test
    void theFartherSideSetsACenteredWidth() {
        int[] halves = {1, 2, 3, 4, 5, 5, 6, 7};
        for (int reach = 1; reach <= 8; reach++) {
            int[] box = new PlantBounds(8 - reach, 0, 8, 16).box();
            assertEquals(8 - halves[reach - 1], box[0], "reach " + reach);
            assertEquals(8 + halves[reach - 1], box[3], "reach " + reach);
            assertEquals(box[0], box[2]);
            assertEquals(box[3], box[5]);
        }
        assertArrayEquals(new int[]{3, 0, 3, 13, 16, 13}, new PlantBounds(2, 0, 10, 16).box());
        assertArrayEquals(new int[]{4, 0, 4, 12, 16, 12}, new PlantBounds(5, 0, 12, 16).box()); // poppy's columns
    }

    @Test
    void boundsSurviveATagAndBadOnesAreDropped() {
        PlantBounds bounds = new PlantBounds(1, 2, 15, 16);
        assertEquals(bounds, PlantBounds.fromTag(bounds.toTag()));
        assertNull(PlantBounds.fromTag(new int[]{1, 2, 15}));
        assertNull(PlantBounds.fromTag(new int[]{1, 2, 15, 16, 0}));
        assertNull(PlantBounds.fromTag(new int[]{8, 0, 8, 16}));
        assertNull(PlantBounds.fromTag(new int[]{0, 5, 16, 5}));
        assertNull(PlantBounds.fromTag(new int[]{0, 0, 17, 16}));
        assertNull(PlantBounds.fromTag(new int[]{-1, 0, 16, 16}));
        assertNull(PlantBounds.fromTag(new int[]{0, -1, 16, 16}));
        assertNull(PlantBounds.fromTag(new int[]{0, 0, 16, 17}));
        assertNull(PlantBounds.fromTag(new int[0]));
    }
}
