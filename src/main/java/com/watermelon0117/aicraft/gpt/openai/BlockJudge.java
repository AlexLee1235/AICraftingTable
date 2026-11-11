package com.watermelon0117.aicraft.gpt.openai;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.watermelon0117.aicraft.blocks.AIBlockShape;
import com.watermelon0117.aicraft.common.ShapedBlockRecipe;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Decides what a name and a recipe make, for an own OpenAI key, as the texture server's {@code blocks.js} does.
 * The prompt was tested on real names and the rules on real answers: change both copies, and test the prompt again.
 */
public final class BlockJudge {
    static final String MODEL = "gpt-6.1-terra";
    /** Reasoning counts toward the limit and it thinks before it answers, so this is well above what the answer needs. */
    static final int MAX_COMPLETION_TOKENS = 4000;
    static final Duration TIMEOUT = Duration.ofSeconds(60);
    static final String SCHEMA_NAME = "block_judge";

    private static final List<String> MODELS = List.of("item", "cube_material", "cube_natural", "cube_machine", "slab", "stairs", "fence", "wall", "cross", "other");
    private static final List<String> TOOLS = List.of("pickaxe", "axe", "shovel", "hoe", "none");
    private static final List<String> HARDNESSES = List.of("instant", "soft", "normal", "hard", "unbreakable");
    private static final List<String> LIGHTS = List.of("none", "dim", "bright");
    private static final List<String> ATTACHMENTS = List.of("below", "above", "any");
    private static final List<String> SOUNDS = List.of("stone", "deepslate", "netherrack", "nether_bricks", "bone", "metal", "copper", "netherite", "amethyst", "glass", "wood",
            "wool", "grass", "gravel", "sand", "mud", "snow", "slime", "honey", "coral", "wart", "sculk");
    private static final List<String> REQUIRED = List.of("model", "tool", "hardness", "light", "sound", "flammable", "see_through", "attach", "output_count", "is_shapeless_crafting", "description", "top", "front", "side");
    private static final List<String> FACES = List.of("top", "front", "side");
    private static final List<String> RECIPE_SLOTS = List.of("Top Left", "Top", "Top Right", "Left", "Center", "Right", "Bottom Left", "Bottom", "Bottom Right");

    /** Which sheet each model draws. A cross is drawn like an item instead. */
    private static final Map<String, String> KIND_OF_MODEL = Map.of(
            "cube_material", "material",
            "cube_natural", "natural",
            "cube_machine", "machine",
            "slab", "material",
            "stairs", "material",
            "fence", "material",
            "wall", "material");
    /** The shapes whose name must say so for the judge's answer to stand. */
    private static final Map<String, AIBlockShape> NAMED_SHAPES = Map.of(
            "slab", AIBlockShape.SLAB,
            "stairs", AIBlockShape.STAIRS,
            "fence", AIBlockShape.FENCE,
            "wall", AIBlockShape.WALL);
    /** What vanilla's own recipe for each shape makes. Any other recipe for one, such as dyeing it, makes what the judge says. */
    private static final Map<String, Integer> VANILLA_COUNT = Map.of("slab", 6, "stairs", 4, "fence", 3, "wall", 6);
    private static final int MAX_COUNT = 8;
    /** What JavaScript counts as white space, which Java's \s stops short of. */
    private static final String SPACE = "\\s\\u00a0\\u1680\\u2000-\\u200a\\u2028\\u2029\\u202f\\u205f\\u3000\\ufeff";

