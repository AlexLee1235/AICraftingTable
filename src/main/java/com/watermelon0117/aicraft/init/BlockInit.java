package com.watermelon0117.aicraft.init;

import com.watermelon0117.aicraft.blocks.*;
import com.watermelon0117.aicraft.AICraftingTable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.material.MaterialColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;



public class BlockInit {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS,AICraftingTable.MODID);
    public static final DeferredRegister<Item> BLOCK_ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, AICraftingTable.MODID);

	public static final RegistryObject<Block> AI_CRAFTING_TABLE = BLOCKS.register("ai_crafting_table",
            () -> new AICraftingTableBlock(BlockBehaviour.Properties.of(Material.STONE).strength(1f).explosionResistance(1f)));
    public static final RegistryObject<BlockItem> AI_CRAFTING_TABLE_BLOCK_ITEM = BLOCK_ITEMS.register("ai_crafting_table",
            () -> new BlockItem(AI_CRAFTING_TABLE.get(), new Item.Properties().tab(AICraftingTable.TAB)));
/*PROGRAM INSERT POINT*/
}
