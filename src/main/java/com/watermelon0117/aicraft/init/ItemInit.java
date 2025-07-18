package com.watermelon0117.aicraft.init;

import com.watermelon0117.aicraft.items.*;
import com.watermelon0117.aicraft.AICraftingTable;
import com.watermelon0117.aicraft.base.ModArmorMaterial;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ItemInit {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, AICraftingTable.MODID);

	public static final RegistryObject<Item> MAIN_ITEM = ITEMS.register("meta_item", () -> new MainItem(new Item.Properties()));
	public static final RegistryObject<Item> MAIN_FOOD_ITEM = ITEMS.register("meta_food_item", () -> new MainFoodItem(new Item.Properties()));
/*PROGRAM INSERT POINT*/
}
