package com.watermelon0117.aicraft.gpt.openai;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The texture server's own cases for these rules (test/blocks.test.js). */
class BlockJudgeTest {
    private static final Object MISSING = new Object();
    private static final String[] STONE_GRID = filled("Stone");
    private static final String[] DYED_GRID = {"Stone Slab", "Red Dye", "empty", "empty", "empty", "empty", "empty", "empty", "empty"};
    /** Vanilla's own recipe for each shape, which alone makes vanilla's count. */
    private static final Map<String, String[]> VANILLA_GRID = Map.of(
            "slab", new String[]{"empty", "empty", "empty", "Stone", "Stone", "Stone", "empty", "empty", "empty"},
            "stairs", new String[]{"Stone", "empty", "empty", "Stone", "Stone", "empty", "Stone", "Stone", "Stone"},
            "fence", new String[]{"Oak Planks", "Stick", "Oak Planks", "Oak Planks", "Stick", "Oak Planks", "empty", "empty", "empty"},
            "wall", new String[]{"Stone", "Stone", "Stone", "Stone", "Stone", "Stone", "empty", "empty", "empty"});
    private static final Map<String, Integer> VANILLA_COUNT = Map.of("slab", 6, "stairs", 4, "fence", 3, "wall", 6);

    private static String[] filled(String cell) {
        String[] grid = new String[9];
        Arrays.fill(grid, cell);
        return grid;
    }

    /** @param fields names and values that replace the defaults; {@link #MISSING} leaves one out */
    private static String answer(String model, Object... fields) {
        JsonObject json = new JsonObject();
        json.addProperty("model", model);
        json.addProperty("tool", "axe");
        json.addProperty("hardness", "hard");
        json.addProperty("light", "dim");
        json.addProperty("sound", "wood");
        json.addProperty("flammable", true);
        json.addProperty("see_through", false);
        json.addProperty("attach", "below");
        json.addProperty("output_count", 1);
        json.addProperty("is_shapeless_crafting", false);
        json.addProperty("description", "");
        json.addProperty("top", "");
        json.addProperty("front", "");
        json.addProperty("side", "");
        for (int i = 0; i < fields.length; i += 2) {
            String field = (String) fields[i];
            Object value = fields[i + 1];
            json.remove(field);
            if (value instanceof String text) {
                json.addProperty(field, text);
            } else if (value instanceof Boolean flag) {
                json.addProperty(field, flag);
            } else if (value instanceof Number number) {
                json.addProperty(field, number);
            }
        }
        return json.toString();
    }

    /** Named for what the judge answers, so that the name lets every model stand. */
    private static Optional<BlockJudge.Plan> parse(String rawJson, String[] grid) {
        JsonElement parsed = JsonParser.parseString(rawJson);
        String model = parsed.isJsonObject() && parsed.getAsJsonObject().has("model") ? parsed.getAsJsonObject().get("model").getAsString() : "x";
        return BlockJudge.parse(rawJson, "Stone " + model, grid);
    }

    private static BlockJudge.Plan plan(String rawJson) {
        return parse(rawJson, STONE_GRID).orElseThrow();
    }

    private static BlockJudge.Plan plan(String rawJson, String[] grid) {
        return parse(rawJson, grid).orElseThrow();
    }

    @Test
    void anItemAndAnOtherAreItems() {
        for (String model : List.of("item", "other")) {
            assertEquals(Optional.empty(), parse(answer(model, "tool", "not looked at"), STONE_GRID), model);
        }
    }

    @Test
    void keepsOnlyWhatTheModelUsesCleaned() {
        BlockJudge.Plan material = plan(answer("cube_material", "description", "  Gray\n stone. ", "top", "ignored"));
        assertEquals(new BlockJudge.Plan("cube_material", "material", "axe", "hard", "dim", "wood", true, false, 1, false, null, "Gray stone.", "", "", ""), material);
        BlockJudge.Plan machine = plan(answer("cube_machine", "top", "a top.", "front", " a\nfront ", "side", "a side . "));
        assertEquals("machine", machine.kind());
        assertEquals(List.of("a top", "a front", "a side"), List.of(machine.top(), machine.front(), machine.side()));
        assertEquals("natural", plan(answer("cube_natural", "top", "t", "front", "f", "side", "s")).kind());
    }

