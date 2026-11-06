package com.watermelon0117.aicraft.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A full cube, opaque or see-through. It is not a BaseEntityBlock, whose render shape is invisible. */
public class AIBlock extends Block implements AIPlacedBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public AIBlock() {
        super(AIPlacedBlock.properties());
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(LIGHT, AIBlockLight.NONE)
                .setValue(TRANSPARENCY, AIBlockTransparency.OPAQUE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIGHT, TRANSPARENCY);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return AIPlacedBlock.withStateOfItem(defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite()), context);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
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
