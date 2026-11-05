package com.watermelon0117.aicraft.init;

import com.watermelon0117.aicraft.AICraftingTable;
import com.watermelon0117.aicraft.blockentities.AIBlockEntity;
import com.watermelon0117.aicraft.blockentities.AICraftingTableBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class BlockEntityInit {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, AICraftingTable.MODID);

    public static final RegistryObject<BlockEntityType<AICraftingTableBlockEntity>> AI_CRAFTING_TABLE_BE = BLOCK_ENTITIES.register("ai_crafting_table",
            () -> BlockEntityType.Builder.of(AICraftingTableBlockEntity::new, BlockInit.AI_CRAFTING_TABLE.get()).build(null));
    public static final RegistryObject<BlockEntityType<AIBlockEntity>> AI_BLOCK_BE = BLOCK_ENTITIES.register("ai_block",
            () -> BlockEntityType.Builder.of(AIBlockEntity::new, BlockInit.AI_BLOCK.get(), BlockInit.AI_SLAB.get(), BlockInit.AI_STAIRS.get(),
                    BlockInit.AI_FENCE.get(), BlockInit.AI_WALL.get(), BlockInit.AI_CROSS.get()).build(null));
/*PROGRAM INSERT POINT*/
}
