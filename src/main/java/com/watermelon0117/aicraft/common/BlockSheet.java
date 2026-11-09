package com.watermelon0117.aicraft.common;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Turns one model image of three squares in a row into three 16x16 block faces, opaque unless asked to keep alpha.
 *
 * <p>Java port of {@code split_sheet} and {@code snap_down} in
 * {@code test/block_texture_16x16/experiment.py}. Image models rarely draw the 16 cells they are
 * asked for, and an even grid splits their one-cell lines into half tones, so 16 uneven cuts are
 * fitted to the edges the model really drew.
 *
 * <p>Coordinates are those of the experiment, where each square was doubled to 1024px. Keeping them
 * gives the same cuts and insets without holding the doubled image.
 *
 * <p>Copied from the texture server's pixelizer, so that an own OpenAI key gets the same pictures: change both.
 */
public final class BlockSheet {
    private static final int SIDE = 16;
    private static final int SQUARE = 512;
    private static final int SHEET_WIDTH = 3 * SQUARE;
    private static final int SOURCE_SIDE = 2 * SQUARE;
    /** The cuts are searched on every 4th source pixel. */
    private static final int UNIT = 4;
    private static final int COLUMNS = SOURCE_SIDE / UNIT;
    private static final int NOMINAL_WIDTH = COLUMNS / SIDE;
    // Wider limits distorted proportions in the experiment, and narrower ones split lines again.
    private static final int MIN_WIDTH = 40 / UNIT;
    private static final int MAX_WIDTH = 96 / UNIT;
    /** A tiny pull toward equal widths settles ties. */
    private static final double WIDTH_PULL = 1e-3;
    private static final double CENTER_INSET_FRACTION = 0.18;
    /** A see-through cell whose median alpha is at or below this is clear. */
    static final int CLEAR_ALPHA = 8;
    /** At or above this a see-through cell is solid: on a transparent sheet the model's solid parts stop just short of 255. */
    static final int SOLID_ALPHA = 247;
    /** Gray that clear pixels are laid over before the cuts are searched, whose colors are whatever the model left there. */
    private static final int UNDER_CLEAR = 128;

    // OKLab's two matrices.
    private static final double[][] M1 = {
            {0.4122214708, 0.5363325363, 0.0514459929},
            {0.2119034982, 0.6806995451, 0.1073969566},
            {0.0883024619, 0.2817188376, 0.6299787005}};
    private static final double[][] M2 = {
            {0.2104542553, 0.7936177850, -0.0040720468},
            {1.9779984951, -2.4285922050, 0.4505937099},
            {0.0259040371, 0.7827717662, -0.8086757660}};

    private BlockSheet() {
    }

    /**
     * @param image the model's PNG or JPEG
     * @return one opaque PNG per square, left to right
     */
    public static List<byte[]> process(byte[] image) {
        return process(image, false);
    }

    /**
     * @param image the model's PNG or JPEG
     * @param keepAlpha for a see-through block: each cell keeps the median alpha of its middle, so clear, half-clear and
     *                  solid parts stay as drawn, and the cuts follow changes of alpha too; otherwise every face is opaque
     * @return one PNG per square, left to right
     */
    public static List<byte[]> process(byte[] image, boolean keepAlpha) {
        BufferedImage sheet = LegacyModEncoder.readLikeMod(image);
        if (sheet == null) {
            throw new IllegalArgumentException("Block sheet is unreadable");
        }
        if (sheet.getWidth() != SHEET_WIDTH || sheet.getHeight() != SQUARE) {
            throw new IllegalArgumentException("Expected a " + SHEET_WIDTH + "x" + SQUARE + " block sheet, got "
                    + sheet.getWidth() + "x" + sheet.getHeight());
        }
        int[] argb = sheet.getRGB(0, 0, SHEET_WIDTH, SQUARE, null, 0, SHEET_WIDTH);
        List<byte[]> faces = new ArrayList<>(3);
        for (int square = 0; square < 3; square++) {
            faces.add(LegacyModEncoder.toPng(snapDown(argb, square * SQUARE, keepAlpha)));
        }
        return faces;
    }

