package com.watermelon0117.aicraft.common;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Picks the item out of one model image of three drawings of it in a row, 1536x512, as test/item_sheet_3x tested it:
 * each 512 square goes through the unchanged {@link SpritePipeline} on its own.
 *
 * <p>The left square is used, as it was the most reliable in the test. A square whose drawing reaches its edge was cut
 * from the drawing beside it, or by the image's own edge, so only squares drawn whole are tried, left to right, when there
 * is one; the first that gives a sprite is the item. When none does, the first one tried returns the fallback that a
 * single image would have.
 *
 * <p>Copied from the texture server's pixelizer, so that an own OpenAI key gets the same pictures: change both.
 */
public final class ItemSheet {
    private static final int SQUARE = 512;
    private static final int SHEET_WIDTH = 3 * SQUARE;
    /** The transparent sheets carry alpha-1 haze; a drawn pixel is at least half opaque. */
    private static final int DRAWN_ALPHA = 128;

    /**
     * @param square    which square the item came from, 0 to 2 from the left
     * @param squarePng that square as PNG, as the pipeline read it
     */
    public record Result(int square, byte[] squarePng, SpritePipeline.Output output) {
    }

    private ItemSheet() {
    }

    /** @param image the model's PNG */
    public static Result process(byte[] image) {
        BufferedImage sheet = LegacyModEncoder.readLikeMod(image);
        if (sheet == null) {
            throw new IllegalArgumentException("Item sheet is unreadable");
        }
        if (sheet.getWidth() != SHEET_WIDTH || sheet.getHeight() != SQUARE) {
            throw new IllegalArgumentException("Expected a " + SHEET_WIDTH + "x" + SQUARE + " item sheet, got "
                    + sheet.getWidth() + "x" + sheet.getHeight());
        }
        List<Integer> order = new ArrayList<>(3);
        for (int square = 0; square < 3; square++) {
            if (drawnWhole(sheet, square)) {
                order.add(square);
            }
        }
        if (order.isEmpty()) {
            order = List.of(0, 1, 2);
        }
        Result first = null;
        for (int square : order) {
            byte[] png = LegacyModEncoder.toPng(square(sheet, square));
            Result result = new Result(square, png, SpritePipeline.process(Base64.getEncoder().encodeToString(png)));
            if (result.output().route() == SpritePipeline.Route.SPRITE) {
                return result;
            }
            if (first == null) {
                first = result;
            }
        }
        return first;
    }

    /** A copy of one square in the sheet's own pixel format, so its PNG holds exactly the sheet's pixels. */
    static BufferedImage square(BufferedImage sheet, int square) {
        ColorModel model = sheet.getColorModel();
        WritableRaster raster = model.createCompatibleWritableRaster(SQUARE, SQUARE);
        raster.setDataElements(0, 0, sheet.getRaster().createChild(square * SQUARE, 0, SQUARE, SQUARE, 0, 0, null));
        return new BufferedImage(model, raster, sheet.isAlphaPremultiplied(), null);
    }

    /** True when no drawn pixel lies on the square's border. An image without alpha is drawn everywhere. */
    static boolean drawnWhole(BufferedImage sheet, int square) {
        int left = square * SQUARE, right = left + SQUARE - 1;
        for (int i = 0; i < SQUARE; i++) {
            if (drawn(sheet, left + i, 0) || drawn(sheet, left + i, SQUARE - 1)
                    || drawn(sheet, left, i) || drawn(sheet, right, i)) {
                return false;
            }
        }
        return true;
    }

    private static boolean drawn(BufferedImage sheet, int x, int y) {
        return (sheet.getRGB(x, y) >>> 24) >= DRAWN_ALPHA;
    }
}
