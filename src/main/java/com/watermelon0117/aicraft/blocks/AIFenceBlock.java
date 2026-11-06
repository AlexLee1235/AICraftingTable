package com.watermelon0117.aicraft.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Fences connect by tags, which cannot differ between block entities, so every AI fence counts as a wooden one. */
public class AIFenceBlock extends FenceBlock implements AIPlacedBlock {
    public AIFenceBlock() {
        super(AIPlacedBlock.properties().forceSolidOn());
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
