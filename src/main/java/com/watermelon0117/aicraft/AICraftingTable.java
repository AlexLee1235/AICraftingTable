package com.watermelon0117.aicraft;

import com.watermelon0117.aicraft.common.AICraftingTableCommonConfigs;
import com.watermelon0117.aicraft.init.*;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;


@Mod(AICraftingTable.MODID)
public class AICraftingTable {
    public static final String MODID="aicraft";

    public AICraftingTable(FMLJavaModLoadingContext context){
	IEventBus bus = context.getModEventBus();

        BlockInit.BLOCKS.register(bus);
        BlockInit.BLOCK_ITEMS.register(bus);
        BlockInit.VANILLA_BLOCKS.register(bus);
        BlockInit.VANILLA_BLOCK_ITEMS.register(bus);
        ItemInit.ITEMS.register(bus);
        EntityInit.ENTITIES.register(bus);
        BlockEntityInit.BLOCK_ENTITIES.register(bus);
        RecipeInit.RECIPE_SERIALIZERS.register(bus);
        FluidInit.FLUID_TYPES.register(bus);
        FluidInit.FLUIDS.register(bus);
        MenuInit.MENU_TYPES.register(bus);
        ParticleInit.PARTICLE_TYPES.register(bus);
        ArgumentInit.ARGUMENTS.register(bus);
        CreativeModeTabInit.CREATIVE_MODE_TABS.register(bus);
        context.registerConfig(ModConfig.Type.COMMON, AICraftingTableCommonConfigs.SPEC);
    }
}
