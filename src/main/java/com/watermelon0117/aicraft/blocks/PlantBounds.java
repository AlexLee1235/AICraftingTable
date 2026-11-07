package com.watermelon0117.aicraft.blocks;

import org.jetbrains.annotations.Nullable;

import java.awt.image.BufferedImage;

/**
 * Where a plant's picture is drawn, in its own pixels, which sets the plant's outline. Kept as the picture's bounds
 * rather than the outline, so a later change to how one becomes the other needs no change to stored blocks.
 *
 * @param right  one past the rightmost drawn column
 * @param bottom one past the lowest drawn row
 */
public record PlantBounds(int left, int top, int right, int bottom) {
    private static final int SIZE = 16;
    /** The lowest alpha a plant shows: vanilla's cutout shader discards anything below 0.1. */
    static final int MIN_DRAWN_ALPHA = 26;

    public PlantBounds {
        if (left < 0 || left >= right || right > SIZE || top < 0 || top >= bottom || bottom > SIZE) {
            throw new IllegalArgumentException("Not bounds in a 16x16 picture: " + left + ", " + top + ", " + right + ", " + bottom);
        }
    }

    /** @return the bounds of the pixels the plant shows, or null when it shows none */
    @Nullable
    public static PlantBounds of(BufferedImage picture) {
        if (picture.getWidth() != SIZE || picture.getHeight() != SIZE) {
            throw new IllegalArgumentException("The plant picture is " + picture.getWidth() + "x" + picture.getHeight() + ", not 16x16");
        }
        int left = SIZE, top = SIZE, right = 0, bottom = 0;
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                if (picture.getRGB(x, y) >>> 24 >= MIN_DRAWN_ALPHA) {
                    left = Math.min(left, x);
                    top = Math.min(top, y);
                    right = Math.max(right, x + 1);
                    bottom = Math.max(bottom, y + 1);
                }
            }
        }
        return right == 0 ? null : new PlantBounds(left, top, right, bottom);
    }

    /** @return the bounds a tag holds, or null when they are not bounds in a 16x16 picture, as a command may have written */
    @Nullable
    public static PlantBounds fromTag(int[] values) {
        if (values.length != 4) {
            return null;
        }
        try {
            return new PlantBounds(values[0], values[1], values[2], values[3]);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public int[] toTag() {
        return new int[]{left, top, right, bottom};
    }

    /**
     * The outline in sixteenths of a block, as minX, minY, minZ, maxX, maxY, maxZ.
     * <ul>
     *     <li>The plant copies vanilla's cross model, whose two diagonal planes run from 0.8 to 15.2 in both x and z,
     *     so a column of the picture covers 0.9 of a sixteenth there. The half width is rounded to whole sixteenths.</li>
     *     <li>Each plane shows the picture mirrored on its back, so what is drawn is centered in x and z.</li>
     *     <li>The planes are 16 tall, so a row is a sixteenth, counted from the top.</li>
     * </ul>
     */
    public int[] box() {
        int reach = Math.max(8 - left, right - 8);
        int half = (9 * reach + 5) / 10;
        return new int[]{8 - half, SIZE - bottom, 8 - half, 8 + half, SIZE - top, 8 + half};
    }
}
