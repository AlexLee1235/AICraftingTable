package com.watermelon0117.aicraft.blocks;

import com.watermelon0117.aicraft.blockentities.AICraftingTableBlockEntity;
import com.watermelon0117.aicraft.client.ClientHooks;
import com.watermelon0117.aicraft.init.BlockEntityInit;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import org.jetbrains.annotations.Nullable;

public class AICraftingTableBlock extends Block implements EntityBlock {
    
    public AICraftingTableBlock(Properties p_41383_) {
        super(p_41383_);
    }

    @Override
    public InteractionResult use(BlockState blockState, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult blockHitResult) {
        BlockEntity be=level.getBlockEntity(pos);
        if(!(be instanceof AICraftingTableBlockEntity blockEntity))
            return InteractionResult.PASS;
        if(level.isClientSide)
            return InteractionResult.SUCCESS;

        if(player instanceof ServerPlayer sPlayer){
            sPlayer.openMenu(blockEntity);
        }
        return InteractionResult.CONSUME;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos p_153215_, BlockState p_153216_) {
        return BlockEntityInit.AI_CRAFTING_TABLE_BE.get().create(p_153215_, p_153216_);
    }
}