package com.watermelon0117.aicraft.network;

import com.watermelon0117.aicraft.SpecialItemManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class CLoadItemsPacket {
    int itemsCount, TexturesCount;
    CompoundTag[] items;
    String recipeFile;
    String[] Textures;

    public CLoadItemsPacket() {
        var itemList=SpecialItemManager.getAllItems();
        this.itemsCount=itemList.size();
        this.items=itemList.toArray(new ItemStack[0]);

    }

    public CLoadItemsPacket(FriendlyByteBuf buf) {
        buf.readAnySizeNbt()
    }

    public void encode(FriendlyByteBuf buf) {

    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {

    }
}
