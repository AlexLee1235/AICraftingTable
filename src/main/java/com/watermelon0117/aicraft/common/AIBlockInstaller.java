package com.watermelon0117.aicraft.common;

import com.watermelon0117.aicraft.gpt.GeneratedBlock;
import com.watermelon0117.aicraft.init.BlockInit;
import com.watermelon0117.aicraft.network.CAddBlockTexturePacket;
import com.watermelon0117.aicraft.network.PacketHandler;
import net.minecraft.world.item.ItemStack;

import java.io.IOException;

/** Makes a generated block usable in this world. Call it on the server thread, which is where the world's folder can be looked up. */
public final class AIBlockInstaller {
    private AIBlockInstaller() {
    }

    /**
     * Saves the block's faces, sends them to every player and remembers the block.
     *
     * @return one of the block, with its id and name
     */
    public static ItemStack install(String id, String name, GeneratedBlock block) throws IOException {
        BlockTextureStore.save(FileUtil.getBlockTextureFolder(), id, block.faces());
        PacketHandler.sendToAllClients(new CAddBlockTexturePacket(id, block.faces()));
        return BlockInit.itemFor(block.shape()).makeStack(id, name, block.tool(), block.hardness(), block.light(), block.sound(), block.flammable(),
                block.transparency());
    }
}
