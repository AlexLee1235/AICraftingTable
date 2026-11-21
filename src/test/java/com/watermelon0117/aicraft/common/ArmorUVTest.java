package com.watermelon0117.aicraft.common;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArmorUVTest {
    /** The Wq bedrock chestplate's table from the armor test, as it wrote it. */
    private static final String CHESTPLATE = """
            {"slot": "chestplate", "step": 21.75, "size": [44, 30], "faces": {
             "BODY.front": [0, 0, 8, 12, "read"], "BODY.back": [9, 0, 9, 12, "read"], "BODY.right": [19, 0, 6, 14, "read"],
             "BODY.left": [26, 0, 6, 14, "read"], "BODY.top": [33, 0, 10, 8, "read"], "R_ARM.front": [0, 15, 6, 12, "read"],
             "R_ARM.back": [7, 15, 6, 12, "read"], "R_ARM.right": [14, 15, 6, 14, "read"],
             "R_ARM.left": [21, 15, 6, 14, "outer side, mirrored"], "R_ARM.top": [28, 15, 6, 6, "read"]},
             "view_steps": {"front": 24.0, "back": 24.0, "right": 21.0, "left": 21.0, "top": 21.75}}""";

    private static String helmet(String faces) {
        return "{\"slot\": \"helmet\", \"size\": [40, 10], \"faces\": {" + faces + "}}";
    }

    private static final String HEAD_FACES = "\"HEAD.front\": [0, 0, 8, 8], \"HEAD.back\": [8, 0, 8, 8], \"HEAD.right\": [16, 0, 8, 8],"
            + " \"HEAD.left\": [24, 0, 8, 8], \"HEAD.top\": [32, 0, 8, 8]";

    @Test
    void theTestsOwnTableIsReadWithEveryFace() {
        ArmorUV uv = ArmorUV.parse(CHESTPLATE);

        assertEquals(ArmorUV.Slot.CHESTPLATE, uv.slot());
        assertEquals(44, uv.width());
        assertEquals(30, uv.height());
        assertEquals(10, uv.faces().size());
        assertEquals(new ArmorUV.Rect(21, 15, 6, 14), uv.face(ArmorUV.R_ARM, "left"));
        assertEquals(null, uv.face(ArmorUV.R_ARM, "bottom"));
    }

    @Test
    void aMissingFaceIsRefused() {
        String noTop = HEAD_FACES.replace(", \"HEAD.top\": [32, 0, 8, 8]", "");
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> ArmorUV.parse(helmet(noTop)));
        assertTrue(e.getMessage().contains("HEAD.top"), e.getMessage());
    }

    @Test
    void aFaceOutsideThePictureOrOfAnotherSlotIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> ArmorUV.parse(helmet(HEAD_FACES.replace("[32, 0, 8, 8]", "[33, 0, 8, 8]"))));
        assertThrows(IllegalArgumentException.class, () -> ArmorUV.parse(helmet(HEAD_FACES.replace("[32, 0, 8, 8]", "[32, 0, 0, 8]"))));
        assertThrows(IllegalArgumentException.class, () -> ArmorUV.parse(helmet(HEAD_FACES.replace("[0, 0, 8, 8]", "[-1, 0, 8, 8]"))));
        assertThrows(IllegalArgumentException.class, () -> ArmorUV.parse(helmet(HEAD_FACES + ", \"R_LEG.front\": [0, 0, 1, 1]")));
        assertThrows(IllegalArgumentException.class, () -> ArmorUV.parse(helmet(HEAD_FACES + ", \"HEAD.inside\": [0, 0, 1, 1]")));
        assertThrows(IllegalArgumentException.class, () -> ArmorUV.parse(helmet(HEAD_FACES.replace("[0, 0, 8, 8]", "[0.5, 0, 8, 8]"))));
    }

    @Test
    void aBadTableIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> ArmorUV.parse("not json"));
        assertThrows(IllegalArgumentException.class, () -> ArmorUV.parse("[]"));
        assertThrows(IllegalArgumentException.class, () -> ArmorUV.parse(helmet(HEAD_FACES).replace("helmet", "gloves")));
        assertThrows(IllegalArgumentException.class, () -> ArmorUV.parse(helmet(HEAD_FACES).replace("[40, 10]", "[300, 10]")));
        assertThrows(IllegalArgumentException.class, () -> ArmorUV.parse("{\"slot\": \"helmet\", \"size\": [40, 10]}"));
    }

    @Test
    void aPlainTableHasEveryRequiredFaceOnOnePixel() {
        for (ArmorUV.Slot slot : ArmorUV.Slot.values()) {
            ArmorUV plain = ArmorUV.plain(slot);
            int faces = slot.required().values().stream().mapToInt(List::size).sum();
            assertEquals(faces, plain.faces().size(), slot.key());
            plain.faces().values().forEach(rect -> assertEquals(new ArmorUV.Rect(0, 0, 1, 1), rect));
        }
    }

    private static byte[] png(int width, int height) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB), "png", out);
        return out.toByteArray();
    }

    @Test
    void aPictureMustHaveItsTablesSize() throws IOException {
        assertEquals(ArmorUV.Slot.CHESTPLATE, new ArmorTexture(png(44, 30), CHESTPLATE).check().slot());
        assertThrows(IllegalArgumentException.class, () -> new ArmorTexture(png(44, 31), CHESTPLATE).check());
        assertThrows(IllegalArgumentException.class, () -> new ArmorTexture(new byte[]{1, 2, 3}, CHESTPLATE).check());
        assertThrows(IllegalArgumentException.class, () -> new ArmorTexture(new byte[ArmorTexture.MAX_PNG_BYTES + 1], CHESTPLATE).check());
    }
}
