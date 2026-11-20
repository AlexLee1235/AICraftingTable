package com.watermelon0117.aicraft.common;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Turns one model image of a plant into the 16x16 texture of a cross block.
 *
 * <p>An item sprite is 16, 24 or 32 cells with a margin all round, which would leave a plant floating above the
 * block it stands on. So the sprite is cut to the plant, shrunk to fit if it is larger than 16 cells, and stood on the
 * bottom row, centered because the two crossed quads meet in the middle of the texture.
 *
 * <p>The shrinking starts from the sprite rather than refitting a coarser grid to the model image: a coarse grid
 * needs a cell to be mostly covered, which drops thin stems, and a plant without its stem floats again.
 *
 * <p>Copied from the texture server's pixelizer, so that an own OpenAI key gets the same pictures: change both.
 */
public final class CrossTexture {
    private static final int SIDE = 16;
    /** Low enough to keep a line one sprite cell wide in a 32-cell sprite, which covers a quarter to a half of a target cell. */
    private static final double MIN_COVERAGE = 0.25;
    /** A cell's colors are weighted by how much of it they cover, in steps of this fraction. */
    private static final int WEIGHT_STEPS = 16;

    private CrossTexture() {
    }

    /**
     * @param image the model's PNG, with a transparent background
     * @return the 16x16 PNG, whose pixels are either opaque or fully transparent
     */
    public static byte[] process(byte[] image) {
        return process(image, false);
    }

    /** @param atTop hang it from the top row, for something that hangs, instead of standing it on the bottom row */
    public static byte[] process(byte[] image, boolean atTop) {
        BufferedImage model = LegacyModEncoder.readLikeMod(image);
        if (model == null) {
            throw new IllegalArgumentException("Plant image is unreadable");
        }
        SpritePixelizer.Result result = SpritePixelizer.pixelize(model)
                .orElseThrow(() -> new IllegalArgumentException("No plant found in the image"));
        return LegacyModEncoder.toPng(fromSprite(result.sprite(), atTop));
    }

    static BufferedImage fromSprite(BufferedImage sprite) {
        return fromSprite(sprite, false);
    }

    static BufferedImage fromSprite(BufferedImage sprite, boolean atTop) {
        int[] box = LegacyModEncoder.contentBox(sprite);
        int largest = Math.max(box[2], box[3]);
        double scale = Math.max(1.0, largest / (double) SIDE);
        BufferedImage shrunk = new BufferedImage(SIDE, SIDE, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < Math.ceil(box[3] / scale); y++) {
            for (int x = 0; x < Math.ceil(box[2] / scale); x++) {
                shrunk.setRGB(x, y, shrunkCell(sprite, box, scale, x, y));
            }
        }
        // Asked again, because a row at the edge can come out empty, and the plant must still touch the bottom.
        int[] plant = LegacyModEncoder.contentBox(shrunk);
        if (plant == null) {
            throw new IllegalArgumentException("The plant vanished when it was shrunk");
        }
        BufferedImage texture = new BufferedImage(SIDE, SIDE, BufferedImage.TYPE_INT_ARGB);
        int left = (SIDE - plant[2]) / 2, top = atTop ? 0 : SIDE - plant[3];
        for (int y = 0; y < plant[3]; y++) {
            for (int x = 0; x < plant[2]; x++) {
                texture.setRGB(left + x, top + y, shrunk.getRGB(plant[0] + x, plant[1] + y));
            }
        }
        return texture;
    }

    /** The sprite cells under one target cell: empty unless they cover enough of it, otherwise their representative color. */
    private static int shrunkCell(BufferedImage sprite, int[] box, double scale, int x, int y) {
        double x0 = x * scale, x1 = Math.min((x + 1) * scale, box[2]);
        double y0 = y * scale, y1 = Math.min((y + 1) * scale, box[3]);
        double covered = 0;
        List<int[]> colors = new ArrayList<>();
        for (int sy = (int) y0; sy < y1; sy++) {
            for (int sx = (int) x0; sx < x1; sx++) {
                int pixel = sprite.getRGB(box[0] + sx, box[1] + sy);
                if (pixel >>> 24 == 0) {
                    continue;
                }
                double overlap = (Math.min(sx + 1, x1) - Math.max(sx, x0)) * (Math.min(sy + 1, y1) - Math.max(sy, y0));
                covered += overlap;
                int[] color = {pixel >> 16 & 0xFF, pixel >> 8 & 0xFF, pixel & 0xFF};
                for (long copies = Math.max(1, Math.round(overlap * WEIGHT_STEPS)); copies > 0; copies--) {
                    colors.add(color);
                }
            }
        }
        if (covered < MIN_COVERAGE * scale * scale) {
            return 0;
        }
        int[] c = SpritePixelizer.representativeColor(colors);
        return 0xFF000000 | (c[0] << 16) | (c[1] << 8) | c[2];
    }
}