    @Test
    void vanillasRecipeMakesVanillasCountAndAnyOtherTheJudges() {
        VANILLA_GRID.forEach((model, grid) -> {
            BlockJudge.Plan shaped = plan(answer(model, "description", "Gray.", "output_count", 2), grid);
            assertEquals("material", shaped.kind(), model);
            assertEquals(VANILLA_COUNT.get(model), shaped.count(), model);
            assertEquals("axe", shaped.tool(), model);
            for (String[] other : List.of(DYED_GRID, STONE_GRID)) {
                assertEquals(2, plan(answer(model, "description", "Gray.", "output_count", 2), other).count(), model);
            }
        });
    }

    @Test
    void aRecipeWithOneShapesLayoutMakesTheJudgesCountWhenJudgedAnother() {
        assertEquals(2, plan(answer("stairs", "description", "Gray.", "output_count", 2), VANILLA_GRID.get("slab")).count());
        assertEquals(2, plan(answer("cube_material", "description", "Gray.", "output_count", 2), VANILLA_GRID.get("wall")).count());
        assertEquals(2, plan(answer("cross", "description", "A sapling.", "output_count", 2), VANILLA_GRID.get("slab")).count());
    }

    @Test
    void aShapeWhoseNameDoesNotSaySoIsAnItem() {
        for (String[] pair : new String[][]{{"slab", "Thin Copper Plate"}, {"slab", "Copper Plate"}, {"stairs", "Oak Corner Step"}, {"fence", "Oak Rack"}, {"wall", "Copper Panel"}, {"wall", "Wallpaper"}}) {
            assertEquals(Optional.empty(), BlockJudge.parse(answer(pair[0], "description", "x"), pair[1], VANILLA_GRID.get(pair[0])), pair[1]);
        }
    }

    @Test
    void aShapeStandsWhenItsNameSaysSoInAnyCase() {
        for (String[] pair : new String[][]{{"slab", "COPPER SLAB"}, {"slab", "Copper Half-Slab"}, {"slab", "Stone Slabs"}, {"stairs", "Oak Stairs"},
                {"stairs", "Reinforced Oak Stair"}, {"stairs", "Diagonal Copper Staircase"}, {"stairs", "Oak Stairway"}, {"fence", "Nether Fence"},
                {"fence", "Oak Fences"}, {"wall", "Village Wall"}, {"wall", "Brick Walls"}}) {
            assertEquals(pair[0], BlockJudge.parse(answer(pair[0], "description", "x"), pair[1], STONE_GRID).orElseThrow().model(), pair[1]);
        }
    }

    @Test
    void onlyTheFourShapesNeedTheirNameToSaySo() {
        for (String model : List.of("cube_material", "cross")) {
            assertEquals(model, BlockJudge.parse(answer(model, "description", "x"), "Thin Copper Plate", VANILLA_GRID.get("slab")).orElseThrow().model());
        }
    }

    @Test
    void aCrossKeepsTheJudgesToolCountAndAttachAndHasNoSheet() {
        BlockJudge.Plan cross = plan(answer("cross", "description", "An iron chain.", "tool", "pickaxe", "output_count", 5));
        assertEquals("pickaxe", cross.tool());
        assertEquals(5, cross.count());
        assertNull(cross.kind());
        for (String attach : List.of("below", "above", "any")) {
            assertEquals(attach, plan(answer("cross", "description", "x", "attach", attach)).attach());
        }
        assertNull(plan(answer("cube_material", "description", "x", "attach", "not looked at")).attach());
    }

    @Test
    void everyShapeKeepsTheJudgesHardness() {
        for (String[] pair : new String[][]{{"cross", "instant"}, {"cross", "unbreakable"}, {"cube_material", "instant"}, {"slab", "instant"}}) {
            assertEquals(pair[1], plan(answer(pair[0], "description", "x", "hardness", pair[1])).hardness());
        }
    }

