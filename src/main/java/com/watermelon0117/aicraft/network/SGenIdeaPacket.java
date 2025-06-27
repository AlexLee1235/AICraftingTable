package com.watermelon0117.aicraft.network;

import com.watermelon0117.aicraft.ImageGridProcessor;
import com.watermelon0117.aicraft.blockentities.AICraftingTableBlockEntity;
import com.watermelon0117.aicraft.gpt.GPTIdeaGenerator2;
import com.watermelon0117.aicraft.gpt.GPTIdeaGenerator4;
import com.watermelon0117.aicraft.recipes.Recipe;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SGenIdeaPacket {
    private final BlockPos pos;
    private final ItemStack[] recipe;

    public SGenIdeaPacket(BlockPos pos, Recipe recipe) {
        this.pos = pos;
        this.recipe = recipe.items;
    }

    public SGenIdeaPacket(FriendlyByteBuf buf) {
        ItemStack[] recipe = new ItemStack[9];
        this.pos = buf.readBlockPos();
        for (int i = 0; i < 9; i++) {
            recipe[i] = buf.readItem();
        }
        this.recipe = recipe;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        for (int i = 0; i < 9; i++) {
            buf.writeItemStack(recipe[i], true);
        }
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        GPTIdeaGenerator4 generator = new GPTIdeaGenerator4();
        ServerPlayer player = contextSupplier.get().getSender();
        if (player != null && !player.level.isClientSide) {
            generator.generate(new Recipe(recipe)).thenAccept(results -> {
                PacketHandler.sendToPlayer(
                        new CGenIdeaPacket(pos, new Recipe(recipe).getDisplayNames(), results, false, ""), player);
            }).exceptionally(e -> {
                e.printStackTrace();
                PacketHandler.sendToPlayer(
                        new CGenIdeaPacket(pos, new Recipe(recipe).getDisplayNames(), new String[]{"", "", ""}, true, e.getMessage()), player);
                return null;
            });
        }
    }
}
