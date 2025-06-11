package com.watermelon0117.aicraft.init;

import com.watermelon0117.aicraft.AICraftingTable;
import com.watermelon0117.aicraft.blocks.AICraftingTableBlock;
import com.watermelon0117.aicraft.items.MainItem;
import com.watermelon0117.aicraft.menu.AICraftingTableMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class MenuInit {
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(ForgeRegistries.MENU_TYPES, AICraftingTable.MODID);
    public static final RegistryObject<MenuType<AICraftingTableMenu>> MAIN_MENU = MENU_TYPES.register("ai_crafting_table_menu",
            () -> IForgeMenuType.create(AICraftingTableMenu::new));
}
