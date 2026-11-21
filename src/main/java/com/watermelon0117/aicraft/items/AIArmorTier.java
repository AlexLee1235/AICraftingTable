package com.watermelon0117.aicraft.items;

import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ArmorMaterials;

/**
 * The vanilla armor set an AI piece acts as, from the same {@code tier} its tools use: the item step answers a tier for every item.
 * Each set brings vanilla's defense, toughness, knockback resistance, durability, enchantability and equip sound.
 */
public enum AIArmorTier {
    LEATHER, CHAIN, IRON, GOLD, DIAMOND, NETHERITE;

    /**
     * @param toolTier the stack's {@code tier} byte, the ordinal of vanilla's {@code Tiers}: wood, stone, iron, diamond, gold, netherite.
     *                 GeneratedItem stores bedrock as netherite. Anything else is iron, the base the four items are registered with.
     */
    public static AIArmorTier ofToolTier(int toolTier) {
        return switch (toolTier) {
            case 0 -> LEATHER;
            case 1 -> CHAIN;
            case 2 -> IRON;
            case 3 -> DIAMOND;
            case 4 -> GOLD;
            case 5 -> NETHERITE;
            default -> IRON;
        };
    }

    public ArmorMaterial material() {
        return switch (this) {
            case LEATHER -> ArmorMaterials.LEATHER;
            case CHAIN -> ArmorMaterials.CHAIN;
            case IRON -> ArmorMaterials.IRON;
            case GOLD -> ArmorMaterials.GOLD;
            case DIAMOND -> ArmorMaterials.DIAMOND;
            case NETHERITE -> ArmorMaterials.NETHERITE;
        };
    }
}
