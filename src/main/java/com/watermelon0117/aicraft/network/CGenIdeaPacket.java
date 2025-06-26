package com.watermelon0117.aicraft.network;

import com.watermelon0117.aicraft.blockentities.AICraftingTableBlockEntity;
import com.watermelon0117.aicraft.client.screen.AICraftingTableScreen;
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
    private final String[] recipe;
    private final String[] ideas;
    private final boolean err;

    public CGenIdeaPacket(BlockPos pos, String[] recipe, String[] ideas, boolean err) {
        this.pos = pos;
        this.recipe = recipe;
        this.ideas = ideas;
        this.err = err;
    }

    public CGenIdeaPacket(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        String[] recipe = new String[9];
        for (int i = 0; i < 9; i++) {
            recipe[i] = buf.readUtf();
        }
        this.recipe = recipe;
        String[] ideas = new String[3];
        for (int i = 0; i < 3; i++) {
            ideas[i] = buf.readUtf();
        }
        this.ideas = ideas;
        this.err = buf.readBoolean();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        for (int i = 0; i < 9; i++) {
            buf.writeUtf(recipe[i]);
        }
        for (int i = 0; i < 3; i++) {
            buf.writeUtf(ideas[i]);
        }
        buf.writeBoolean(err);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        System.out.println("handle packet to player");
        if (Minecraft.getInstance().screen instanceof AICraftingTableScreen screen) {
            System.out.println("handle packet to player2");
            screen.handleIdeaPacket(recipe, ideas, err);
        }
    }
}
