package com.watermelon0117.aicraft.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class AIStairBlock extends StairBlock implements AIPlacedBlock {
    /** Vanilla passes clicks, ticks and such on to its base block, and stone does nothing with them. */
    public AIStairBlock() {
        super(Blocks.STONE::defaultBlockState, AIPlacedBlock.properties());
        registerDefaultState(defaultBlockState().setValue(LIGHT, AIBlockLight.NONE).setValue(TRANSPARENCY, AIBlockTransparency.OPAQUE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(LIGHT, TRANSPARENCY);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return AIPlacedBlock.withStateOfItem(super.getStateForPlacement(context), context);
    }

    /** Vanilla only tells its base block, which has no block entity to remove. */
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        super.onRemove(state, level, pos, newState, moved);
        if (!state.is(newState.getBlock())) {
            level.removeBlockEntity(pos);
        }
    }

    @Override
    public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return AIPlacedBlock.emptyIfSeeThrough(state, super.getOcclusionShape(state, level, pos));
    }

    @Override
    public VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return AIPlacedBlock.emptyIfSeeThrough(state, super.getVisualShape(state, level, pos, context));
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return AIPlacedBlock.propagatesSkylightDown(state, super.propagatesSkylightDown(state, level, pos));
    }

    @Override
    public boolean skipRendering(BlockState state, BlockState adjacent, Direction direction) {
        return AIPlacedBlock.hiddenAgainst(state, adjacent) || super.skipRendering(state, adjacent, direction);
    }

    @Override
    public float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return AIPlacedBlock.shadeBrightness(state, super.getShadeBrightness(state, level, pos));
    }

    @Override
    public float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        return AIPlacedBlock.destroyProgress(player, level, pos);
    }
}
