package com.watermelon0117.aicraft.common;

import com.watermelon0117.aicraft.items.AIArmorItem;
import com.watermelon0117.aicraft.items.MainItem;
import com.watermelon0117.aicraft.network.CAddArmorTexturePacket;
import com.watermelon0117.aicraft.network.CAddTexturePacket;
import com.watermelon0117.aicraft.network.PacketHandler;
import net.minecraft.world.item.ItemStack;

import java.io.IOException;

/** Makes an AI armor piece usable in this world. Call it on the server thread, which is where the world's folder can be looked up. */
public final class AIArmorInstaller {
    private AIArmorInstaller() {
    }

    /**
     * Saves the piece's icon and worn picture and sends both to every player. The caller then remembers the stack, as for items.
     *
     * @param icon the sprite players see in the inventory, already pixelized
     * @throws IllegalArgumentException if the worn picture cannot be drawn on this stack's slot; nothing is saved then
     */
    public static void install(ItemStack stack, byte[] icon, ArmorTexture worn) throws IOException {
        if (!(stack.getItem() instanceof AIArmorItem armor)) {
            throw new IllegalArgumentException("Not an AI armor piece: " + stack);
        }
        String id = MainItem.getID(stack);
        if (id == null) {
            throw new IllegalArgumentException("The armor piece has no id");
        }
        ArmorUV table = worn.check();
        if (table.slot() != armor.uvSlot()) {
            throw new IllegalArgumentException("A " + table.slot().key() + " picture cannot go on a " + armor.uvSlot().key());
        }
        TextureManager.saveSprite(icon, id);
        ArmorTextureStore.save(FileUtil.getArmorTextureFolder(), id, worn);
        PacketHandler.sendToAllClients(new CAddTexturePacket(id, icon));
        PacketHandler.sendToAllClients(new CAddArmorTexturePacket(id, worn));
    }
}
