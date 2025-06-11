package com.watermelon0117.aicraft.init;

import com.watermelon0117.aicraft.blockentities.*;
import com.watermelon0117.aicraft.blocks.*;
import com.watermelon0117.aicraft.AICraftingTable;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class BlockEntityInit {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, AICraftingTable.MODID);

    public static final RegistryObject<BlockEntityType<AICraftingTableBlockEntity>> AI_CRAFTING_TABLE_BE = BLOCK_ENTITIES.register("ai_crafting_table",
            () -> BlockEntityType.Builder.of(AICraftingTableBlockEntity::new, BlockInit.AI_CRAFTING_TABLE.get()).build(null));
/*PROGRAM INSERT POINT*/
}
