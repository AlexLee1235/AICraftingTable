package com.watermelon0117.aicraft.common;

import com.watermelon0117.aicraft.gpt.GeneratedBlock;
import com.watermelon0117.aicraft.gpt.GeneratedItem;
import com.watermelon0117.aicraft.gpt.GeneratedResult;
import com.watermelon0117.aicraft.network.CAddTexturePacket;
import com.watermelon0117.aicraft.network.PacketHandler;
import net.minecraft.world.item.ItemStack;

import java.io.IOException;

/** Makes a generated item or block usable in this world. Call it on the server thread, which is where the world's folder can be looked up. */
public final class GeneratedInstaller {
    private GeneratedInstaller() {
    }

    /**
     * Saves its pictures and sends them to every player.
     *
     * @return one of it, with its id and name
     */
    public static ItemStack install(String id, String name, GeneratedResult generated) throws IOException {
        if (generated instanceof GeneratedBlock block) {
            return AIBlockInstaller.install(id, name, block);
        }
        GeneratedItem item = (GeneratedItem) generated;
        PacketHandler.sendToAllClients(new CAddTexturePacket(id, TextureManager.applyTexture(item.rawTexture(), id)));
        return item.itemStack();
    }
}
