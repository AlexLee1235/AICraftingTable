package com.watermelon0117.aicraft.network;

import com.watermelon0117.aicraft.blockentities.AICraftingTableBlockEntity;
import com.watermelon0117.aicraft.gpt.GPTIdeaGenerator2;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class CGenIdeaPacket {
    private final BlockPos pos;
    private final String[] ideas;

    public CGenIdeaPacket(BlockPos pos, String[] ideas) {
        this.pos = pos;
        this.ideas = ideas;
    }

    public CGenIdeaPacket(FriendlyByteBuf buf) {
        String[] ideas = new String[9];
        this.pos = buf.readBlockPos();
        for (int i = 0; i < 3; i++) {
            ideas[i] = buf.readUtf();
        }
        this.ideas = ideas;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        for (int i = 0; i < 3; i++) {
            buf.writeUtf(ideas[i]);
        }
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        ServerPlayer player = contextSupplier.get().getSender();
        if (player != null && !player.level.isClientSide) {
            BlockEntity blockEntity = player.level.getBlockEntity(pos);
            if (blockEntity instanceof AICraftingTableBlockEntity be) {

            }
        }
    }
}
