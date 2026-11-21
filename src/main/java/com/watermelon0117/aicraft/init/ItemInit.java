package com.watermelon0117.aicraft.init;

import com.watermelon0117.aicraft.AICraftingTable;
import com.watermelon0117.aicraft.common.ArmorUV;
import com.watermelon0117.aicraft.items.AIArmorItem;
import com.watermelon0117.aicraft.items.MainFoodItem;
import com.watermelon0117.aicraft.items.MainItem;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ItemInit {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, AICraftingTable.MODID);

    public static final RegistryObject<Item> MAIN_ITEM = ITEMS.register("meta_item", () -> new MainItem(new Item.Properties()));
    public static final RegistryObject<Item> MAIN_FOOD_ITEM = ITEMS.register("meta_food_item", () -> new MainFoodItem(new Item.Properties()));
    public static final RegistryObject<AIArmorItem> AI_HELMET = ITEMS.register("ai_helmet", () -> new AIArmorItem(ArmorItem.Type.HELMET, new Item.Properties()));
    public static final RegistryObject<AIArmorItem> AI_CHESTPLATE = ITEMS.register("ai_chestplate", () -> new AIArmorItem(ArmorItem.Type.CHESTPLATE, new Item.Properties()));
    public static final RegistryObject<AIArmorItem> AI_LEGGINGS = ITEMS.register("ai_leggings", () -> new AIArmorItem(ArmorItem.Type.LEGGINGS, new Item.Properties()));
    public static final RegistryObject<AIArmorItem> AI_BOOTS = ITEMS.register("ai_boots", () -> new AIArmorItem(ArmorItem.Type.BOOTS, new Item.Properties()));
/*PROGRAM INSERT POINT*/

    public static AIArmorItem armorFor(ArmorUV.Slot slot) {
        return switch (slot) {
            case HELMET -> AI_HELMET.get();
            case CHESTPLATE -> AI_CHESTPLATE.get();
            case LEGGINGS -> AI_LEGGINGS.get();
            case BOOTS -> AI_BOOTS.get();
        };
    }
}
