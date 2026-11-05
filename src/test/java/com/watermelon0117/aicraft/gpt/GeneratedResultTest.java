package com.watermelon0117.aicraft.gpt;

import com.watermelon0117.aicraft.blocks.AIBlockShape;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** An item answer builds an ItemStack, which needs the game's registries, so only the refusals of items are tested here. */
class GeneratedResultTest {
    private static String face() throws IOException {
        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                image.setRGB(x, y, 0xFF336699);
            }
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return Base64.getEncoder().encodeToString(out.toByteArray());
    }

    private static GeneratedResult.Response slab(Integer count, Boolean shapeless) throws IOException {
        String face = face();
        return new GeneratedResult.Response("block", null, null, "slab", "material", "pickaxe", "normal", "none", count, shapeless, null, face, face, face);
    }

    private static GeneratedResult parse(GeneratedResult.Response response) {
        return GeneratedResult.fromResponse(response, "Mossy Brick Slab", "Mossy Brick Slab");
    }

    @Test
    void aBlockKeepsItsCountAndLayoutRule() throws IOException {
        GeneratedBlock block = assertInstanceOf(GeneratedBlock.class, parse(slab(6, true)));
        assertEquals(AIBlockShape.SLAB, block.shape());
        assertEquals(6, block.count());
        assertTrue(block.shapeless());
    }

    @Test
    void aBlockWithNoLayoutRuleIsShaped() throws IOException {
        assertFalse(parse(slab(6, null)).shapeless());
    }

    @Test
    void aStackIsTheMostOneCraftCanMake() throws IOException {
        assertEquals(1, parse(slab(1, false)).count());
        assertEquals(64, parse(slab(64, false)).count());
        for (int count : new int[]{0, -1, 65}) {
            IllegalArgumentException refused = assertThrows(IllegalArgumentException.class, () -> parse(slab(count, false)));
            assertTrue(refused.getMessage().contains("count out of range: " + count), refused.getMessage());
        }
    }

    @Test
    void aBlockWithNoCountIsRefused() {
        IllegalArgumentException refused = assertThrows(IllegalArgumentException.class, () -> parse(slab(null, false)));
        assertTrue(refused.getMessage().contains("no count"), refused.getMessage());
    }

    @Test
    void anItemWithNoPictureIsRefused() {
        IllegalArgumentException noImage = assertThrows(IllegalArgumentException.class,
                () -> parse(new GeneratedResult.Response("item", null, "{}", null, null, null, null, null, null, null, null, null, null, null)));
        assertTrue(noImage.getMessage().contains("no item"), noImage.getMessage());

        IllegalArgumentException noJson = assertThrows(IllegalArgumentException.class,
                () -> parse(new GeneratedResult.Response("item", "AAAA", null, null, null, null, null, null, null, null, null, null, null, null)));
        assertTrue(noJson.getMessage().contains("no item"), noJson.getMessage());
    }

    @Test
    void anUnknownTypeIsRefused() throws IOException {
        String face = face();
        for (String type : new String[]{"mob", null}) {
            IllegalArgumentException refused = assertThrows(IllegalArgumentException.class,
                    () -> parse(new GeneratedResult.Response(type, null, null, "slab", "material", "pickaxe", "normal", "none", 6, false, null, face, face, face)));
            assertTrue(refused.getMessage().contains("unknown type: " + type), refused.getMessage());
        }
    }
}
