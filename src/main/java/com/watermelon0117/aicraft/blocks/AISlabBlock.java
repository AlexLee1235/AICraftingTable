package com.watermelon0117.aicraft.blocks;

import com.watermelon0117.aicraft.blockentities.AIBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

public class AISlabBlock extends SlabBlock implements AIPlacedBlock {
    public AISlabBlock() {
        super(AIPlacedBlock.properties());
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
    public float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        return AIPlacedBlock.destroyProgress(player, level, pos);
    }
}
