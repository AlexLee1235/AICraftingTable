package com.watermelon0117.aicraft.common;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlockSheetTest {
    private static final int[] EVEN = widths(32);
    /** Within the 20 to 48px each cell may take in a 512px square, and adding up to 512. */
    private static final int[] UNEVEN = {24, 40, 24, 40, 24, 40, 24, 40, 40, 24, 40, 24, 40, 24, 40, 24};

    @Test
    void returnsEachSpriteDrawnOnAnEvenGrid() {
        int[][] sprites = {sprite(1), sprite(2), sprite(3)};
        List<byte[]> faces = BlockSheet.process(png(sheet(sprites, EVEN, EVEN)));

        assertEquals(3, faces.size());
        for (int i = 0; i < 3; i++) {
            assertArrayEquals(sprites[i], pixels(faces.get(i)), "square " + i);
        }
    }

    @Test
    void cutsFollowAnUnevenDrawing() {
        int[][] sprites = {sprite(4), sprite(5), sprite(6)};
        List<byte[]> faces = BlockSheet.process(png(sheet(sprites, UNEVEN, EVEN)));

        for (int i = 0; i < 3; i++) {
            assertArrayEquals(sprites[i], pixels(faces.get(i)), "square " + i);
        }
    }

    @Test
    void keepsOneCellLinesOfA24CellDrawingAtFullStrength() {
        int wood = 0xFFC89650, line = 0xFF462814;
        int[] square = new int[512 * 512];
        for (int y = 0; y < 512; y++) {
            for (int x = 0; x < 512; x++) {
                int cellX = x * 24 / 512, cellY = y * 24 / 512;
                boolean onLine = cellX == 5 || cellX == 11 || cellX == 17 || cellY == 5 || cellY == 11 || cellY == 17;
                square[y * 512 + x] = onLine ? line : wood;
            }
        }
        BufferedImage image = new BufferedImage(1536, 512, BufferedImage.TYPE_INT_ARGB);
        for (int left = 0; left < 1536; left += 512) {
            image.setRGB(left, 0, 512, 512, square, 0, 512);
        }

        int[] face = pixels(BlockSheet.process(png(image)).get(0));

        Set<Integer> colors = new HashSet<>();
        for (int pixel : face) {
            colors.add(pixel);
        }
        assertEquals(Set.of(wood, line), colors);
        int fullRows = 0, fullColumns = 0;
        for (int k = 0; k < 16; k++) {
            boolean row = true, column = true;
            for (int other = 0; other < 16; other++) {
                row &= face[k * 16 + other] == line;
                column &= face[other * 16 + k] == line;
            }
            fullRows += row ? 1 : 0;
            fullColumns += column ? 1 : 0;
        }
        assertEquals(3, fullRows);
        assertEquals(3, fullColumns);
    }

    @Test
    void givesTheSameResultEachTime() {
        byte[] noise = png(noiseSheet(7));

        List<byte[]> first = BlockSheet.process(noise);
        List<byte[]> second = BlockSheet.process(noise);

        for (int i = 0; i < 3; i++) {
            assertArrayEquals(first.get(i), second.get(i));
        }
    }

    @Test
    void readsJpegAsWellAsPng() throws IOException {
        int[][] sprites = {sprite(8), sprite(9), sprite(10)};
        BufferedImage opaque = new BufferedImage(1536, 512, BufferedImage.TYPE_INT_RGB);
        opaque.getGraphics().drawImage(sheet(sprites, EVEN, EVEN), 0, 0, null);
        ByteArrayOutputStream jpeg = new ByteArrayOutputStream();
        assertTrue(ImageIO.write(opaque, "jpeg", jpeg));

        List<byte[]> faces = BlockSheet.process(jpeg.toByteArray());

        assertEquals(3, faces.size());
        for (int i = 0; i < 3; i++) {
            int[] face = pixels(faces.get(i));
            assertEquals(256, face.length);
            for (int k = 0; k < 256; k++) {
                assertEquals(0xFF, face[k] >>> 24);
                for (int shift = 0; shift <= 16; shift += 8) {
                    int difference = Math.abs((face[k] >> shift & 0xFF) - (sprites[i][k] >> shift & 0xFF));
                    assertTrue(difference <= 32, "pixel " + k + " of square " + i + " is off by " + difference);
                }
            }
        }
    }

    @Test
    void facesAreOpaqueEvenWhenTheSheetIsNot() {
        BufferedImage image = new BufferedImage(1536, 512, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 512; y++) {
            for (int x = 0; x < 1536; x++) {
                image.setRGB(x, y, 0x40336699);
            }
        }

        for (byte[] face : BlockSheet.process(png(image))) {
            BufferedImage decoded = LegacyModEncoder.readLikeMod(face);
            assertEquals(16, decoded.getWidth());
            assertEquals(16, decoded.getHeight());
            for (int pixel : decoded.getRGB(0, 0, 16, 16, null, 0, 16)) {
                assertEquals(0xFF336699, pixel);
            }
        }
    }

    @Test
    void keepsClearHalfClearAndSolidCellsWhenAsked() {
        int[][] sprites = {seeThroughSprite(11), seeThroughSprite(12), seeThroughSprite(13)};
        List<byte[]> faces = BlockSheet.process(png(sheet(sprites, EVEN, EVEN)), true);

        for (int i = 0; i < 3; i++) {
            assertArrayEquals(sprites[i], pixels(faces.get(i)), "square " + i);
        }
    }

    @Test
    void cutsFollowAChangeOfAlphaAlone() {
        // One color, so only alpha tells the cells apart.
        int[][] sprites = new int[3][];
        for (int square = 0; square < 3; square++) {
            Random random = new Random(20 + square);
            sprites[square] = new int[256];
            for (int k = 0; k < 256; k++) {
                sprites[square][k] = new int[]{0, 0x66, 0xA3, 0xFF}[random.nextInt(4)] << 24 | 0x336699;
                if (sprites[square][k] >>> 24 == 0) {
                    sprites[square][k] = 0;
                }
            }
        }
        List<byte[]> faces = BlockSheet.process(png(sheet(sprites, UNEVEN, EVEN)), true);

        for (int i = 0; i < 3; i++) {
            assertArrayEquals(sprites[i], pixels(faces.get(i)), "square " + i);
        }
    }

    @Test
    void anAlmostSolidCellIsSolidAndAnAlmostClearOneIsClear() {
        BufferedImage image = new BufferedImage(1536, 512, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 512; y++) {
            for (int x = 0; x < 1536; x++) {
                int alpha = x < 512 ? BlockSheet.SOLID_ALPHA : x < 1024 ? BlockSheet.CLEAR_ALPHA : BlockSheet.CLEAR_ALPHA + 1;
                image.setRGB(x, y, alpha << 24 | 0x336699);
            }
        }

        List<byte[]> faces = BlockSheet.process(png(image), true);

        assertEquals(0xFF336699, pixels(faces.get(0))[100]);
        assertEquals(0, pixels(faces.get(1))[100]);
        assertEquals((BlockSheet.CLEAR_ALPHA + 1) << 24 | 0x336699, pixels(faces.get(2))[100]);
    }

    @Test
    void keepingAlphaChangesNothingOnAnOpaqueSheet() {
        byte[] noise = png(noiseSheet(8));

        List<byte[]> opaque = BlockSheet.process(noise);
        List<byte[]> kept = BlockSheet.process(noise, true);

        for (int i = 0; i < 3; i++) {
            assertArrayEquals(pixels(opaque.get(i)), pixels(kept.get(i)));
        }
    }

    @Test
    void rejectsASheetOfAnyOtherSize() {
        byte[] square = png(new BufferedImage(1024, 1024, BufferedImage.TYPE_INT_RGB));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> BlockSheet.process(square));

        assertEquals("Expected a 1536x512 block sheet, got 1024x1024", error.getMessage());
    }

    @Test
    void rejectsAnUnreadableSheet() {
        assertThrows(IllegalArgumentException.class, () -> BlockSheet.process(new byte[]{1, 2, 3, 4}));
    }

    private static int[] widths(int width) {
        int[] widths = new int[16];
        java.util.Arrays.fill(widths, width);
        return widths;
    }

    /** Colors far apart, so that every neighboring cell has a real edge. */
    private static int[] sprite(long seed) {
        Random random = new Random(seed);
        int[] out = new int[256];
        for (int i = 0; i < 256; i++) {
            out[i] = 0xFF000000 | (random.nextInt(6) * 51) << 16 | (random.nextInt(6) * 51) << 8 | random.nextInt(6) * 51;
        }
        return out;
    }

    /** Like {@link #sprite}, with stained glass's alphas of 102 and 163 among clear and solid cells; a clear cell has no color. */
    private static int[] seeThroughSprite(long seed) {
        Random random = new Random(seed);
        int[] out = sprite(seed);
        for (int i = 0; i < 256; i++) {
            int alpha = new int[]{0, 102, 163, 255}[random.nextInt(4)];
            out[i] = alpha == 0 ? 0 : alpha << 24 | (out[i] & 0xFFFFFF);
        }
        return out;
    }

    private static BufferedImage sheet(int[][] sprites, int[] columnWidths, int[] rowHeights) {
        BufferedImage image = new BufferedImage(1536, 512, BufferedImage.TYPE_INT_ARGB);
        int[] xs = lookup(columnWidths), ys = lookup(rowHeights);
        for (int square = 0; square < 3; square++) {
            for (int y = 0; y < 512; y++) {
                for (int x = 0; x < 512; x++) {
                    image.setRGB(square * 512 + x, y, sprites[square][ys[y] * 16 + xs[x]]);
                }
            }
        }
        return image;
    }

    private static int[] lookup(int[] widths) {
        List<Integer> cells = new ArrayList<>();
        for (int cell = 0; cell < 16; cell++) {
            for (int k = 0; k < widths[cell]; k++) {
                cells.add(cell);
            }
        }
        assertEquals(512, cells.size());
        return cells.stream().mapToInt(Integer::intValue).toArray();
    }

    private static BufferedImage noiseSheet(long seed) {
        Random random = new Random(seed);
        BufferedImage image = new BufferedImage(1536, 512, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < 512; y++) {
            for (int x = 0; x < 1536; x++) {
                image.setRGB(x, y, random.nextInt(0x1000000));
            }
        }
        return image;
    }

    private static byte[] png(BufferedImage image) {
        return LegacyModEncoder.toPng(image);
    }

    private static int[] pixels(byte[] png) {
        BufferedImage image = LegacyModEncoder.readLikeMod(png);
        return image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
    }
}
