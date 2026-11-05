package com.watermelon0117.aicraft.init;

import com.watermelon0117.aicraft.blocks.AIBlock;
import com.watermelon0117.aicraft.blocks.AIBlockShape;
import com.watermelon0117.aicraft.blocks.AICraftingTableBlock;
import com.watermelon0117.aicraft.blocks.AICrossBlock;
import com.watermelon0117.aicraft.blocks.AIFenceBlock;
import com.watermelon0117.aicraft.blocks.AISlabBlock;
import com.watermelon0117.aicraft.blocks.AIStairBlock;
import com.watermelon0117.aicraft.blocks.AIWallBlock;
import com.watermelon0117.aicraft.blocks.MyCraftingTableBlock;
import com.watermelon0117.aicraft.AICraftingTable;
import com.watermelon0117.aicraft.items.AIBlockItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;


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

    public static final RegistryObject<Block> AI_BLOCK = BLOCKS.register("ai_block", AIBlock::new);
    public static final RegistryObject<AIBlockItem> AI_BLOCK_ITEM = aiBlockItem("ai_block", AI_BLOCK);
    public static final RegistryObject<Block> AI_SLAB = BLOCKS.register("ai_slab", AISlabBlock::new);
    public static final RegistryObject<AIBlockItem> AI_SLAB_ITEM = aiBlockItem("ai_slab", AI_SLAB);
    public static final RegistryObject<Block> AI_STAIRS = BLOCKS.register("ai_stairs", AIStairBlock::new);
    public static final RegistryObject<AIBlockItem> AI_STAIRS_ITEM = aiBlockItem("ai_stairs", AI_STAIRS);
    public static final RegistryObject<Block> AI_FENCE = BLOCKS.register("ai_fence", AIFenceBlock::new);
    public static final RegistryObject<AIBlockItem> AI_FENCE_ITEM = aiBlockItem("ai_fence", AI_FENCE);
    public static final RegistryObject<Block> AI_WALL = BLOCKS.register("ai_wall", AIWallBlock::new);
    public static final RegistryObject<AIBlockItem> AI_WALL_ITEM = aiBlockItem("ai_wall", AI_WALL);
    public static final RegistryObject<Block> AI_CROSS = BLOCKS.register("ai_cross", AICrossBlock::new);
    public static final RegistryObject<AIBlockItem> AI_CROSS_ITEM = aiBlockItem("ai_cross", AI_CROSS);

    private static RegistryObject<AIBlockItem> aiBlockItem(String name, RegistryObject<Block> block) {
        return BLOCK_ITEMS.register(name, () -> new AIBlockItem(block.get(), new Item.Properties()));
    }

    public static AIBlockItem itemFor(AIBlockShape shape) {
        return switch (shape) {
            case CUBE -> AI_BLOCK_ITEM.get();
            case SLAB -> AI_SLAB_ITEM.get();
            case STAIRS -> AI_STAIRS_ITEM.get();
            case FENCE -> AI_FENCE_ITEM.get();
            case WALL -> AI_WALL_ITEM.get();
            case CROSS -> AI_CROSS_ITEM.get();
        };
    }

/*PROGRAM INSERT POINT*/
}