    private static final String INSTRUCTIONS = "You decide what the result of a new Minecraft crafting recipe is. If it is a block you also describe it, so that an image model can draw its textures. "
            + "Given its name and its crafting recipe, fill in the schema. Judge by the name and the recipe together.\n\n"
            + "model, choose one:\n"
            + "- \"item\": anything held, worn, eaten or thrown, such as tools, weapons, armor, food, potions, charms, rods and crafting materials, even when the name contains the word block. "
            + "If you are unsure, choose \"item\".\n"
            + "- \"cube_material\": a full solid cube whose six faces look the same, such as stone, bricks, an ore, wool, planks or a block of metal.\n"
            + "- \"cube_natural\": a full solid cube with no machinery whose top differs from its sides, such as a log, grass, sandstone or a hay bale.\n"
            + "- \"cube_machine\": a full solid cube with a front that shows what it does, such as a crafting table, furnace, bookshelf, generator, chest or barrel.\n"
            + "- \"slab\", \"stairs\", \"fence\", \"wall\": that vanilla shape, made of one material.\n"
            + "- \"cross\": two flat upright pictures of the object, crossing each other in an X when seen from above. "
            + "Only for something placed in the world that still looks right drawn that way: it fits inside one block, looks the same from every side, and stands on the block below or hangs from the block above. "
            + "Such as a sapling, flower, mushroom, fern, cobweb, hanging vine, amethyst bud, stalactite, candle, torch, lantern, chandelier, chain or rope. "
            + "Never a tool, weapon, food or crafting material, even a thin one or one named a cluster.\n"
            + "- \"other\": anything else placed in the world, which none of the shapes above can draw, such as doors, beds, banners, buttons, ladders, signs and rails.\n\n"
            + "The fields below describe the block once placed. For \"item\" and \"other\" use tool \"none\", hardness \"normal\", light \"none\", sound \"stone\", flammable false, see_through false, attach \"below\" and output_count 1.\n\n"
            + "tool: what mines it fastest. \"pickaxe\" for stone, metal and machines of them, \"axe\" for wood, \"shovel\" for dirt and sand, \"hoe\" for leaves and hay, \"none\" for wool and plants.\n"
            + "hardness: \"instant\" like a sapling, flower or TNT, which one hit breaks, \"soft\" like dirt or wool, \"normal\" like stone or wood, \"hard\" like obsidian, \"unbreakable\" like bedrock.\n"
            + "light: \"none\", \"dim\" like a redstone torch, or \"bright\" like glowstone.\n"
            + "sound: what it sounds like to walk on, place and break. Choose the closest: \"stone\" like stone, bricks, ores, concrete or a furnace, \"deepslate\", \"netherrack\", \"nether_bricks\", "
            + "\"bone\" like a bone block, \"metal\" like a block of iron, gold or diamond, \"copper\", \"netherite\", \"amethyst\", \"glass\" like glass, ice, glowstone or a sea lantern, "
            + "\"wood\" like planks, logs, a crafting table or a chest, \"wool\" like wool or cloth, \"grass\" like grass, leaves, hay, TNT or plants, "
            + "\"gravel\" like dirt, gravel or clay, \"sand\", \"mud\", \"snow\", \"slime\" like slime or jelly, \"honey\", \"coral\", \"wart\" like a nether wart block or meat, \"sculk\".\n"
            + "flammable: true if fire burns it away, like wood, wool, leaves, hay, plants or TNT. false for stone, metal, glass, dirt and sand.\n"
            + "see_through: true for a block you can see through, such as glass, stained glass, tinted glass or ice, false otherwise.\n"
            + "attach: what holds a \"cross\" in place. \"below\" if it stands on the block below, like a flower, candle or torch. \"above\" if it hangs from the block above, like a chandelier or hanging vine. "
            + "\"any\" if it needs no support and stacks on itself, like a chain or rope. \"below\" for every other model.\n"
            + "output_count: how many one craft makes, from 1 to 8. Use 1 for a compressed or valuable block, and more for a cheap building block, as one log makes 4 planks. "
            + "A recipe that only changes blocks already made, such as dyeing them, makes as many as it uses, as 8 terracotta and a dye make 8 stained terracotta.\n"
            + "is_shapeless_crafting: true if the recipe is mixing or combining, so the layout does not matter.\n\n"
            + "description: for \"cube_material\", \"slab\", \"stairs\", \"fence\", \"wall\" and \"cross\", otherwise an empty string. One sentence saying what it looks like: its colors and the pattern on it. "
            + "For a see-through block, also say which parts are clear, which are tinted and which are solid. "
            + "For \"cross\", describe only the object itself.\n"
            + "top, front, side: for \"cube_natural\" and \"cube_machine\", otherwise empty strings. A short phrase for each face naming its colors and shapes, with no period at the end. "
            + "If two faces look the same, say so. For a see-through block, say which parts of each face are clear, tinted or solid.\n\n"
            + "Describe only what can be seen. Let the materials in the recipe decide the colors. Do not mention pixels, resolution, lighting or the shape of the cube.\n\n";

    /**
     * Everything a block needs, cleaned, with what code decides filled in.
     *
     * @param kind   which sheet it draws, or null for a cross
     * @param attach what holds a cross in place, or null for any other model
     */
    public record Plan(String model, @Nullable String kind, String tool, String hardness, String light, String sound, boolean flammable,
                       boolean seeThrough, int count, boolean shapeless, @Nullable String attach, String description, String top, String front, String side) {
        boolean isCross() {
            return "cross".equals(model);
        }
    }

    private BlockJudge() {
    }

    static JsonObject schema() {
        JsonObject properties = new JsonObject();
        properties.add("model", enumOf(MODELS));
        properties.add("tool", enumOf(TOOLS));
        properties.add("hardness", enumOf(HARDNESSES));
        properties.add("light", enumOf(LIGHTS));
        properties.add("sound", enumOf(SOUNDS));
        properties.add("flammable", typeOf("boolean"));
        properties.add("see_through", typeOf("boolean"));
        properties.add("attach", enumOf(ATTACHMENTS));
        properties.add("output_count", typeOf("integer"));
        properties.add("is_shapeless_crafting", typeOf("boolean"));
        properties.add("description", typeOf("string"));
        for (String face : FACES) {
            properties.add(face, typeOf("string"));
        }
        JsonObject schema = typeOf("object");
        schema.addProperty("additionalProperties", false);
        schema.add("required", arrayOf(REQUIRED));
        schema.add("properties", properties);
        return schema;
    }

    private static JsonObject typeOf(String type) {
        JsonObject property = new JsonObject();
        property.addProperty("type", type);
        return property;
    }

    private static JsonObject enumOf(List<String> values) {
        JsonObject property = typeOf("string");
        property.add("enum", arrayOf(values));
        return property;
    }

