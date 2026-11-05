package com.watermelon0117.aicraft.gpt;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.GsonBuilder;
import com.watermelon0117.aicraft.blocks.AIBlockShape;
import com.watermelon0117.aicraft.blocks.AIBlockSound;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** An item answer builds an ItemStack, which needs the game's registries, so only the refusals of items are tested here. */
class GeneratedResultTest {
    /** {@code SOUNDS} in the texture server's src/blocks.js, in its order. */
    private static final String[] SERVER_SOUNDS = {"stone", "deepslate", "netherrack", "nether_bricks", "bone", "metal", "copper", "netherite",
            "amethyst", "glass", "wood", "wool", "grass", "gravel", "sand", "mud", "snow", "slime", "honey", "coral", "wart", "sculk"};

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
        return slab("stone", false, count, shapeless);
    }

    private static GeneratedResult.Response sounding(String sound, Boolean flammable) throws IOException {
        return slab(sound, flammable, 6, false);
    }

    private static GeneratedResult.Response slab(String sound, Boolean flammable, Integer count, Boolean shapeless) throws IOException {
        String face = face();
        return new GeneratedResult.Response("block", null, null, "slab", "material", "pickaxe", "normal", "none", sound, flammable, count, shapeless, null, face, face, face);
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
    void everySoundTheServerSendsIsKnown() throws IOException {
        assertArrayEquals(SERVER_SOUNDS, Arrays.stream(AIBlockSound.values()).map(AIBlockSound::getSerializedName).toArray());
        for (String sound : SERVER_SOUNDS) {
            GeneratedBlock block = assertInstanceOf(GeneratedBlock.class, parse(sounding(sound, false)));
            assertEquals(sound, block.sound().getSerializedName());
            assertEquals(block.sound(), AIBlockSound.byName(sound));
        }
    }

    /** The texture server's README example, read the way {@link JsonHttpClient} reads an answer. */
    @Test
    void theServersAnswerIsReadByName() throws IOException {
        String face = face();
        String json = "{\"type\": \"block\", \"model\": \"slab\", \"kind\": \"material\", \"tool\": \"axe\", \"hardness\": \"normal\", \"light\": \"none\", "
                + "\"sound\": \"wood\", \"flammable\": true, \"count\": 6, \"shapeless\": false, "
                + "\"top\": \"" + face + "\", \"front\": \"" + face + "\", \"side\": \"" + face + "\", \"json\": \"{}\"}";
        GeneratedResult.Response response = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.IDENTITY).create().fromJson(json, GeneratedResult.Response.class);
        GeneratedBlock block = assertInstanceOf(GeneratedBlock.class, parse(response));
        assertEquals(AIBlockSound.WOOD, block.sound());
        assertTrue(block.flammable());
        assertEquals(6, block.count());
    }

    @Test
    void aTagWithNoSoundSoundsLikeStone() {
        assertEquals(AIBlockSound.STONE, AIBlockSound.byName(""));
        assertEquals(AIBlockSound.STONE, AIBlockSound.byName("squeak"));
    }

    @Test
    void aBlockKeepsItsFlammability() throws IOException {
        assertTrue(assertInstanceOf(GeneratedBlock.class, parse(sounding("wood", true))).flammable());
        assertFalse(assertInstanceOf(GeneratedBlock.class, parse(sounding("wood", false))).flammable());
    }

    @Test
    void aBlockWithNoOrAnUnknownSoundIsRefused() {
        IllegalArgumentException none = assertThrows(IllegalArgumentException.class, () -> parse(sounding(null, false)));
        assertTrue(none.getMessage().contains("no sound"), none.getMessage());
        IllegalArgumentException unknown = assertThrows(IllegalArgumentException.class, () -> parse(sounding("squeak", false)));
        assertTrue(unknown.getMessage().contains("unknown sound: squeak"), unknown.getMessage());
    }

    @Test
    void aBlockWithNoFlammabilityIsRefused() {
        IllegalArgumentException refused = assertThrows(IllegalArgumentException.class, () -> parse(sounding("wood", null)));
        assertTrue(refused.getMessage().contains("no flammable"), refused.getMessage());
    }

    @Test
    void aBlockWithNoCountIsRefused() {
        IllegalArgumentException refused = assertThrows(IllegalArgumentException.class, () -> parse(slab(null, false)));
        assertTrue(refused.getMessage().contains("no count"), refused.getMessage());
    }

    @Test
    void anItemWithNoPictureIsRefused() {
        IllegalArgumentException noImage = assertThrows(IllegalArgumentException.class,
                () -> parse(new GeneratedResult.Response("item", null, "{}", null, null, null, null, null, null, null, null, null, null, null, null, null)));
        assertTrue(noImage.getMessage().contains("no item"), noImage.getMessage());

        IllegalArgumentException noJson = assertThrows(IllegalArgumentException.class,
                () -> parse(new GeneratedResult.Response("item", "AAAA", null, null, null, null, null, null, null, null, null, null, null, null, null, null)));
        assertTrue(noJson.getMessage().contains("no item"), noJson.getMessage());
    }

    @Test
    void anUnknownTypeIsRefused() throws IOException {
        String face = face();
        for (String type : new String[]{"mob", null}) {
            IllegalArgumentException refused = assertThrows(IllegalArgumentException.class,
                    () -> parse(new GeneratedResult.Response(type, null, null, "slab", "material", "pickaxe", "normal", "none", "stone", false, 6, false, null, face, face, face)));
            assertTrue(refused.getMessage().contains("unknown type: " + type), refused.getMessage());
        }
    }
}
