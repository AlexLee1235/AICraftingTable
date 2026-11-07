package com.watermelon0117.aicraft.common;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class FaceImagesTest {
    private static BufferedImage filled(int width, int height, int argb) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                image.setRGB(x, y, argb);
            }
        }
        return image;
    }

    @Test
    void anAnimatedStripGivesItsTopFrame() {
        BufferedImage strip = filled(16, 48, 0xFF112233);
        for (int y = 16; y < 48; y++) {
            for (int x = 0; x < 16; x++) {
                strip.setRGB(x, y, 0xFF445566);
            }
        }
        BufferedImage frame = FaceImages.firstFrame(strip);

        assertNotNull(frame);
        assertEquals(16, frame.getWidth());
        assertEquals(16, frame.getHeight());
        assertEquals(0xFF112233, frame.getRGB(15, 15));
    }

    @Test
    void aPictureNot16WideOrNotWholeFramesIsRefused() {
        assertNull(FaceImages.firstFrame(filled(32, 32, 0xFF000000)));
        assertNull(FaceImages.firstFrame(filled(16, 24, 0xFF000000)));
        assertNull(FaceImages.firstFrame(filled(16, 8, 0xFF000000)));
        assertNull(FaceImages.firstFrame(null));
        assertNotNull(FaceImages.firstFrame(filled(16, 16, 0xFF000000)));
    }

    @Test
    void oneUntintedLayerComesOutUnchanged() {
        BufferedImage layer = filled(16, 16, 0xFF7F3A10);
        layer.setRGB(3, 4, 0x00000000);
        layer.setRGB(5, 6, 0x80C0FFEE);

        BufferedImage out = FaceImages.compose(List.of(new FaceImages.Layer(layer, -1)));

        assertEquals(0xFF7F3A10, out.getRGB(0, 0));
        assertEquals(0x00000000, out.getRGB(3, 4));
        assertEquals(0x80C0FFEE, out.getRGB(5, 6));
    }

    @Test
    void aTintMultipliesEachChannel() {
        assertEquals(0xFF7F7F00, FaceImages.tinted(0xFFFFFFFF, 0x7F7F00));
        assertEquals(0xFF000000 | Math.round(200 * 145 / 255f) << 16 | Math.round(100 * 189 / 255f) << 8 | Math.round(50 * 89 / 255f),
                FaceImages.tinted(0xFFC86432, 0x91BD59));
        assertEquals(0x80FFFFFF, FaceImages.tinted(0x80FFFFFF, -1));
    }

    @Test
    void aTintedOverlayCoversTheBaseWhereItIsSolidOnly() {
        BufferedImage base = filled(16, 16, 0xFF6B4A2F);
        BufferedImage overlay = filled(16, 16, 0x00000000);
        overlay.setRGB(0, 0, 0xFFFFFFFF);
        overlay.setRGB(1, 0, 0x80FFFFFF);

        BufferedImage out = FaceImages.compose(List.of(new FaceImages.Layer(base, -1), new FaceImages.Layer(overlay, 0x91BD59)));

        assertEquals(0xFF91BD59, out.getRGB(0, 0));
        assertEquals(0xFF6B4A2F, out.getRGB(0, 1));
        float a = 0x80 / 255f;
        int r = Math.round(0x91 * a + 0x6B * (1 - a)), g = Math.round(0xBD * a + 0x4A * (1 - a)), b = Math.round(0x59 * a + 0x2F * (1 - a));
        assertEquals(0xFF000000 | r << 16 | g << 8 | b, out.getRGB(1, 0));
    }

    @Test
    void theFaceIsAReadable16x16Png() throws IOException {
        byte[] png = FaceImages.png(FaceImages.compose(List.of(new FaceImages.Layer(filled(16, 16, 0xFF123456), -1))));

        BufferedImage read = ImageIO.read(new ByteArrayInputStream(png));

        assertEquals(16, read.getWidth());
        assertEquals(0xFF123456, read.getRGB(7, 7));
        new BlockFaces(png, png, png).validated();
    }
}