    private static JsonArray arrayOf(List<String> values) {
        JsonArray array = new JsonArray();
        values.forEach(array::add);
        return array;
    }

    /** It is not told when a recipe has a slab's layout: told so, the judge called a Thin Copper Plate of three copper ingots a slab. */
    public static String buildPrompt(String name, String[] grid) {
        StringBuilder prompt = new StringBuilder(INSTRUCTIONS).append("Name: ").append(name).append("\nRecipe:\n");
        for (int i = 0; i < RECIPE_SLOTS.size(); i++) {
            prompt.append('\t').append(RECIPE_SLOTS.get(i)).append(": ").append(grid[i]).append('\n');
        }
        return prompt.toString();
    }

    /**
     * Unsure means item: a block taken for an item is what a player would get anyway, but a slab of a plate loses the plate.
     * There is no shape to draw an "other" with, so the player gets an item for it too.
     *
     * @return the block to draw, or empty for an item
     * @throws RuntimeException on an unusable answer
     */
    public static Optional<Plan> parse(String rawJson, String name, String[] grid) {
        JsonElement parsed = JsonParser.parseString(rawJson);
        JsonObject answer = parsed.isJsonObject() ? parsed.getAsJsonObject() : new JsonObject();
        String model = oneOf(MODELS, answer.get("model"), "model");
        AIBlockShape named = NAMED_SHAPES.get(model);
        if (model.equals("item") || model.equals("other") || named != null && !ShapedBlockRecipe.names(named, name)) {
            return Optional.empty();
        }
        String kind = KIND_OF_MODEL.get(model);
        boolean hasFaces = "natural".equals(kind) || "machine".equals(kind);
        String tool = oneOf(TOOLS, answer.get("tool"), "tool");
        String hardness = oneOf(HARDNESSES, answer.get("hardness"), "hardness");
        String light = oneOf(LIGHTS, answer.get("light"), "light");
        String sound = oneOf(SOUNDS, answer.get("sound"), "sound");
        boolean flammable = booleanOf(answer.get("flammable"), "flammable");
        // A cross is already drawn on a transparent background, and its pixels are only clear or solid.
        boolean seeThrough = booleanOf(answer.get("see_through"), "see_through") && !model.equals("cross");
        int count = countOf(model, answer.get("output_count"), grid);
        JsonElement shapeless = answer.get("is_shapeless_crafting");
        boolean isShapeless = shapeless != null && shapeless.isJsonPrimitive() && shapeless.getAsJsonPrimitive().isBoolean() && shapeless.getAsBoolean();
        String attach = model.equals("cross") ? oneOf(ATTACHMENTS, answer.get("attach"), "attach") : null;
        String description = hasFaces ? "" : required(answer, "description", model);
        String top = hasFaces ? required(answer, "top", model) : "";
        String front = hasFaces ? required(answer, "front", model) : "";
        String side = hasFaces ? required(answer, "side", model) : "";
        return Optional.of(new Plan(model, kind, tool, hardness, light, sound, flammable, seeThrough, count, isShapeless, attach, description, top, front, side));
    }

    private static String oneOf(List<String> values, @Nullable JsonElement value, String field) {
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString() || !values.contains(value.getAsString())) {
            throw new IllegalArgumentException("Block judge answered an unknown " + field + ": " + shown(value));
        }
        return value.getAsString();
    }

    private static boolean booleanOf(@Nullable JsonElement value, String field) {
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isBoolean()) {
            throw new IllegalArgumentException("Block judge answered an unknown " + field + ": " + shown(value));
        }
        return value.getAsBoolean();
    }

    private static String shown(@Nullable JsonElement value) {
        return value == null ? "undefined" : value.isJsonPrimitive() ? value.getAsString() : value.toString();
    }

    /** Vanilla's count only for vanilla's own recipe of that shape, so that dyeing a slab does not make 6 of it. */
    private static int countOf(String model, @Nullable JsonElement count, String[] grid) {
        RecipeShapeMatcher.Match shape = RecipeShapeMatcher.matchBlockShape(grid);
        if (shape != null && shape.shapeName().toLowerCase(Locale.ROOT).equals(model)) {
            return VANILLA_COUNT.get(model);
        }
        if (count == null || !count.isJsonPrimitive() || !count.getAsJsonPrimitive().isNumber()) {
            return 1;
        }
        double value = count.getAsDouble();
        return value == Math.rint(value) && !Double.isInfinite(value) ? (int) Math.min(Math.max(value, 1), MAX_COUNT) : 1;
    }

    private static String required(JsonObject answer, String field, String model) {
        JsonElement raw = answer.get(field);
        String value = raw != null && raw.isJsonPrimitive() && raw.getAsJsonPrimitive().isString()
                ? raw.getAsString().replaceAll("[" + SPACE + "]+", " ").strip() : "";
        // The face prompts add their own period.
        String cleaned = field.equals("description") ? value : value.replaceAll("[." + SPACE + "]+$", "");
        if (cleaned.isEmpty()) {
            throw new IllegalArgumentException("Block judge answered " + model + " with no " + field);
        }
        return cleaned;
    }
}
