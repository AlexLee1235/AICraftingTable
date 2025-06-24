package com.watermelon0117.aicraft.network;

import com.watermelon0117.aicraft.blockentities.AICraftingTableBlockEntity;
import com.watermelon0117.aicraft.gpt.GPTIdeaGenerator2;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SGenIdeaPacket {
    private final BlockPos pos;
    private final String[] recipe;
    GPTIdeaGenerator2 generator = new GPTIdeaGenerator2();

    public SGenIdeaPacket(BlockPos pos, String[] recipe) {
        this.pos = pos;
        this.recipe = recipe;
    }

    public SGenIdeaPacket(FriendlyByteBuf buf) {
        String[] recipe = new String[9];
        this.pos = buf.readBlockPos();
        for (int i = 0; i < 9; i++) {
            recipe[i] = buf.readUtf();
        }
        this.recipe = recipe;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        for (int i = 0; i < 9; i++) {
            buf.writeUtf(recipe[i]);
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
