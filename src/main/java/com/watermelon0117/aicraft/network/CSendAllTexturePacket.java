package com.watermelon0117.aicraft.network;

import com.watermelon0117.aicraft.common.SpecialItemManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.Map;
import java.util.function.Supplier;

public class CSendAllTexturePacket {
    private final Map<String, String> payload;
    public CSendAllTexturePacket(Map<String, String> payload){
        this.payload=payload;
    }
    CSendAllTexturePacket(FriendlyByteBuf buf) {
        payload=buf.readMap(FriendlyByteBuf::readUtf, FriendlyByteBuf::readUtf);
    }

    void encode(FriendlyByteBuf buf) {
        buf.writeMap(payload, FriendlyByteBuf::writeUtf, FriendlyByteBuf::writeUtf);
    }

    void handle(Supplier<NetworkEvent.Context> ctx) {

    }
}
