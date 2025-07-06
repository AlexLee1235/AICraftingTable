package com.watermelon0117.aicraft.gpt;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.watermelon0117.aicraft.blockentities.AICraftingTableBlockEntity;
import com.watermelon0117.aicraft.common.ItemStackArray;
import com.watermelon0117.aicraft.common.RecipeManager;
import com.watermelon0117.aicraft.common.SpecialItemManager;
import com.watermelon0117.aicraft.common.TextureManager;
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
    private final AIChatClient client = new AIChatClient("gpt-4o", 0.0,1024,"","json_object");
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

    public CompletableFuture<ItemStack> generate(String id, String name, ItemStackArray recipe, AICraftingTableBlockEntity be, Predicate<AICraftingTableBlockEntity> predicate, String user) {
        String prompt = buildPrompt(id, recipe);
        System.out.println(prompt);
        return client.chat(prompt, "Design", user).thenCompose(rawJson -> {
            System.out.println(rawJson);
            ItemResult json = gson.fromJson(rawJson, ItemResult.class);
            ItemStack itemStack = makeItem(json, id, name);
            if (json.visual_description == null) json.visual_description = "";
            return imgClient.generateItem(id, recipe.getDisplayNames(), json.visual_description, user).thenApply(textureBytes -> {
                if (predicate.test(be)) {
                    byte[] processedTexture = TextureManager.applyTexture(textureBytes, id);
                    PacketHandler.sendToAllClients(new CAddTexturePacket(id, processedTexture));
                    SpecialItemManager.get().put(itemStack);
                    RecipeManager.get().addRecipe(SpecialItemManager.get().getItem(id), recipe.items, json.is_shapeless_crafting);
                }
                return itemStack;
            });
        });
    }

    private static ItemStack makeItem(ItemResult json, String id, String name) {
        ItemStack itemStack = json.is_edible
                ? new ItemStack(ItemInit.MAIN_FOOD_ITEM.get())
                : new ItemStack(ItemInit.MAIN_ITEM.get());

        CompoundTag tag = new CompoundTag();
        tag.putString("id", id);
        tag.putString("name", name);

        putBooleanIfTrue(tag, "isPickaxe", json.is_suitable_for_breaking_stone);
        putBooleanIfTrue(tag, "isAxe", json.is_suitable_for_breaking_woods);
        putBooleanIfTrue(tag, "isShovel", json.is_suitable_for_breaking_dirt);
        putBooleanIfTrue(tag, "isHoe", json.is_suitable_to_plow);
        putBooleanIfTrue(tag, "isMelee", json.is_melee_weapon);

        if (isToolOrWeapon(json)) {
            tag.putByte("tier", getTier(json));
            tag.putDouble("attackDamage", getDamage(json.damage));
            tag.putDouble("attackSpeed", getAttackSpeed(json.attack_speed));
        }

        if (json.is_edible) {
            tag.putByte("nutrition", (byte) json.nutrition_value);
            tag.putBoolean(json.food_is_solid_or_liquid != null && json.food_is_solid_or_liquid.equals("liquid") ? "isDrink" : "isFood", true);
        }

        itemStack.getOrCreateTag().put("aicraft", tag);
        return itemStack;
    }

    private static void putBooleanIfTrue(CompoundTag tag, String key, boolean condition) {
        if (condition) tag.putBoolean(key, true);
    }

    private static boolean isToolOrWeapon(ItemResult json) {
        return json.is_tool || json.is_melee_weapon ||
                json.is_suitable_for_breaking_stone || json.is_suitable_for_breaking_woods ||
                json.is_suitable_for_breaking_dirt || json.is_suitable_to_plow;
    }

    private static byte getTier(ItemResult json) {
        if (json.tier == null)
            return (byte) Tiers.STONE.ordinal();
        return switch (json.tier) {
            case "wooden" -> (byte) Tiers.WOOD.ordinal();
            case "stone" -> (byte) Tiers.STONE.ordinal();
            case "iron" -> (byte) Tiers.IRON.ordinal();
            case "diamond" -> (byte) Tiers.DIAMOND.ordinal();
            case "netherite" -> (byte) Tiers.NETHERITE.ordinal();
            case "golden" -> (byte) Tiers.GOLD.ordinal();
            default -> (byte) Tiers.STONE.ordinal();
        };
    }

    private static double getDamage(String damage) {
        return switch (damage == null ? "normal" : damage) {
            case "low" -> 1;
            case "high" -> 7;
            default -> 3;
        };
    }

    private static double getAttackSpeed(String attackSpeed) {
        return switch (attackSpeed == null ? "normal" : attackSpeed) {
            case "fast" -> -2.0;
            case "slow" -> -3.2;
            default -> -2.5;
        };
    }

    private static final class ItemResult {
        String visual_description;
        boolean is_shapeless_crafting;
        boolean is_tool;
        String tier;
        boolean is_suitable_for_breaking_stone;
        boolean is_suitable_for_breaking_woods;
        boolean is_suitable_for_breaking_dirt;
        boolean is_suitable_to_plow;
        boolean is_melee_weapon;
        String damage;
        String attack_speed;
        boolean is_edible;
        int nutrition_value;
        String food_is_solid_or_liquid;
    }
}


