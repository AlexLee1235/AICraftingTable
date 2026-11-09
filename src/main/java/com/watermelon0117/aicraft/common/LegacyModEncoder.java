package com.watermelon0117.aicraft.common;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Encodes a finished sprite as a 1024x1024 image that the released mods turn back into
 * exactly that sprite.
 *
 * <p>Installed mods still run their own {@code ImageGridProcessor} on whatever this server
 * returns: Canny edges, a background color estimated outside the edge bounding box (only
 * exact matches are removed when that background is perfectly flat), grid lines from edge
 * histograms, then one color per cell. So the sprite is enlarged by nearest neighbor onto a
 * flat background color it never uses, saved losslessly as PNG, and every candidate encoding
 * is run through {@link ImageGridProcessor} before it is accepted.
 *
 * <p>Copied from the texture server's pixelizer, so that an own OpenAI key gets the same pictures: change both.
 */
public final class LegacyModEncoder {
    static final int CANVAS = 1024;
    private static final int MARGIN_CELLS = 2;
    private static final int[] BACKGROUNDS = {
            0xFF00FF, 0x00FFFF, 0x00FF00, 0xFFFF00, 0x0000FF, 0xFF0000, 0xFFFFFF, 0x000000};

    /**
     * @param png           image to return to the mod
     * @param playerTexture what the mod will produce from it (verified)
     * @param seams         whether cells are separated by 1px background seams
     */
    public record Encoded(byte[] png, BufferedImage playerTexture, int cellSize, int background, boolean seams) {
    }

    private LegacyModEncoder() {
    }

    public static Optional<Encoded> encode(BufferedImage sprite) {
        int[] box = contentBox(sprite);
        if (box == null) {
            return Optional.empty();
        }
        BufferedImage expected = expectedModOutput(sprite, box);
        List<Integer> backgrounds = rankBackgrounds(sprite, box);
        // Plain enlargement first; seams give every cell its own edges when neighbors share colors.
        for (boolean seams : new boolean[]{false, true}) {
            for (int background : backgrounds.subList(0, Math.min(2, backgrounds.size()))) {
                BufferedImage canvas = render(sprite, box, background, seams);
                byte[] png = toPng(canvas);
                BufferedImage produced = ImageGridProcessor.tryProcess(readLikeMod(png));
                if (produced != null && sameTexture(produced, expected)) {
                    return Optional.of(new Encoded(png, expected, cellSize(box), background, seams));
                }
            }
        }
        return Optional.empty();
    }

    /** {xMin, yMin, width, height} of the opaque cells, or null when the sprite is empty. */
    static int[] contentBox(BufferedImage sprite) {
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, maxX = -1, maxY = -1;
        for (int y = 0; y < sprite.getHeight(); y++) {
            for (int x = 0; x < sprite.getWidth(); x++) {
                if ((sprite.getRGB(x, y) >>> 24) != 0) {
                    minX = Math.min(minX, x);
                    maxX = Math.max(maxX, x);
                    minY = Math.min(minY, y);
                    maxY = Math.max(maxY, y);
                }
            }
        }
        return maxX < 0 ? null : new int[]{minX, minY, maxX - minX + 1, maxY - minY + 1};
    }

    /** The mod crops to the content and pads to ceil8(max(w, h) + 1), at least 16, centered. */
    static BufferedImage expectedModOutput(BufferedImage sprite, int[] box) {
        int w = box[2], h = box[3];
        int side = ImageGridProcessor.calcPaddingSize(w, h, 1);
        BufferedImage out = new BufferedImage(side, side, BufferedImage.TYPE_INT_ARGB);
        int left = (side - w) / 2, top = (side - h) / 2;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int argb = sprite.getRGB(box[0] + x, box[1] + y);
                out.setRGB(left + x, top + y, (argb >>> 24) == 0 ? 0 : argb | 0xFF000000);
            }
        }
        return out;
    }

    static int cellSize(int[] box) {
        return Math.min(64, CANVAS / (Math.max(box[2], box[3]) + 2 * MARGIN_CELLS));
    }

    static BufferedImage render(BufferedImage sprite, int[] box, int background, boolean seams) {
        int cell = cellSize(box);
        int left = (CANVAS - box[2] * cell) / 2, top = (CANVAS - box[3] * cell) / 2;
        int inset = seams ? 1 : 0;
        BufferedImage canvas = new BufferedImage(CANVAS, CANVAS, BufferedImage.TYPE_INT_RGB);
        int[] row = new int[CANVAS];
        java.util.Arrays.fill(row, background);
        for (int y = 0; y < CANVAS; y++) {
            canvas.setRGB(0, y, CANVAS, 1, row, 0, CANVAS);
        }
        for (int cy = 0; cy < box[3]; cy++) {
            for (int cx = 0; cx < box[2]; cx++) {
                int argb = sprite.getRGB(box[0] + cx, box[1] + cy);
                if ((argb >>> 24) == 0) {
                    continue;
                }
                int x0 = left + cx * cell + inset, y0 = top + cy * cell + inset, size = cell - 2 * inset;
                for (int y = y0; y < y0 + size; y++) {
                    for (int x = x0; x < x0 + size; x++) {
                        canvas.setRGB(x, y, argb & 0xFFFFFF);
                    }
                }
            }
        }
        return canvas;
    }

    /** Candidate backgrounds the sprite does not use, farthest from its palette first. */
    static List<Integer> rankBackgrounds(BufferedImage sprite, int[] box) {
        Set<Integer> palette = new HashSet<>();
        for (int y = box[1]; y < box[1] + box[3]; y++) {
            for (int x = box[0]; x < box[0] + box[2]; x++) {
                int argb = sprite.getRGB(x, y);
                if ((argb >>> 24) != 0) {
                    palette.add(argb & 0xFFFFFF);
                }
            }
        }
        List<int[]> scored = new ArrayList<>();
        for (int bg : BACKGROUNDS) {
            int nearest = Integer.MAX_VALUE;
            for (int c : palette) {
                int dr = (c >> 16 & 0xFF) - (bg >> 16 & 0xFF), dg = (c >> 8 & 0xFF) - (bg >> 8 & 0xFF), db = (c & 0xFF) - (bg & 0xFF);
                nearest = Math.min(nearest, dr * dr + dg * dg + db * db);
            }
            if (nearest > 0) {
                scored.add(new int[]{bg, nearest});
            }
        }
        scored.sort(Comparator.comparingInt((int[] s) -> -s[1]));
        List<Integer> result = new ArrayList<>();
        for (int[] s : scored) {
            result.add(s[0]);
        }
        return result;
    }

    static boolean sameTexture(BufferedImage actual, BufferedImage expected) {
        if (actual.getWidth() != expected.getWidth() || actual.getHeight() != expected.getHeight()) {
            return false;
        }
        for (int y = 0; y < expected.getHeight(); y++) {
            for (int x = 0; x < expected.getWidth(); x++) {
                int a = actual.getRGB(x, y), e = expected.getRGB(x, y);
                boolean opaque = (e >>> 24) != 0;
                if (opaque != ((a >>> 24) != 0) || (opaque && a != e)) {
                    return false;
                }
            }
        }
        return true;
    }

    /** Same decoding as the mod's {@code ImageGridProcessor.readImageFromBytes}. */
    static BufferedImage readLikeMod(byte[] bytes) {
        try {
            return ImageIO.read(new ByteArrayInputStream(bytes));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static byte[] toPng(BufferedImage image) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            if (!ImageIO.write(image, "png", out)) {
                throw new IllegalStateException("PNG writer is unavailable");
            }
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