    private static BufferedImage snapDown(int[] argb, int squareLeft, boolean keepAlpha) {
        int channels = keepAlpha ? 4 : 3;
        double[] lab = sampleLab(argb, squareLeft, keepAlpha);
        int[] xs = cuts(lab, false, channels);
        int[] ys = cuts(lab, true, channels);
        BufferedImage face = new BufferedImage(SIDE, SIDE, BufferedImage.TYPE_INT_ARGB);
        for (int j = 0; j < SIDE; j++) {
            int insetY = (int) ((ys[j + 1] - ys[j]) * CENTER_INSET_FRACTION);
            for (int i = 0; i < SIDE; i++) {
                int insetX = (int) ((xs[i + 1] - xs[i]) * CENTER_INSET_FRACTION);
                List<int[]> selected = new ArrayList<>();
                List<Integer> alphas = keepAlpha ? new ArrayList<>() : null;
                for (int y = ys[j] + insetY; y < ys[j + 1] - insetY; y++) {
                    for (int x = xs[i] + insetX; x < xs[i + 1] - insetX; x++) {
                        int pixel = argb[(y / 2) * SHEET_WIDTH + squareLeft + x / 2];
                        if (keepAlpha) {
                            alphas.add(pixel >>> 24);
                            // A clear pixel's color is whatever the model left there.
                            if (pixel >>> 24 <= CLEAR_ALPHA) {
                                continue;
                            }
                        }
                        selected.add(new int[]{pixel >> 16 & 0xFF, pixel >> 8 & 0xFF, pixel & 0xFF});
                    }
                }
                face.setRGB(i, j, keepAlpha ? seeThroughCell(selected, alphas) : opaqueCell(selected));
            }
        }
        return face;
    }

    private static int opaqueCell(List<int[]> selected) {
        int[] c = SpritePixelizer.representativeColor(selected);
        return 0xFF000000 | (c[0] << 16) | (c[1] << 8) | c[2];
    }

    private static int seeThroughCell(List<int[]> seen, List<Integer> alphas) {
        alphas.sort(null);
        int alpha = alphas.get((alphas.size() - 1) / 2);
        if (alpha <= CLEAR_ALPHA || seen.isEmpty()) {
            return 0;
        }
        int[] c = SpritePixelizer.representativeColor(seen);
        return (alpha >= SOLID_ALPHA ? 0xFF : alpha) << 24 | (c[0] << 16) | (c[1] << 8) | c[2];
    }

    /**
     * OKLab of every {@link #UNIT}th source pixel, row by row, three values each. With alpha, each pixel is first laid
     * over gray and its alpha is a fourth value, so a frame against clear glass is an edge whatever its color.
     */
    private static double[] sampleLab(int[] argb, int squareLeft, boolean keepAlpha) {
        int channels = keepAlpha ? 4 : 3;
        double[] lab = new double[COLUMNS * COLUMNS * channels];
        int step = UNIT / 2;
        for (int row = 0; row < COLUMNS; row++) {
            for (int column = 0; column < COLUMNS; column++) {
                int pixel = argb[row * step * SHEET_WIDTH + squareLeft + column * step];
                int offset = (row * COLUMNS + column) * channels;
                if (keepAlpha) {
                    double alpha = (pixel >>> 24) / 255.0;
                    writeOklab(overGray(pixel, alpha), lab, offset);
                    lab[offset + 3] = alpha;
                } else {
                    writeOklab(pixel, lab, offset);
                }
            }
        }
        return lab;
    }

    private static int overGray(int pixel, double alpha) {
        int out = 0;
        for (int shift = 0; shift <= 16; shift += 8) {
            int value = (int) Math.round((pixel >> shift & 0xFF) * alpha + UNDER_CLEAR * (1 - alpha));
            out |= value << shift;
        }
        return out;
    }

