package com.watermelon0117.aicraft.network;

import com.watermelon0117.aicraft.common.RecipeManager;
import com.watermelon0117.aicraft.common.SpecialItemManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class CSyncRecipesPacket {
    private final List<RecipeManager.Recipe> payload;

    // Constructor for sending
    public CSyncRecipesPacket(List<RecipeManager.Recipe> payload){
        this.payload = payload;
    }

    // Constructor for receiving (decode)
    public CSyncRecipesPacket(FriendlyByteBuf buf) {
        int size = buf.readInt();
        this.payload = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            RecipeManager.Recipe recipe = fromNetwork(buf);
            this.payload.add(recipe);
        }
    }

    // Encode for sending
    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(payload.size());
        for (RecipeManager.Recipe recipe : payload) {
            toNetwork(buf, recipe);
        }
    }

    // Handle on client side
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> RecipeManager.ClientSide.refill(payload));
        ctx.get().setPacketHandled(true);
    }
    public static RecipeManager.Recipe fromNetwork(FriendlyByteBuf buf) {
        boolean shapeless = buf.readBoolean();
        ItemStack result = buf.readItem();
        ItemStack[] grid = new ItemStack[9];
        for (int i = 0; i < 9; i++) {
            grid[i] = buf.readItem();
        }
        return new RecipeManager.Recipe(result, grid, shapeless);
    }

    public static void toNetwork(FriendlyByteBuf buf, RecipeManager.Recipe recipe) {
        buf.writeBoolean(recipe.shapeless);
        buf.writeItem(recipe.result);
        for (int i = 0; i < 9; i++) {
            buf.writeItem(recipe.grid[i]);
        }
    }
}
