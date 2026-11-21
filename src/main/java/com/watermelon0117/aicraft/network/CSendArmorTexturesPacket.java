package com.watermelon0117.aicraft.network;

import com.watermelon0117.aicraft.client.armor.ClientArmorPackets;
import com.watermelon0117.aicraft.common.ArmorTexture;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.Map;
import java.util.function.Supplier;

/** One batch of a world's worn armor pictures at login. See ArmorTextureStore.batches for why there can be several. */
public class CSendArmorTexturesPacket {
    private final Map<String, ArmorTexture> textures;

    public CSendArmorTexturesPacket(Map<String, ArmorTexture> textures) {
        this.textures = textures;
    }

    CSendArmorTexturesPacket(FriendlyByteBuf buf) {
        textures = buf.readMap(FriendlyByteBuf::readUtf, CAddArmorTexturePacket::readTexture);
    }

    void encode(FriendlyByteBuf buf) {
        buf.writeMap(textures, FriendlyByteBuf::writeUtf, CAddArmorTexturePacket::writeTexture);
    }

    void handle(Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientArmorPackets.addAll(textures));
    }
}
