package com.watermelon0117.aicraft.common;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** A worn armor piece's picture (PNG) and its UV table ({@link ArmorUV}), as saved and sent. */
public record ArmorTexture(byte[] png, String uv) {
    /** The test's largest picture is under 6 KB; this leaves room for a fine one and keeps a login batch at many pieces. */
    public static final int MAX_PNG_BYTES = 64 * 1024;

    /**
     * Checks that the piece can be drawn: the table is good, the picture decodes, and it has the table's size.
     *
     * @return the parsed table
     * @throws IllegalArgumentException with the reason otherwise
     */
    public ArmorUV check() {
        ArmorUV table = ArmorUV.parse(uv);
        if (png.length > MAX_PNG_BYTES) {
            throw new IllegalArgumentException("The armor picture is " + png.length + " bytes, over " + MAX_PNG_BYTES);
        }
        BufferedImage image;
        try {
            image = ImageIO.read(new ByteArrayInputStream(png));
        } catch (IOException e) {
            throw new IllegalArgumentException("The armor picture cannot be read", e);
        }
        if (image == null) {
            throw new IllegalArgumentException("The armor picture is not a PNG");
        }
        if (image.getWidth() != table.width() || image.getHeight() != table.height()) {
            throw new IllegalArgumentException("The armor picture is " + image.getWidth() + "x" + image.getHeight()
                    + " but its table says " + table.width() + "x" + table.height());
        }
        return table;
    }

    public int byteSize() {
        return png.length + uv.getBytes(StandardCharsets.UTF_8).length;
    }
}
