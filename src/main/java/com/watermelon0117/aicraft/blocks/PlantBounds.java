package com.watermelon0117.aicraft.blocks;

import org.jetbrains.annotations.Nullable;

import java.awt.image.BufferedImage;

/**
 * Where a plant's picture is drawn, in its own pixels, which sets the plant's outline. Kept as the picture's bounds
 * rather than the outline, so a later change to how one becomes the other needs no change to stored blocks.
 *
 * <p>The whole picture is one block, whatever its side: 16, or 24 or 32 when the plant keeps its item sprite's size.
 * The side is kept with the bounds, as the model must know it before the picture arrives.
 *
 * @param right  one past the rightmost drawn column
 * @param bottom one past the lowest drawn row
 * @param side   the picture's width and height
 */
public record PlantBounds(int left, int top, int right, int bottom, int side) {
    public static final int SIDE_16 = 16;
    /** Sixteenths of a block. */
    private static final int BLOCK = 16;
    /** The lowest alpha a plant shows: vanilla's cutout shader discards anything below 0.1. */
    static final int MIN_DRAWN_ALPHA = 26;

    public PlantBounds {
        if (!isPictureSide(side) || left < 0 || left >= right || right > side || top < 0 || top >= bottom || bottom > side) {
            throw new IllegalArgumentException("Not bounds in a " + side + "x" + side + " picture: " + left + ", " + top + ", " + right + ", " + bottom);
        }
    }

    /** Bounds in a 16x16 picture, the only size before plants kept their sprite's. */
    public PlantBounds(int left, int top, int right, int bottom) {
        this(left, top, right, bottom, SIDE_16);
    }

    /** @return whether a plant's picture can be this wide and high: the sides of an item sprite */
    public static boolean isPictureSide(int side) {
        return side == 16 || side == 24 || side == 32;
    }

    /** @return the bounds of the pixels the plant shows, or null when it shows none */
    @Nullable
    public static PlantBounds of(BufferedImage picture) {
        int side = picture.getWidth();
        if (picture.getHeight() != side || !isPictureSide(side)) {
            throw new IllegalArgumentException("The plant picture is " + picture.getWidth() + "x" + picture.getHeight() + ", not 16x16, 24x24 or 32x32");
        }
        int left = side, top = side, right = 0, bottom = 0;
        for (int y = 0; y < side; y++) {
            for (int x = 0; x < side; x++) {
                if (picture.getRGB(x, y) >>> 24 >= MIN_DRAWN_ALPHA) {
                    left = Math.min(left, x);
                    top = Math.min(top, y);
                    right = Math.max(right, x + 1);
                    bottom = Math.max(bottom, y + 1);
                }
            }
        }
        return right == 0 ? null : new PlantBounds(left, top, right, bottom, side);
    }

    /**
     * @param values four bounds in a 16x16 picture, or those four and the side
     * @return the bounds a tag holds, or null when they are not bounds in a picture a plant can have, as a command may have written
     */
    @Nullable
    public static PlantBounds fromTag(int[] values) {
        if (values.length != 4 && values.length != 5) {
            return null;
        }
        try {
            return new PlantBounds(values[0], values[1], values[2], values[3], values.length == 5 ? values[4] : SIDE_16);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** A 16x16 picture's bounds stay four numbers, as blocks made before other sides were kept. */
    public int[] toTag() {
        return side == SIDE_16 ? new int[]{left, top, right, bottom} : new int[]{left, top, right, bottom, side};
    }

    /** @return these bounds moved straight up, until the first row drawn is the picture's top row */
    public PlantBounds atTop() {
        return new PlantBounds(left, 0, right, bottom - top, side);
    }

    /**
     * The outline in sixteenths of a block, as minX, minY, minZ, maxX, maxY, maxZ.
     * <ul>
     *     <li>The plant copies vanilla's cross model, whose two diagonal planes run from 0.8 to 15.2 in both x and z,
     *     so a column of the picture covers 14.4 / side sixteenths there, 0.9 in a 16x16 picture. The half width is
     *     rounded half up to whole sixteenths.</li>
     *     <li>Each plane shows the picture mirrored on its back, so what is drawn is centered in x and z.</li>
     *     <li>The planes are 16 tall, so a row is 16 / side sixteenths, counted from the top. A row that ends inside a
     *     sixteenth takes all of it, so the outline holds every drawn row.</li>
     * </ul>
     */
    public int[] box() {
        int center = side / 2;
        int reach = Math.max(center - left, right - center);
        // reach * 14.4 / side, rounded half up, in whole numbers: for a side of 16 that is (9 * reach + 5) / 10.
        // At least 1, as one column of a 32x32 picture would round to an outline nothing can aim at.
        int half = Math.max(1, (288 * reach + 10 * side) / (20 * side));
        int minY = (side - bottom) * BLOCK / side;
        int maxY = ((side - top) * BLOCK + side - 1) / side;
        return new int[]{8 - half, minY, 8 - half, 8 + half, maxY, 8 + half};
    }
}
