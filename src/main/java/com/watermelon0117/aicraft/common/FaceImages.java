package com.watermelon0117.aicraft.common;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

/** Puts one face together from a vanilla block's layers, as the game draws them. Only pictures here, so tests need no game. */
public final class FaceImages {
    private static final int SIZE = BlockFaces.FACE_SIZE;

    private FaceImages() {
    }

    /**
     * One picture of a face.
     *
     * @param tint the color the game multiplies it by, as 0xRRGGBB, or -1 for none
     */
    public record Layer(BufferedImage image, int tint) {
    }

    /** @return the top square of a picture, which is the first frame of an animated one, or null unless it is 16 wide and whole frames tall */
    public static BufferedImage firstFrame(BufferedImage image) {
        if (image == null || image.getWidth() != SIZE || image.getHeight() < SIZE || image.getHeight() % SIZE != 0) {
            return null;
        }
        return image.getSubimage(0, 0, SIZE, SIZE);
    }

    /** Draws each 16x16 layer in order, multiplied by its tint, over what is below it by its own alpha. */
    public static BufferedImage compose(List<Layer> layers) {
        BufferedImage out = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        for (Layer layer : layers) {
            for (int y = 0; y < SIZE; y++) {
                for (int x = 0; x < SIZE; x++) {
                    out.setRGB(x, y, over(tinted(layer.image.getRGB(x, y), layer.tint), out.getRGB(x, y)));
                }
            }
        }
        return out;
    }

    static int tinted(int argb, int tint) {
        if (tint == -1) {
            return argb;
        }
        int r = Math.round((argb >> 16 & 255) * (tint >> 16 & 255) / 255f);
        int g = Math.round((argb >> 8 & 255) * (tint >> 8 & 255) / 255f);
        int b = Math.round((argb & 255) * (tint & 255) / 255f);
        return argb & 0xFF000000 | r << 16 | g << 8 | b;
    }

    /** Straight-alpha source over destination. */
    static int over(int src, int dst) {
        float sa = (src >>> 24) / 255f, da = (dst >>> 24) / 255f;
        float oa = sa + da * (1 - sa);
        if (oa == 0) {
            return 0;
        }
        int r = channel(src >> 16 & 255, sa, dst >> 16 & 255, da, oa);
        int g = channel(src >> 8 & 255, sa, dst >> 8 & 255, da, oa);
        int b = channel(src & 255, sa, dst & 255, da, oa);
        return Math.round(oa * 255) << 24 | r << 16 | g << 8 | b;
    }

    private static int channel(int src, float sa, int dst, float da, float oa) {
        return Math.round((src * sa + dst * da * (1 - sa)) / oa);
    }

    public static byte[] png(BufferedImage image) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "png", out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
