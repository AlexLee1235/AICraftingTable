package com.watermelon0117.aicraft.network;

import com.watermelon0117.aicraft.client.screen.AICraftingTableScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Tells a client that a table's generation failed: its progress cannot say so, as a failure in the tick it began never shows there. */
public class CGenerationFailedPacket {
    private final BlockPos pos;

    public CGenerationFailedPacket(BlockPos pos) {
        this.pos = pos;
    }

    public CGenerationFailedPacket(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        if (Minecraft.getInstance().screen instanceof AICraftingTableScreen screen) {
            screen.handleGenerationFailed(pos);
        }
    }
}
