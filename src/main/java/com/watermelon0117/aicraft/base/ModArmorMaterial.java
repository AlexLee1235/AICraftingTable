package com.watermelon0117.aicraft.base;

import com.watermelon0117.aicraft.AICraftingTable;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.function.Supplier;

public record ModArmorMaterial(String name, int durabilityMultiplier, int[] protection, int enchantability, SoundEvent equipSound,
                               float toughness, float knockbackResistance, Supplier<Ingredient> repairMaterial) implements ArmorMaterial {
    private static final int[] DURA_PER_SLOT = new int[] {13, 15, 16, 11};
    @Override
    public int getDurabilityForSlot(EquipmentSlot p_40410_) {
        return DURA_PER_SLOT[p_40410_.getIndex()]*durabilityMultiplier;
    }

    @Override
    public int getDefenseForSlot(EquipmentSlot p_40411_) {
        return this.protection[p_40411_.getIndex()];
    }

    @Override
    public int getEnchantmentValue() {
        return this.enchantability;
    }

    @Override
    public SoundEvent getEquipSound() {
        return this.equipSound;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return this.repairMaterial.get();
    }

    @Override
    public String getName() {
        return AICraftingTable.MODID + ":" + this.name;
    }

    @Override
    public float getToughness() {
        return this.toughness;
    }

    @Override
    public float getKnockbackResistance() {
        return this.knockbackResistance;
    }
}
