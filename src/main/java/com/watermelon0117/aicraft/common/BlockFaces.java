package com.watermelon0117.aicraft.common;

import com.watermelon0117.aicraft.blocks.AIBlockTransparency;

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

    /** A plant has one picture, which stands in for all three faces. */
    public static BlockFaces ofCross(byte[] texture) {
        return new BlockFaces(texture, texture, texture);
    }

    /**
     * Throws unless each face is a 16x16 image, another size reading as an animation in the atlas, that shows something.
     * A face may be see-through: the texture server draws one so only when its judge says the block is.
     */
    public BlockFaces validated() {
        validate("top", top);
        validate("front", front);
        validate("side", side);
        return this;
    }

    /** What the faces' pixels let through. Read when a block is made, as a block state must know it before any face arrives. */
    public AIBlockTransparency transparency() {
        int[] alphas = new int[3 * FACE_SIZE * FACE_SIZE];
        int at = 0;
        for (byte[] face : new byte[][]{top, front, side}) {
            BufferedImage image = readSized("block", face);
            for (int pixel : image.getRGB(0, 0, FACE_SIZE, FACE_SIZE, null, 0, FACE_SIZE)) {
                alphas[at++] = pixel >>> 24;
            }
        }
        return AIBlockTransparency.ofAlphas(alphas);
    }

    /** Throws unless the picture is 16x16 and shows something. It may be see-through, unlike a face. */
    public BlockFaces validatedCross() {
        BufferedImage image = readSized("plant", top);
        for (int y = 0; y < FACE_SIZE; y++) {
            for (int x = 0; x < FACE_SIZE; x++) {
                if ((image.getRGB(x, y) >>> 24) != 0) {
                    return this;
                }
            }
        }
        throw new IllegalArgumentException("The plant picture is empty");
    }

    private static void validate(String name, byte[] png) {
        BufferedImage image = readSized(name, png);
        for (int y = 0; y < FACE_SIZE; y++) {
            for (int x = 0; x < FACE_SIZE; x++) {
                if ((image.getRGB(x, y) >>> 24) != 0) {
                    return;
                }
            }
        }
        throw new IllegalArgumentException("The " + name + " face is empty");
    }

    private static BufferedImage readSized(String name, byte[] png) {
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
        return image;
    }

    /** Whether one side of the block looks different, as on a furnace. */
    public boolean hasFront() {
        return !Arrays.equals(front, side);
    }

    public int byteSize() {
        return top.length + front.length + side.length;
    }
}
