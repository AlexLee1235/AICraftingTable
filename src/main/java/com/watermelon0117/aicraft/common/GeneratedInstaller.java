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
     * Saves its pictures and sends them to every player. An id made into something other than armor loses the worn picture it
     * had, so players are not sent it at every login.
     *
     * @return one of it, with its id and name
     */
    public static ItemStack install(String id, String name, GeneratedResult generated) throws IOException {
        if (generated instanceof GeneratedBlock block) {
            ItemStack stack = AIBlockInstaller.install(id, name, block);
            ArmorTextureStore.delete(FileUtil.getArmorTextureFolder(), id);
            return stack;
        }
        GeneratedItem item = (GeneratedItem) generated;
        if (item.worn() != null) {
            // Checked before anything is saved, so a worn picture that cannot go on the piece leaves nothing behind.
            AIArmorInstaller.check(item.itemStack(), item.worn());
        }
        PacketHandler.sendToAllClients(new CAddTexturePacket(id, TextureManager.applyTexture(item.rawTexture(), id)));
        if (item.worn() != null) {
            AIArmorInstaller.installWorn(item.itemStack(), item.worn());
        } else {
            ArmorTextureStore.delete(FileUtil.getArmorTextureFolder(), id);
        }
        return item.itemStack();
    }
}
