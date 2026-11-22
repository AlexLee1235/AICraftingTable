package com.watermelon0117.aicraft.gpt;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.watermelon0117.aicraft.common.ArmorTexture;
import com.watermelon0117.aicraft.common.ArmorUV;
import com.watermelon0117.aicraft.gpt.openai.ItemResult;
import com.watermelon0117.aicraft.init.ItemInit;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tiers;

import java.util.Base64;
import java.util.Optional;

/** @param worn the picture of an armor piece as worn; null for every other item */
public record GeneratedItem(ItemStack itemStack, byte[] rawTexture, boolean shapeless, ArmorTexture worn) implements GeneratedResult {
    private static final Gson gson = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();
    /** @throws IllegalArgumentException if the answer has no item, or an armor piece's answer has no worn picture of its slot */
    public static GeneratedItem fromResponse(GeneratedResult.Response response, String id, String name) {
        if (response.b64Image() == null || response.json() == null) {
            throw new IllegalArgumentException("The texture server's answer has no item");
        }
        ItemResult json = gson.fromJson(response.json(), ItemResult.class);
        ArmorTexture worn = null;
        if (ArmorUV.Slot.ofAnswer(json.armor_slot).isPresent()) {
            GeneratedResult.Armor armor = response.armor();
            if (armor == null || armor.atlas() == null || armor.uv() == null) {
                throw new IllegalArgumentException("The texture server's answer has no worn picture for this armor");
            }
            worn = new ArmorTexture(Base64.getDecoder().decode(armor.atlas()), armor.uv());
        }
        return of(json, id, name, Base64.getDecoder().decode(response.b64Image()), worn);
    }

    /**
     * @param worn the worn picture, which an answer naming a slot must have and any other answer must not
     * @throws IllegalArgumentException if the worn picture is missing, unexpected, unreadable or for another slot
     */
    public static GeneratedItem of(ItemResult json, String id, String name, byte[] rawTexture, ArmorTexture worn) {
        Optional<ArmorUV.Slot> slot = ArmorUV.Slot.ofAnswer(json.armor_slot);
        if (slot.isPresent() != (worn != null)) {
            throw new IllegalArgumentException(slot.isPresent() ? "An armor answer came without its worn picture" : "A worn picture came without an armor answer");
        }
        if (worn != null && worn.check().slot() != slot.get()) {
            throw new IllegalArgumentException("The worn picture is for another slot than " + slot.get().key());
        }
        return new GeneratedItem(makeItem(json, id, name), rawTexture, json.is_shapeless_crafting, worn);
    }

    /** The item prompt asks for no count, and an item has always come one at a time. */
    @Override
    public int count() {
        return 1;
    }

    public static ItemStack makeItem(ItemResult json, String id, String name) {
        Optional<ArmorUV.Slot> slot = ArmorUV.Slot.ofAnswer(json.armor_slot);
        if (slot.isPresent()) {
            // Worn armor wins over a tool or food answer: one item cannot be both, and the slot is what the AI made it for.
            // Its protection is the vanilla set of its tier (AIArmorTier), as a tool's mining is.
            return ItemInit.armorFor(slot.get()).makeStack(id, name, tierByte(json.tier), "bedrock".equals(json.tier));
        }
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
            if (json.tier != null && json.tier.contentEquals("bedrock"))
                tag.putBoolean("invincible", true);
            tag.putDouble("attackDamage", getDamage(json.damage));
            tag.putDouble("attackSpeed", getAttackSpeed(json.attack_speed));
        }

        if (json.is_edible) {
            tag.putByte("nutrition", (byte) json.nutrition_value);
            tag.putBoolean(json.food_is_solid_or_liquid != null && json.food_is_solid_or_liquid.equals("liquid") ? "isDrink" : "isFood", true);
        }

        CompoundTag root = new CompoundTag();
        root.put("aicraft", tag);
        // setTag marks a tool undamaged, as saving and sending it do. Ingredients match by tags, so a new tool must not differ from the same tool sent or saved.
        itemStack.setTag(root);
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
        return tierByte(json.tier);
    }

    /** @return the stored {@code tier} byte for the item step's tier answer, as tools and armor keep it */
    public static byte tierByte(String tier) {
        if (tier == null)
            return (byte) Tiers.STONE.ordinal();
        return switch (tier) {
            case "wooden" -> (byte) Tiers.WOOD.ordinal();
            case "stone" -> (byte) Tiers.STONE.ordinal();
            case "iron" -> (byte) Tiers.IRON.ordinal();
            case "diamond" -> (byte) Tiers.DIAMOND.ordinal();
            case "netherite", "bedrock" -> (byte) Tiers.NETHERITE.ordinal();
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
}
