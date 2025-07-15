package com.watermelon0117.aicraft.gpt.opanai;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.watermelon0117.aicraft.blockentities.AICraftingTableBlockEntity;
import com.watermelon0117.aicraft.common.ItemStackArray;
import com.watermelon0117.aicraft.common.RecipeManager;
import com.watermelon0117.aicraft.common.SpecialItemManager;
import com.watermelon0117.aicraft.common.TextureManager;
import com.watermelon0117.aicraft.gpt.GeneratedItem;
import com.watermelon0117.aicraft.init.ItemInit;
import com.watermelon0117.aicraft.network.CAddTexturePacket;
import com.watermelon0117.aicraft.network.PacketHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tiers;

import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

public class GPTItemGenerator4 {
    private static final String inst = "given a minecraft item, please answer following questions:\n" +
                    "visual_description(a one sentence describe what the finished item itself looks like in-game. Highlight how the arrangement of materials informs the shape)?\n" +
                    "is_shapeless_crafting(true if is mixing, combine, etc)?\n" +
                    "is_tool?\n" +
                    "tier(equivalent in wooden, stone, iron, diamond, netherite, golden)?\n" +
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
    private final OpenAIChatClient client = new OpenAIChatClient("gpt-4o", 0.0,1024,"","json_object");
    private final Gson gson = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();
    private final GPTImageGenerator imgClient = new GPTImageGenerator();

    private static String buildPrompt(String name, ItemStackArray recipe) {
        String[] in = recipe.getDisplayNames();
        String r = String.format("Recipe:\n\tTop Left: %s\n" +
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
    public CompletableFuture<GeneratedItem> generate(String id, String name, ItemStackArray recipe, String user) {
        String prompt = buildPrompt(id, recipe);
        System.out.println(prompt);
        return client.chat(prompt, "Design", user).thenCompose(rawJson -> {
            System.out.println(rawJson);
            GeneratedItem.ItemResult json = gson.fromJson(rawJson, GeneratedItem.ItemResult.class);
            if (json.visual_description == null) json.visual_description = "";
            return imgClient.generateItem(id, recipe.getDisplayNames(), json.visual_description, user).thenApply(textureBytes -> {
                return GeneratedItem.fromJson(json, id, name, textureBytes);
            });
        });
    }


}


