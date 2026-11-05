package com.watermelon0117.aicraft.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

/** Fences connect by tags, which cannot differ between block entities, so every AI fence counts as a wooden one. */
public class AIFenceBlock extends FenceBlock implements AIPlacedBlock {
    public AIFenceBlock() {
        super(AIPlacedBlock.properties().forceSolidOn());
        registerDefaultState(defaultBlockState().setValue(LIGHT, AIBlockLight.NONE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(LIGHT);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return AIPlacedBlock.withLightOfItem(super.getStateForPlacement(context), context);
    }

    @Override
    public float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        return AIPlacedBlock.destroyProgress(player, level, pos);
    }
}
