package com.watermelon0117.aicraft.network;

import com.watermelon0117.aicraft.client.block.ClientBlockPackets;
import com.watermelon0117.aicraft.common.BlockFaces;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.Map;
import java.util.function.Supplier;

/** One batch of a world's faces at login. See BlockTextureStore.batches for why there can be several. */
public class CSendBlockTexturesPacket {
    private final Map<String, BlockFaces> faces;

    public CSendBlockTexturesPacket(Map<String, BlockFaces> faces) {
        this.faces = faces;
    }

    CSendBlockTexturesPacket(FriendlyByteBuf buf) {
        faces = buf.readMap(FriendlyByteBuf::readUtf, CAddBlockTexturePacket::readFaces);
    }

    void encode(FriendlyByteBuf buf) {
        buf.writeMap(faces, FriendlyByteBuf::writeUtf, CAddBlockTexturePacket::writeFaces);
    }

    void handle(Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientBlockPackets.addAll(faces));
    }
}
