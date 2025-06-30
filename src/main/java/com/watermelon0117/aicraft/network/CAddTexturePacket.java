package com.watermelon0117.aicraft.network;

import com.watermelon0117.aicraft.items.MainItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.Map;
import java.util.function.Supplier;

public class CAddTexturePacket {
    private final String id;
    private final byte[] text;
    public CAddTexturePacket(String id, byte[] text){
        this.id=id;
        this.text=text;
    }
    CAddTexturePacket(FriendlyByteBuf buf) {
        id=buf.readUtf();
        text=buf.readByteArray();
    }

    void encode(FriendlyByteBuf buf) {
        buf.writeUtf(id);
        buf.writeByteArray(text);
    }

    void handle(Supplier<NetworkEvent.Context> ctx) {
        MainItem.renderer.addFromPacket(id,text);
    }
}
