package com.watermelon0117.aicraft.common;

import com.watermelon0117.aicraft.blocks.AIBlockHardness;
import com.watermelon0117.aicraft.blocks.AIBlockLight;
import com.watermelon0117.aicraft.blocks.AIBlockShape;
import com.watermelon0117.aicraft.blocks.AIBlockTool;
import com.watermelon0117.aicraft.gpt.GeneratedBlock;
import com.watermelon0117.aicraft.gpt.GeneratedResult;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlockFacesTest {
    private static byte[] png(int size, int argb) throws IOException {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                image.setRGB(x, y, argb);
            }
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }

    private static String base64(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }

    private static GeneratedBlock cube(String kind, String tool, String hardness, String light, String top, String front, String side) {
        return GeneratedBlock.fromResponse(new GeneratedResult.Response("block", null, null, "cube_material", kind, tool, hardness, light, "stone", false, 1, false, null, top, front, side));
    }

    private static GeneratedBlock material(String model, String face) {
        return GeneratedBlock.fromResponse(new GeneratedResult.Response("block", null, null, model, "material", "axe", "hard", "dim", "wood", true, 6, false, null, face, face, face));
    }

    private static GeneratedBlock cross(String texture) {
        return GeneratedBlock.fromResponse(new GeneratedResult.Response("block", null, null, "cross", null, "none", "instant", "none", "grass", true, 1, false, texture, null, null, null));
    }

    @Test
    void theKindDecidesWhichFacesAreShown() {
        byte[] top = {1}, front = {2}, side = {3};

        BlockFaces material = BlockFaces.of("material", top, front, side);
        assertSame(top, material.front());
        assertSame(top, material.side());

        BlockFaces natural = BlockFaces.of("natural", top, front, side);
        assertSame(top, natural.top());
        assertSame(side, natural.front());
        assertSame(side, natural.side());

        BlockFaces machine = BlockFaces.of("machine", top, front, side);
        assertSame(top, machine.top());
        assertSame(front, machine.front());
        assertSame(side, machine.side());
    }

    @Test
    void onlyAMachineHasAFront() {
        byte[] top = {1}, front = {2}, side = {3};
        assertTrue(BlockFaces.of("machine", top, front, side).hasFront());
        assertFalse(BlockFaces.of("natural", top, front, side).hasFront());
        assertFalse(BlockFaces.of("material", top, front, side).hasFront());
    }

    @Test
    void anUnknownKindIsRefused() {
        byte[] face = {1};
        assertThrows(IllegalArgumentException.class, () -> BlockFaces.of("liquid", face, face, face));
        assertThrows(IllegalArgumentException.class, () -> BlockFaces.of(null, face, face, face));
    }

    @Test
    void onlyOpaque16x16ImagesPass() throws IOException {
        byte[] good = png(16, 0xFF336699);
        new BlockFaces(good, good, good).validated();

        byte[] tooBig = png(32, 0xFF336699);
        IllegalArgumentException size = assertThrows(IllegalArgumentException.class, () -> new BlockFaces(good, tooBig, good).validated());
        assertTrue(size.getMessage().contains("front"), size.getMessage());
        assertTrue(size.getMessage().contains("32x32"), size.getMessage());

        byte[] seeThrough = png(16, 0x80336699);
        assertThrows(IllegalArgumentException.class, () -> new BlockFaces(good, good, seeThrough).validated());

        byte[] notAnImage = {1, 2, 3};
        assertThrows(IllegalArgumentException.class, () -> new BlockFaces(notAnImage, good, good).validated());
    }

    @Test
    void aServerAnswerBecomesABlock() throws IOException {
        byte[] top = png(16, 0xFF111111), front = png(16, 0xFF222222), side = png(16, 0xFF333333);

        GeneratedBlock block = cube("natural", "axe", "hard", "dim", base64(top), base64(front), base64(side));

        assertEquals(AIBlockTool.AXE, block.tool());
        assertEquals(AIBlockHardness.HARD, block.hardness());
        assertEquals(AIBlockLight.DIM, block.light());
        assertArrayEquals(top, block.faces().top());
        assertArrayEquals(side, block.faces().front());
        assertArrayEquals(side, block.faces().side());
    }

    @Test
    void aMissingValueIsRefusedWithItsField() throws IOException {
        String face = base64(png(16, 0xFF111111));
        IllegalArgumentException tool = assertThrows(IllegalArgumentException.class,
                () -> cube("material", null, "hard", "dim", face, face, face));
        assertTrue(tool.getMessage().contains("no tool"), tool.getMessage());

        IllegalArgumentException hardness = assertThrows(IllegalArgumentException.class,
                () -> cube("material", "axe", null, "dim", face, face, face));
        assertTrue(hardness.getMessage().contains("no hardness"), hardness.getMessage());

        IllegalArgumentException light = assertThrows(IllegalArgumentException.class,
                () -> cube("material", "axe", "hard", null, face, face, face));
        assertTrue(light.getMessage().contains("no light"), light.getMessage());
    }

    @Test
    void aTagIsReadByName() {
        for (AIBlockTool tool : AIBlockTool.values()) {
            assertSame(tool, AIBlockTool.byName(tool.getSerializedName()));
        }
        for (AIBlockHardness hardness : AIBlockHardness.values()) {
            assertSame(hardness, AIBlockHardness.byName(hardness.getSerializedName()));
        }
    }

    @Test
    void aTagWithNoKnownValueGetsTheDefaults() {
        assertSame(AIBlockTool.PICKAXE, AIBlockTool.byName(""));
        assertSame(AIBlockTool.PICKAXE, AIBlockTool.byName("sword"));
        assertSame(AIBlockHardness.NORMAL, AIBlockHardness.byName(""));
        assertSame(AIBlockHardness.NORMAL, AIBlockHardness.byName("weird"));
    }

    @Test
    void everyValueTheServerCanSendIsKnown() throws IOException {
        String face = base64(png(16, 0xFF111111));
        for (String tool : new String[]{"pickaxe", "axe", "shovel", "hoe", "none"}) {
            for (String hardness : new String[]{"instant", "soft", "normal", "hard", "unbreakable"}) {
                for (String light : new String[]{"none", "dim", "bright"}) {
                    GeneratedBlock block = cube("material", tool, hardness, light, face, face, face);
                    assertEquals(tool, block.tool().getSerializedName());
                    assertEquals(hardness, block.hardness().getSerializedName());
                    assertEquals(light, block.light().getSerializedName());
                }
            }
        }
    }

    @Test
    void anUnknownValueIsRefusedWithItsField() throws IOException {
        String face = base64(png(16, 0xFF111111));
        IllegalArgumentException tool = assertThrows(IllegalArgumentException.class,
                () -> cube("material", "sword", "hard", "dim", face, face, face));
        assertTrue(tool.getMessage().contains("tool: sword"), tool.getMessage());

        IllegalArgumentException hardness = assertThrows(IllegalArgumentException.class,
                () -> cube("material", "axe", "medium", "dim", face, face, face));
        assertTrue(hardness.getMessage().contains("hardness: medium"), hardness.getMessage());

        IllegalArgumentException light = assertThrows(IllegalArgumentException.class,
                () -> cube("material", "axe", "hard", "7", face, face, face));
        assertTrue(light.getMessage().contains("light: 7"), light.getMessage());
    }

    @Test
    void everyModelTheServerCanSendHasAShape() throws IOException {
        String face = base64(png(16, 0xFF111111));
        assertSame(AIBlockShape.CUBE, material("cube_material", face).shape());
        assertSame(AIBlockShape.CUBE, material("cube_natural", face).shape());
        assertSame(AIBlockShape.CUBE, material("cube_machine", face).shape());
        assertSame(AIBlockShape.SLAB, material("slab", face).shape());
        assertSame(AIBlockShape.STAIRS, material("stairs", face).shape());
        assertSame(AIBlockShape.FENCE, material("fence", face).shape());
        assertSame(AIBlockShape.WALL, material("wall", face).shape());
        assertSame(AIBlockShape.CROSS, cross(face).shape());
    }

    @Test
    void aModelOfNoKnownShapeIsRefused() throws IOException {
        String face = base64(png(16, 0xFF111111));
        IllegalArgumentException unknown = assertThrows(IllegalArgumentException.class, () -> material("door", face));
        assertTrue(unknown.getMessage().contains("model: door"), unknown.getMessage());

        IllegalArgumentException none = assertThrows(IllegalArgumentException.class, () -> material(null, face));
        assertTrue(none.getMessage().contains("no model"), none.getMessage());
    }

    @Test
    void aPlantPictureStandsInForEveryFaceAndMayBeSeeThrough() throws IOException {
        byte[] seeThrough = png(16, 0x80336699);
        GeneratedBlock plant = cross(base64(seeThrough));
        assertArrayEquals(seeThrough, plant.faces().top());
        assertArrayEquals(seeThrough, plant.faces().front());
        assertArrayEquals(seeThrough, plant.faces().side());
        assertFalse(plant.faces().hasFront());
    }

    @Test
    void aBadPlantPictureIsRefused() throws IOException {
        IllegalArgumentException missing = assertThrows(IllegalArgumentException.class, () -> cross(null));
        assertTrue(missing.getMessage().contains("plant picture"), missing.getMessage());

        String empty = base64(png(16, 0x00000000));
        IllegalArgumentException nothing = assertThrows(IllegalArgumentException.class, () -> cross(empty));
        assertTrue(nothing.getMessage().contains("empty"), nothing.getMessage());

        String tooBig = base64(png(32, 0xFF336699));
        IllegalArgumentException size = assertThrows(IllegalArgumentException.class, () -> cross(tooBig));
        assertTrue(size.getMessage().contains("32x32"), size.getMessage());
    }

    @Test
    void onlyAPlantMayBeSeeThrough() throws IOException {
        String seeThrough = base64(png(16, 0x80336699));
        for (String model : new String[]{"cube_material", "slab", "stairs", "fence", "wall"}) {
            assertThrows(IllegalArgumentException.class, () -> material(model, seeThrough), model);
        }
    }

    @Test
    void theLevelsHaveVanillasNumbers() {
        assertEquals(0.0F, AIBlockHardness.INSTANT.hardness());
        assertEquals(0.0F, AIBlockHardness.INSTANT.blastResistance());
        assertEquals(0.5F, AIBlockHardness.SOFT.hardness());
        assertEquals(0.5F, AIBlockHardness.SOFT.blastResistance());
        assertEquals(1.5F, AIBlockHardness.NORMAL.hardness());
        assertEquals(6.0F, AIBlockHardness.NORMAL.blastResistance());
        assertEquals(5.0F, AIBlockHardness.HARD.hardness());
        assertEquals(6.0F, AIBlockHardness.HARD.blastResistance());
        assertEquals(-1.0F, AIBlockHardness.UNBREAKABLE.hardness());
        assertEquals(3600000.0F, AIBlockHardness.UNBREAKABLE.blastResistance());

        assertEquals(0, AIBlockLight.NONE.level());
        assertEquals(5, AIBlockLight.DIM.level());
        assertEquals(15, AIBlockLight.BRIGHT.level());
    }

    @Test
    void aBadServerAnswerIsRefusedWithAMessage() throws IOException {
        String face = base64(png(16, 0xFF111111));
        IllegalArgumentException noKind = assertThrows(IllegalArgumentException.class,
                () -> cube(null, "axe", "hard", "dim", face, face, face));
        assertTrue(noKind.getMessage().contains("kind"), noKind.getMessage());

        IllegalArgumentException noFaces = assertThrows(IllegalArgumentException.class,
                () -> cube("material", "axe", "hard", "dim", null, face, face));
        assertTrue(noFaces.getMessage().contains("faces"), noFaces.getMessage());
    }
}
