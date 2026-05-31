package com.watermelon0117.aicraft.init;

import com.watermelon0117.aicraft.AICraftingTable;
import com.watermelon0117.aicraft.common.SpecialItemManager;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class CreativeModeTabInit {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, AICraftingTable.MODID);

    public static final RegistryObject<CreativeModeTab> MAIN = CREATIVE_MODE_TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + AICraftingTable.MODID))
                    .icon(() -> BlockInit.AI_CRAFTING_TABLE_BLOCK_ITEM.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(BlockInit.AI_CRAFTING_TABLE_BLOCK_ITEM.get());
                        for (ItemStack stack : SpecialItemManager.ClientSide.list()) {
                            if (!stack.isEmpty()) {
                                output.accept(stack.copy(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
                            }
                        }
                    })
                    .build());
}
