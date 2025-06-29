package com.watermelon0117.aicraft.network;

import com.watermelon0117.aicraft.common.SpecialItemManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.Map;
import java.util.function.Supplier;

public class CSyncSpecialItemsPacket {
    private final Map<String, CompoundTag> payload;
    public CSyncSpecialItemsPacket(Map<String, CompoundTag> payload){
        this.payload=payload;
    }
    CSyncSpecialItemsPacket(FriendlyByteBuf buf) {
        payload=buf.readMap(FriendlyByteBuf::readUtf, FriendlyByteBuf::readNbt);
    }

    void encode(FriendlyByteBuf buf) {
        buf.writeMap(payload, FriendlyByteBuf::writeUtf, FriendlyByteBuf::writeNbt);
    }

    void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> SpecialItemManager.ClientSide.refill(payload));
        ctx.get().setPacketHandled(true);
    }
}
