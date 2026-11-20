package com.watermelon0117.aicraft.common;

import java.awt.image.BufferedImage;
import java.util.Random;

/** Test sprites and model-like renderings of them. */
final class SyntheticSprites {
    private SyntheticSprites() {
    }

    /** A sprite of random opaque colors with a dark outline; {@code shape} marks opaque cells. */
    static BufferedImage sprite(boolean[][] shape, long seed) {
        Random random = new Random(seed);
        int h = shape.length, w = shape[0].length;
        BufferedImage sprite = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (!shape[y][x]) {
                    continue;
                }
                boolean edge = y == 0 || x == 0 || y == h - 1 || x == w - 1
                        || !shape[y - 1][x] || !shape[y + 1][x] || !shape[y][x - 1] || !shape[y][x + 1];
                int rgb = edge ? 0x1A1410 : 0x404040 + random.nextInt(0xBFBFBF);
                sprite.setRGB(x, y, 0xFF000000 | rgb);
            }
        }
        return sprite;
    }

    static boolean[][] filled(int w, int h) {
        boolean[][] shape = new boolean[h][w];
        for (boolean[] row : shape) {
            java.util.Arrays.fill(row, true);
        }
        return shape;
    }

    static boolean[][] diamond(int size) {
        boolean[][] shape = new boolean[size][size];
        double c = (size - 1) / 2.0;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                shape[y][x] = Math.abs(x - c) + Math.abs(y - c) <= c + 0.5;
            }
        }
        return shape;
    }

    /**
     * Draws the sprite like a transparent model image: each cell a {@code cell}-pixel square at
     * {@code (left, top)}, alpha 0 elsewhere with random hidden RGB, as Sunburst PNGs have.
     */
    static BufferedImage modelImage(BufferedImage sprite, int cell, int left, int top, boolean transparent) {
        Random random = new Random(7);
        BufferedImage image = new BufferedImage(1024, 1024, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 1024; y++) {
            for (int x = 0; x < 1024; x++) {
                image.setRGB(x, y, transparent ? random.nextInt(0x1000000) : 0xFFFF00FF);
            }
        }
        for (int y = 0; y < sprite.getHeight() * cell; y++) {
            for (int x = 0; x < sprite.getWidth() * cell; x++) {
                int argb = sprite.getRGB(x / cell, y / cell);
                if ((argb >>> 24) != 0) {
                    image.setRGB(left + x, top + y, argb);
                }
            }
        }
        return image;
    }

    /** True when both images have the same opaque cells with identical colors. */
    static boolean sameContent(BufferedImage a, BufferedImage b) {
        int[] boxA = LegacyModEncoder.contentBox(a), boxB = LegacyModEncoder.contentBox(b);
        if (boxA == null || boxB == null || boxA[2] != boxB[2] || boxA[3] != boxB[3]) {
            return false;
        }
        for (int y = 0; y < boxA[3]; y++) {
            for (int x = 0; x < boxA[2]; x++) {
                int pa = a.getRGB(boxA[0] + x, boxA[1] + y), pb = b.getRGB(boxB[0] + x, boxB[1] + y);
                boolean oa = (pa >>> 24) != 0, ob = (pb >>> 24) != 0;
                if (oa != ob || (oa && (pa | 0xFF000000) != (pb | 0xFF000000))) {
                    return false;
                }
            }
        }
        return true;
    }
}
