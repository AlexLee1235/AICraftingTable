package com.watermelon0117.aicraft.gpt;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.watermelon0117.aicraft.init.ItemInit;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tiers;

public record GeneratedItem(ItemStack itemStack, byte[] rawTexture, boolean shapeless) {
    private static final Gson gson = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();
    public static GeneratedItem fromJson(ItemResult json, String id, String name, byte[] rawTexture){
        ItemStack itemStack1=makeItem(json, id, name);
        return new GeneratedItem(itemStack1, rawTexture, json.is_shapeless_crafting);
    }
    public static GeneratedItem fromJson(String rawJson, String id, String name, byte[] rawTexture) {
        var json = gson.fromJson(rawJson, ItemResult.class);
        return GeneratedItem.fromJson(json, id, name, rawTexture);
    }

    public static ItemStack makeItem(ItemResult json, String id, String name) {
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

    public static final class ItemResult {
        public String visual_description;
        public boolean is_shapeless_crafting;
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
