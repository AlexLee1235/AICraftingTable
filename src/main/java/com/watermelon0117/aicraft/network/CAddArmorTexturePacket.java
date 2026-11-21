package com.watermelon0117.aicraft.network;

import com.watermelon0117.aicraft.client.armor.ClientArmorPackets;
import com.watermelon0117.aicraft.common.ArmorTexture;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** A new or regenerated armor piece's worn picture, for every player. */
public class CAddArmorTexturePacket {
    private final String id;
    private final ArmorTexture texture;

    public CAddArmorTexturePacket(String id, ArmorTexture texture) {
        this.id = id;
        this.texture = texture;
    }

    CAddArmorTexturePacket(FriendlyByteBuf buf) {
        id = buf.readUtf();
        texture = readTexture(buf);
    }

    void encode(FriendlyByteBuf buf) {
        buf.writeUtf(id);
        writeTexture(buf, texture);
    }

    void handle(Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientArmorPackets.add(id, texture));
    }

    static ArmorTexture readTexture(FriendlyByteBuf buf) {
        return new ArmorTexture(buf.readByteArray(), buf.readUtf());
    }

    static void writeTexture(FriendlyByteBuf buf, ArmorTexture texture) {
        buf.writeByteArray(texture.png());
        buf.writeUtf(texture.uv());
    }
}