    static void writeOklab(int pixel, double[] out, int offset) {
        double r = linear((pixel >> 16 & 0xFF) / 255.0);
        double g = linear((pixel >> 8 & 0xFF) / 255.0);
        double b = linear((pixel & 0xFF) / 255.0);
        double l = Math.cbrt(M1[0][0] * r + M1[0][1] * g + M1[0][2] * b);
        double m = Math.cbrt(M1[1][0] * r + M1[1][1] * g + M1[1][2] * b);
        double s = Math.cbrt(M1[2][0] * r + M1[2][1] * g + M1[2][2] * b);
        out[offset] = M2[0][0] * l + M2[0][1] * m + M2[0][2] * s;
        out[offset + 1] = M2[1][0] * l + M2[1][1] * m + M2[1][2] * s;
        out[offset + 2] = M2[2][0] * l + M2[2][1] * m + M2[2][2] * s;
    }

    private static double linear(double c) {
        return c <= 0.04045 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
    }

    /**
     * Splits one axis into {@value #SIDE} runs with the least color variance inside them.
     *
     * @param alongRows false to cut columns, true to cut rows
     * @param channels values per sample in {@code lab}
     * @return the 17 cut positions in source pixels
     */
    private static int[] cuts(double[] lab, boolean alongRows, int channels) {
        // Prefix sums along the axis being cut: per line and channel, then of the squares.
        int lineValues = COLUMNS * channels;
        double[] first = new double[(COLUMNS + 1) * lineValues];
        double[] second = new double[COLUMNS + 1];
        for (int position = 0; position < COLUMNS; position++) {
            double squares = 0;
            for (int line = 0; line < COLUMNS; line++) {
                int at = (alongRows ? position * COLUMNS + line : line * COLUMNS + position) * channels;
                for (int channel = 0; channel < channels; channel++) {
                    double value = lab[at + channel];
                    int k = line * channels + channel;
                    first[(position + 1) * lineValues + k] = first[position * lineValues + k] + value;
                    squares += value * value;
                }
            }
            second[position + 1] = second[position] + squares;
        }

        double[][] best = new double[SIDE + 1][COLUMNS + 1];
        int[][] back = new int[SIDE + 1][COLUMNS + 1];
        for (double[] counts : best) {
            Arrays.fill(counts, Double.POSITIVE_INFINITY);
        }
        best[0][0] = 0.0;
        double[] spread = new double[MAX_WIDTH + 1];
        for (int end = MIN_WIDTH; end <= COLUMNS; end++) {
            int firstStart = Math.max(0, end - MAX_WIDTH);
            int lastStart = end - MIN_WIDTH;
            for (int start = firstStart; start <= lastStart; start++) {
                int width = end - start;
                double sums = 0;
                for (int k = 0; k < lineValues; k++) {
                    double difference = first[end * lineValues + k] - first[start * lineValues + k];
                    sums += difference * difference;
                }
                double off = width - NOMINAL_WIDTH;
                spread[start - firstStart] = (second[end] - second[start]) - sums / width + WIDTH_PULL * off * off;
            }
            for (int count = 1; count <= SIDE; count++) {
                double smallest = Double.POSITIVE_INFINITY;
                int pick = firstStart;
                for (int start = firstStart; start <= lastStart; start++) {
                    double total = best[count - 1][start] + spread[start - firstStart];
                    if (total < smallest) {
                        smallest = total;
                        pick = start;
                    }
                }
                best[count][end] = smallest;
                back[count][end] = pick;
            }
        }

        int[] cuts = new int[SIDE + 1];
        int end = COLUMNS;
        cuts[SIDE] = end * UNIT;
        for (int count = SIDE; count > 0; count--) {
            end = back[count][end];
            cuts[count - 1] = end * UNIT;
        }
        return cuts;
    }
}
