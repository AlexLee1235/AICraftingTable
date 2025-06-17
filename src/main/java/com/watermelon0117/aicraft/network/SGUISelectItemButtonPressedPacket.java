package com.watermelon0117.aicraft.network;

import com.watermelon0117.aicraft.blockentities.AICraftingTableBlockEntity;
import com.watermelon0117.aicraft.init.ItemInit;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class SGUISelectItemButtonPressedPacket {
    private final BlockPos pos;
    private final String name;
    public SGUISelectItemButtonPressedPacket(BlockPos pos, String name){
        this.pos=pos;
        this.name=name;
    }
    public SGUISelectItemButtonPressedPacket(FriendlyByteBuf buf){
        this(buf.readBlockPos(), buf.readUtf());
    }
    public void encode(FriendlyByteBuf buf){
        buf.writeBlockPos(pos);
        buf.writeUtf(name);
    }
    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        ServerPlayer player = contextSupplier.get().getSender();
        if (player != null && !player.level.isClientSide) {
            BlockEntity blockEntity=player.level.getBlockEntity(pos);
            if(blockEntity instanceof AICraftingTableBlockEntity be){
                be.setProgress(1);
                player.level.sendBlockUpdated(pos, player.level.getBlockState(pos), player.level.getBlockState(pos), Block.UPDATE_ALL);
                System.out.println("progress");
            }
        }
    }
}
