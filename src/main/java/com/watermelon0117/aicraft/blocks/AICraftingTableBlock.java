package com.watermelon0117.aicraft.blocks;

import com.watermelon0117.aicraft.init.BlockEntityInit;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class AICraftingTableBlock extends Block implements EntityBlock {
    
    public AICraftingTableBlock(Properties p_41383_) {
        super(p_41383_);
    }
    
    
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos p_153215_, BlockState p_153216_) {
        return BlockEntityInit.AI_CRAFTING_TABLE_BE.get().create(p_153215_, p_153216_);
    }
}