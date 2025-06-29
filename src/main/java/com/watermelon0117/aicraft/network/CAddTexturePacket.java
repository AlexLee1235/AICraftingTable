package com.watermelon0117.aicraft.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.Map;
import java.util.function.Supplier;

public class CAddTexturePacket {
    private final String id,text;
    public CAddTexturePacket(String id, String text){
        this.id=id;
        this.text=text;
    }
    CAddTexturePacket(FriendlyByteBuf buf) {
        id=buf.readUtf();
        text=buf.readUtf();
    }

    void encode(FriendlyByteBuf buf) {
        buf.writeUtf(id);
        buf.writeUtf(text);
    }

    void handle(Supplier<NetworkEvent.Context> ctx) {

    }
}
