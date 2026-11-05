package com.watermelon0117.aicraft.init;

import com.watermelon0117.aicraft.blocks.AIBlock;
import com.watermelon0117.aicraft.blocks.AIBlockTool;
import com.watermelon0117.aicraft.blocks.AICraftingTableBlock;
import com.watermelon0117.aicraft.blocks.MyCraftingTableBlock;
import com.watermelon0117.aicraft.AICraftingTable;
import com.watermelon0117.aicraft.items.AIBlockItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.EnumMap;
import java.util.Map;


public class BlockInit {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS,AICraftingTable.MODID);
    public static final DeferredRegister<Item> BLOCK_ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, AICraftingTable.MODID);
    public static final DeferredRegister<Block> VANILLA_BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "minecraft");
    public static final DeferredRegister<Item> VANILLA_BLOCK_ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, "minecraft");

    public static final RegistryObject<Block> AI_CRAFTING_TABLE = BLOCKS.register("ai_crafting_table",
            () -> new AICraftingTableBlock(BlockBehaviour.Properties.of().strength(1f).explosionResistance(1f)));
    public static final RegistryObject<BlockItem> AI_CRAFTING_TABLE_BLOCK_ITEM = BLOCK_ITEMS.register("ai_crafting_table",
            () -> new BlockItem(AI_CRAFTING_TABLE.get(), new Item.Properties()));
    public static final RegistryObject<Block> MY_CRAFTING_TABLE = VANILLA_BLOCKS.register("crafting_table",
            () -> new MyCraftingTableBlock(BlockBehaviour.Properties.copy(Blocks.CRAFTING_TABLE)));
    public static final RegistryObject<BlockItem> MY_CRAFTING_TABLE_BLOCK_ITEM = VANILLA_BLOCK_ITEMS.register("crafting_table",
            () -> new BlockItem(MY_CRAFTING_TABLE.get(), new Item.Properties()));

    private static final Map<AIBlockTool, RegistryObject<Block>> AI_BLOCKS = new EnumMap<>(AIBlockTool.class);
    private static final Map<AIBlockTool, RegistryObject<AIBlockItem>> AI_BLOCK_ITEMS = new EnumMap<>(AIBlockTool.class);

    static {
        for (AIBlockTool tool : AIBlockTool.values()) {
            RegistryObject<Block> block = BLOCKS.register(tool.blockName(), () -> new AIBlock(tool));
            AI_BLOCKS.put(tool, block);
            AI_BLOCK_ITEMS.put(tool, BLOCK_ITEMS.register(tool.blockName(), () -> new AIBlockItem(block.get(), new Item.Properties())));
        }
    }

    public static Block aiBlock(AIBlockTool tool) {
        return AI_BLOCKS.get(tool).get();
    }

    public static AIBlockItem aiBlockItem(AIBlockTool tool) {
        return AI_BLOCK_ITEMS.get(tool).get();
    }

/*PROGRAM INSERT POINT*/
}
