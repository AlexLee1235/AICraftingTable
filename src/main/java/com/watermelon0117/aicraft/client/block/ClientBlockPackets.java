package com.watermelon0117.aicraft.client.block;

import com.watermelon0117.aicraft.common.BlockFaces;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Map;

/** Keeps the client-only atlas out of the packet classes, which a dedicated server also loads. */
@OnlyIn(Dist.CLIENT)
public final class ClientBlockPackets {
    private ClientBlockPackets() {
    }

    public static void add(String id, BlockFaces faces) {
        BlockTileAtlas.put(id, faces);
    }

    public static void addAll(Map<String, BlockFaces> faces) {
        faces.forEach(BlockTileAtlas::put);
    }
}