    @Test
    void acceptsEachOfTheSoundsWhichTheSchemaOffersInTheSameOrder() {
        List<String> sounds = List.of("stone", "deepslate", "netherrack", "nether_bricks", "bone", "metal", "copper", "netherite", "amethyst", "glass", "wood",
                "wool", "grass", "gravel", "sand", "mud", "snow", "slime", "honey", "coral", "wart", "sculk");
        List<String> kept = new ArrayList<>();
        for (String sound : sounds) {
            kept.add(plan(answer("cube_material", "description", "x", "sound", sound)).sound());
        }
        assertEquals(sounds, kept);
        JsonArray offered = BlockJudge.schema().getAsJsonObject("properties").getAsJsonObject("sound").getAsJsonArray("enum");
        List<String> schemaSounds = new ArrayList<>();
        offered.forEach(sound -> schemaSounds.add(sound.getAsString()));
        assertEquals(sounds, schemaSounds);
    }

    @Test
    void everyShapeWithASheetCanBeSeeThroughButNotACross() {
        for (String model : List.of("cube_material", "slab", "stairs", "fence", "wall")) {
            assertTrue(plan(answer(model, "description", "Clear glass.", "see_through", true)).seeThrough(), model);
        }
        for (String model : List.of("cube_natural", "cube_machine")) {
            assertTrue(plan(answer(model, "top", "t", "front", "f", "side", "s", "see_through", true)).seeThrough(), model);
        }
        assertFalse(plan(answer("cube_material", "description", "Gray.")).seeThrough());
        assertFalse(plan(answer("cross", "description", "A glass flower.", "see_through", true)).seeThrough());
    }

    @Test
    void theCountIsAWholeNumberFromOneToEight() {
        Object[][] cases = {{0, 1}, {-3, 1}, {4, 4}, {8, 8}, {64, 8}, {2.5, 1}, {"4", 1}};
        for (Object[] pair : cases) {
            assertEquals(pair[1], plan(answer("cube_material", "description", "x", "output_count", pair[0])).count(), String.valueOf(pair[0]));
            assertEquals(pair[1], plan(answer("cross", "description", "x", "output_count", pair[0])).count(), String.valueOf(pair[0]));
            assertEquals(pair[1], plan(answer("slab", "description", "x", "output_count", pair[0]), DYED_GRID).count(), String.valueOf(pair[0]));
        }
    }

    @Test
    void onlyARealTrueMeansShapeless() {
        assertTrue(plan(answer("cube_material", "description", "x", "is_shapeless_crafting", true)).shapeless());
        assertFalse(plan(answer("cube_material", "description", "x", "is_shapeless_crafting", "true")).shapeless());
    }

    private static void refused(String rawJson, String message) {
        RuntimeException error = assertThrows(RuntimeException.class, () -> parse(rawJson, STONE_GRID));
        assertTrue(error.getMessage().contains(message), error.getMessage());
    }

    @Test
    void anUnusableAnswerIsRefusedWithItsField() {
        refused(answer("liquid"), "unknown model: liquid");
        refused(answer("cube_material", "description", "x", "tool", "sword"), "unknown tool: sword");
        refused(answer("cube_material", "description", "x", "hardness", "medium"), "unknown hardness: medium");
        refused(answer("cube_material", "description", "x", "light", "7"), "unknown light: 7");
        refused(answer("cube_material", "description", "x", "sound", "ladder"), "unknown sound: ladder");
        refused(answer("cube_material", "description", "x", "sound", MISSING), "unknown sound: undefined");
        refused(answer("cube_material", "description", "x", "flammable", "true"), "unknown flammable: true");
        refused(answer("cube_material", "description", "x", "flammable", MISSING), "unknown flammable: undefined");
        refused(answer("cube_material", "description", "x", "see_through", "true"), "unknown see_through: true");
        refused(answer("cube_material", "description", "x", "see_through", MISSING), "unknown see_through: undefined");
        refused(answer("cross", "description", "x", "attach", "wall"), "unknown attach: wall");
        refused(answer("cross", "description", "x", "attach", MISSING), "unknown attach: undefined");
        refused(answer("cube_material", "description", " "), "cube_material with no description");
        refused(answer("slab"), "slab with no description");
        refused(answer("cross"), "cross with no description");
        refused(answer("cube_machine", "top", "x", "front", "x"), "cube_machine with no side");
        refused(answer("cube_natural", "top", ".", "front", "x", "side", "x"), "cube_natural with no top");
        refused(answer("cube_natural", "top", 3, "front", "x", "side", "x"), "cube_natural with no top");
        refused("null", "unknown model");
        assertThrows(RuntimeException.class, () -> parse("not json", STONE_GRID));
    }

