package com.watermelon0117.aicraft.common;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;

/** The three PNG faces of one AI block. Top also stands in for the bottom, and side for the back and both flanks. */
public record BlockFaces(byte[] top, byte[] front, byte[] side) {
    public static final int FACE_SIZE = 16;

    /**
     * Settles what the block's kind means here, so the model needs no kind of its own.
     * A natural block has no front: it shows its side all the way round.
     */
    public static BlockFaces of(String kind, byte[] top, byte[] front, byte[] side) {
        return switch (kind == null ? "" : kind) {
            case "material" -> new BlockFaces(top, top, top);
            case "natural" -> new BlockFaces(top, side, side);
            case "machine" -> new BlockFaces(top, front, side);
            default -> throw new IllegalArgumentException("Unknown block kind: " + kind);
        };
    }

    /** Throws unless each face is an opaque 16x16 image: another size would read as an animation in the atlas. */
    public BlockFaces validated() {
        validate("top", top);
        validate("front", front);
        validate("side", side);
        return this;
    }

    private static void validate(String name, byte[] png) {
        BufferedImage image;
        try {
            image = ImageIO.read(new ByteArrayInputStream(png));
        } catch (IOException e) {
            throw new IllegalArgumentException("The " + name + " face is not a readable image", e);
        }
        if (image == null) {
            throw new IllegalArgumentException("The " + name + " face is not a readable image");
        }
        if (image.getWidth() != FACE_SIZE || image.getHeight() != FACE_SIZE) {
            throw new IllegalArgumentException("The " + name + " face is " + image.getWidth() + "x" + image.getHeight() + ", not 16x16");
        }
        for (int y = 0; y < FACE_SIZE; y++) {
            for (int x = 0; x < FACE_SIZE; x++) {
                if ((image.getRGB(x, y) >>> 24) != 255) {
                    throw new IllegalArgumentException("The " + name + " face is not opaque");
                }
            }
        }
    }

    /** Whether one side of the block looks different, as on a furnace. */
    public boolean hasFront() {
        return !Arrays.equals(front, side);
    }

    public int byteSize() {
        return top.length + front.length + side.length;
    }
}
