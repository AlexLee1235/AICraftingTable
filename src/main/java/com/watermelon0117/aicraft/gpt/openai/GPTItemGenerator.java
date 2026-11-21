package com.watermelon0117.aicraft.gpt.openai;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.watermelon0117.aicraft.common.SpritePipeline;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

public class GPTItemGenerator {
    private static final String inst = "given a minecraft item, please answer following questions:\n" +
            "visual_description(one sentence describe what the finished item(not block) itself looks like in-game. Based on the arrangement of materials)?\n" +
            "is_shapeless_crafting(true if is mixing, combine, etc)?\n" +
            "is_tool?\n" +
            "tier(equivalent in wooden, stone, iron, diamond, netherite, bedrock, golden)?\n" +
            "is_suitable_for_breaking_stone(like pickaxe)?\n" +
            "is_suitable_for_breaking_woods(like axe)?\n" +
            "is_suitable_for_breaking_dirt(like shovel)?\n" +
            "is_suitable_to_plow(like hoe)?\n" +
            "is_melee_weapon?\n" +
            "damage(low, normal, high)?\n" +
            "attack_speed(slow, normal, fast)?\n" +
            "is_edible?\n" +
            "nutrition_value(0 to 20, apple is 4 for reference)?\n" +
            "food_is_solid_or_liquid?\n";
    private final OpenAIChatClient client = new OpenAIChatClient("gpt-4.1", 1.0, 1024, "", "json_object");
    private final Gson gson = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();
    private final GPTImageGenerator imgClient = new GPTImageGenerator();

    /**
     * /aicraft generateItem sends nine "empty" slots. Shown them and asked to go by the materials, gpt-4.1 answered that
     * "The suluski is not a finished item with any visual appearance in-game, as the recipe grid is completely empty",
     * and the image model was then asked to draw that. So with no materials it sees no recipe. Told only to go by the name, it
     * still said a made-up name such as Sulus "does not exist in standard Minecraft"; told the item is new, it described a look
     * in all 30 answers tested, made-up names included. The server's buildItemPrompt does the same, word for word.
     */
    static String buildPrompt(String name, String[] in) {
        if (Arrays.stream(in, 0, 9).allMatch(slot -> slot.equals("empty") || slot.isEmpty())) {
            return inst.replace("Based on the arrangement of materials", "It is a new item: imagine its look from its name") + String.format("Item: %s\n", name)
                    + "\nplease answer in json format";
        }
        String r = String.format("Recipe:\n" +
                        "\tTop Left: %s\n" +
                        "\tTop: %s\n" +
                        "\tTop Right: %s\n" +
                        "\tLeft: %s\n" +
                        "\tCenter: %s\n" +
                        "\tRight: %s\n" +
                        "\tBottom Left: %s\n" +
                        "\tBottom: %s\n" +
                        "\tBottom Right: %s\n",
                in[0], in[1], in[2], in[3], in[4], in[5], in[6], in[7], in[8]
        );
        return inst + String.format("Item: %s\n", name) + r + "\nplease answer in json format";
    }

    public CompletableFuture<GPTItemGeneratorResult> generate(String id, String[] recipe, String user) {
        String prompt = buildPrompt(id, recipe);
        System.out.println(prompt);
        return client.chat(prompt, "Design", user).thenCompose(rawJson -> {
            System.out.println(rawJson);
            ItemResult json = gson.fromJson(rawJson, ItemResult.class);
            if (json.visual_description == null) json.visual_description = "";
            return imgClient.generateItem(id, json.visual_description).thenApply(textureB64 ->
                    new GPTItemGeneratorResult(rawJson, SpritePipeline.process(textureB64).modImageB64(), json));
        });
    }

    public record GPTItemGeneratorResult(String rawJson, String b64Tex, ItemResult json) {
    }
}





















