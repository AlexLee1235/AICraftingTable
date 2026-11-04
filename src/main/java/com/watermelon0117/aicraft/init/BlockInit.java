package com.watermelon0117.aicraft.init;

import com.watermelon0117.aicraft.blocks.AIBlock;
import com.watermelon0117.aicraft.blocks.AIBlockMaterial;
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

    private static final Map<AIBlockMaterial, RegistryObject<Block>> AI_BLOCKS = new EnumMap<>(AIBlockMaterial.class);
    private static final Map<AIBlockMaterial, RegistryObject<AIBlockItem>> AI_BLOCK_ITEMS = new EnumMap<>(AIBlockMaterial.class);

    static {
        for (AIBlockMaterial material : AIBlockMaterial.values()) {
            RegistryObject<Block> block = BLOCKS.register(material.blockName(), () -> new AIBlock(material));
            AI_BLOCKS.put(material, block);
            AI_BLOCK_ITEMS.put(material, BLOCK_ITEMS.register(material.blockName(), () -> new AIBlockItem(block.get(), new Item.Properties())));
        }
    }

    public static Block aiBlock(AIBlockMaterial material) {
        return AI_BLOCKS.get(material).get();
    }

    public static AIBlockItem aiBlockItem(AIBlockMaterial material) {
        return AI_BLOCK_ITEMS.get(material).get();
    }

/*PROGRAM INSERT POINT*/
}