    @Test
    void theJudgePromptCarriesTheNameAndEveryRecipeSlot() {
        String prompt = BlockJudge.buildPrompt("Test Block", new String[]{"a", "b", "c", "d", "e", "f", "g", "h", "i"});
        assertTrue(prompt.endsWith("Name: Test Block\nRecipe:\n\tTop Left: a\n\tTop: b\n\tTop Right: c\n\tLeft: d\n\tCenter: e\n\tRight: f\n\tBottom Left: g\n\tBottom: h\n\tBottom Right: i\n"));
    }

    private static BlockJudge.Plan sheetPlan(String kind, boolean seeThrough) {
        return new BlockJudge.Plan("x", kind, "none", "soft", "none", "stone", false, seeThrough, 1, false, null, "material".equals(kind) ? "Gray." : "", "T", "F", "S");
    }

    @Test
    void eachKindAsksForItsOwnSheet() {
        String material = BlockImagePrompts.sheet("Rock", sheetPlan("material", false));
        String natural = BlockImagePrompts.sheet("Log", sheetPlan("natural", false));
        String machine = BlockImagePrompts.sheet("Mill", sheetPlan("machine", false));
        assertTrue(material.startsWith("Create a Minecraft block texture sheet for Rock: three flat square tiles"));
        assertTrue(material.contains("\nDescription: Gray.\n"));
        assertTrue(material.contains("Each square repeats seamlessly by itself"));
        for (String prompt : List.of(natural, machine)) {
            assertTrue(prompt.contains("three flat square faces of the same block"));
            assertTrue(prompt.contains("\nLeft square, the top face: T.\nMiddle square, the front face: F.\nRight square, the side face: S.\n"));
        }
        assertTrue(natural.contains("left edge continues into its own right edge"));
        assertTrue(machine.contains("one large central feature that identifies the block"));
        assertFalse(machine.contains("left edge continues"));
    }

    @Test
    void aSeeThroughBlockIsToldToUseTransparency() {
        String glass = BlockImagePrompts.sheet("Clear Glass", sheetPlan("material", true));
        assertTrue(glass.startsWith("Create a Minecraft block texture sheet for Clear Glass: three flat square tiles of the same see-through glass block side by side"));
        assertTrue(glass.contains("There is no background behind the glass.\n"));
        assertFalse(glass.contains("No border, frame"));
        for (String kind : List.of("natural", "machine")) {
            String opaque = BlockImagePrompts.sheet("Glass Case", sheetPlan(kind, false));
            String seeThrough = BlockImagePrompts.sheet("Glass Case", sheetPlan(kind, true));
            assertTrue(seeThrough.startsWith(opaque + "\nUse the image's transparency as the glass's own: "), kind);
            assertTrue(seeThrough.endsWith("There is no background behind the glass."), kind);
            assertFalse(opaque.contains("transparen"), kind);
        }
    }

    @Test
    void aCrossIsTheItemPromptPlusHowItIsHeld() {
        String below = BlockImagePrompts.cross("Bedrock Sapling", "A gray sapling.", "below");
        assertTrue(below.startsWith(GPTImageGenerator.buildPrompt("Bedrock Sapling", "A gray sapling.")));
        assertTrue(below.contains("stem or base at the bottom of its silhouette"));
        assertTrue(BlockImagePrompts.cross("Iron Chandelier", "x", "above").contains("hangs straight down"));
        assertTrue(BlockImagePrompts.cross("Iron Chain", "x", "any").contains("runs straight up and down"));
    }
}
