package com.watermelon0117.aicraft.network;

import com.watermelon0117.aicraft.client.block.ClientBlockPackets;
import com.watermelon0117.aicraft.common.BlockFaces;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class CAddBlockTexturePacket {
    private final String id;
    private final BlockFaces faces;

    public CAddBlockTexturePacket(String id, BlockFaces faces) {
        this.id = id;
        this.faces = faces;
    }

    CAddBlockTexturePacket(FriendlyByteBuf buf) {
        id = buf.readUtf();
        faces = readFaces(buf);
    }

    void encode(FriendlyByteBuf buf) {
        buf.writeUtf(id);
        writeFaces(buf, faces);
    }

    void handle(Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientBlockPackets.add(id, faces));
    }

    static BlockFaces readFaces(FriendlyByteBuf buf) {
        return new BlockFaces(buf.readByteArray(), buf.readByteArray(), buf.readByteArray());
    }

    static void writeFaces(FriendlyByteBuf buf, BlockFaces faces) {
        buf.writeByteArray(faces.top());
        buf.writeByteArray(faces.front());
        buf.writeByteArray(faces.side());
    }
}
