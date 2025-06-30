package com.watermelon0117.aicraft.network;

import com.watermelon0117.aicraft.client.renderer.MyBlockEntityWithoutLevelRenderer;
import com.watermelon0117.aicraft.common.SpecialItemManager;
import com.watermelon0117.aicraft.items.MainItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.function.Supplier;

public class CSendAllTexturePacket {
    private final Map<String, byte[]> payload;
    public CSendAllTexturePacket(Map<String, byte[]> payload){
        this.payload=payload;
    }
    CSendAllTexturePacket(FriendlyByteBuf buf) {
        payload=buf.readMap(FriendlyByteBuf::readUtf, FriendlyByteBuf::readByteArray);
    }

    void encode(FriendlyByteBuf buf) {
        buf.writeMap(payload, FriendlyByteBuf::writeUtf, FriendlyByteBuf::writeByteArray);
    }

    void handle(Supplier<NetworkEvent.Context> ctx) {
        MainItem.renderer.loadFromPacket(payload);
    }
}
