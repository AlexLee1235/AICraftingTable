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
        String id = check(stack, worn);
        TextureManager.saveSprite(icon, id);
        PacketHandler.sendToAllClients(new CAddTexturePacket(id, icon));
        saveWorn(id, worn);
    }

    /** The worn picture alone, for a piece whose icon was made as any item's (GeneratedInstaller). */
    public static void installWorn(ItemStack stack, ArmorTexture worn) throws IOException {
        saveWorn(check(stack, worn), worn);
    }

    /**
     * @return the piece's id
     * @throws IllegalArgumentException if the stack is no AI armor piece with an id, or the worn picture is unreadable or for another slot
     */
    public static String check(ItemStack stack, ArmorTexture worn) {
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
        return id;
    }

    private static void saveWorn(String id, ArmorTexture worn) throws IOException {
        ArmorTextureStore.save(FileUtil.getArmorTextureFolder(), id, worn);
        PacketHandler.sendToAllClients(new CAddArmorTexturePacket(id, worn));
    }
}
