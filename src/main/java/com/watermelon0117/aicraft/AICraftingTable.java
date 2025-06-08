package com.watermelon0117.aicraft;

import com.watermelon0117.aicraft.init.*;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;


@Mod(AICraftingTable.MODID)
public class AICraftingTable {
    public static final String MODID="aicraft";

    public AICraftingTable(){
        IEventBus bus= FMLJavaModLoadingContext.get().getModEventBus();
        //test
        BlockInit.BLOCKS.register(bus);
        BlockInit.BLOCK_ITEMS.register(bus);
        ItemInit.ITEMS.register(bus);
        EntityInit.ENTITIES.register(bus);
        BlockEntityInit.BLOCK_ENTITIES.register(bus);
        RecipeInit.RECIPE_SERIALIZERS.register(bus);
        FluidInit.FLUID_TYPES.register(bus);
        FluidInit.FLUIDS.register(bus);
    }
 
    public static final CreativeModeTab TAB = new CreativeModeTab(MODID) {
        @Override
        public ItemStack makeIcon() {
            return Items.CRAFTING_TABLE.getDefaultInstance();
        }
    };
}
