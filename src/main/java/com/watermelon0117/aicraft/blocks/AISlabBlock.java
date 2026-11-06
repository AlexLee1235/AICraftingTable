package com.watermelon0117.aicraft.blocks;

import com.watermelon0117.aicraft.blockentities.AIBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class AISlabBlock extends SlabBlock implements AIPlacedBlock {
    public AISlabBlock() {
        super(AIPlacedBlock.properties());
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

    /** Vanilla only compares items, and all AI slabs are one item: two different ones must not become one block. */
    @Override
    public boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        return super.canBeReplaced(state, context)
                && context.getLevel().getBlockEntity(context.getClickedPos()) instanceof AIBlockEntity blockEntity
                && blockEntity.getAicraftTag().getString("id").equals(idOf(context));
    }

    private static String idOf(BlockPlaceContext context) {
        CompoundTag data = context.getItemInHand().getTagElement(AIBlockEntity.DATA_KEY);
        return data == null ? "" : data.getString("id");
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
