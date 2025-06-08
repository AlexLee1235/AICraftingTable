package com.watermelon0117.aicraft;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * Tiny helper for turning a PNG into a flat RGBA byte array.
 *
 * Each pixel becomes four consecutive bytes in the order:
 *   index   0 → Red
 *   index   1 → Green
 *   index   2 → Blue
 *   index   3 → Alpha
 *
 * Pixels are laid out row-major (top-left first, bottom-right last).
 */
public final class ImageUtils {

    /** Holder for the raw pixel buffer + dimensions. */
    public static final class RgbaImage {
        public final int    width;
        public final int    height;
        public final byte[] pixels;   // length = width * height * 4

        private RgbaImage(int w, int h, byte[] p) {
            this.width  = w;
            this.height = h;
            this.pixels = p;
        }
    }

    /**
     * Convert PNG bytes (ARGB, RGB, indexed … anything ImageIO understands)
     * into a simple RGBA buffer.
     *
     * @param pngBytes raw PNG data
     * @return         width, height and a byte[] of size width*height*4
     * @throws IOException if the PNG is malformed or unsupported
     */
    public static RgbaImage pngToRgba(byte[] pngBytes) throws IOException {

        BufferedImage img = ImageIO.read(new ByteArrayInputStream(pngBytes));
        if (img == null) {
            throw new IOException("Unsupported or corrupt PNG data");
        }

        int w = img.getWidth();
        int h = img.getHeight();
        byte[] out = new byte[w * h * 4];

        int p = 0;                    // running index in the out buffer
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int argb = img.getRGB(x, y);        // 0xAARRGGBB

                out[p++] = (byte) ((argb >> 16) & 0xFF);   // R
                out[p++] = (byte) ((argb >>  8) & 0xFF);   // G
                out[p++] = (byte) ( argb        & 0xFF);   // B
                out[p++] = (byte) ((argb >> 24) & 0xFF);   // A
            }
        }
        return new RgbaImage(w, h, out);
    }

    // prevent instantiation
    private ImageUtils() {}
}