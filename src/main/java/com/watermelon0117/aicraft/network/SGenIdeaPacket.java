package com.watermelon0117.aicraft.network;

import com.watermelon0117.aicraft.gpt.delegate.IdeaGenerator;
import com.watermelon0117.aicraft.gpt.opanai.GPTIdeaGenerator;
import com.watermelon0117.aicraft.gpt.ItemIdeas;
import com.watermelon0117.aicraft.common.ItemStackArray;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.net.ConnectException;
import java.util.function.Supplier;

public class SGenIdeaPacket {
    private final BlockPos pos;
    private final ItemStack[] recipe;
    private final String lang;

    public SGenIdeaPacket(BlockPos pos, ItemStackArray recipe, String lang) {
        this.pos = pos;
        this.recipe = recipe.items;
        this.lang = lang;
    }

    public SGenIdeaPacket(FriendlyByteBuf buf) {
        ItemStack[] recipe = new ItemStack[9];
        this.pos = buf.readBlockPos();
        for (int i = 0; i < 9; i++) {
            recipe[i] = buf.readItem();
        }
        this.recipe = recipe;
        this.lang = buf.readUtf();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        for (int i = 0; i < 9; i++) {
            buf.writeItemStack(recipe[i], true);
        }
        buf.writeUtf(lang);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        IdeaGenerator generator = new IdeaGenerator();
        ServerPlayer player = contextSupplier.get().getSender();
        if (player != null && !player.level.isClientSide) {
            generator.generate(new ItemStackArray(recipe), lang, player.getStringUUID()).thenAccept(results -> {
                PacketHandler.sendToPlayer(
                        new CGenIdeaPacket(pos, recipe, results, false, ""), player);
            }).exceptionally(e -> {
                e.printStackTrace();
                PacketHandler.sendToPlayer(new CGenIdeaPacket(pos, recipe, new ItemIdeas(), true,
                        e.getCause() instanceof ConnectException ? "Unable to connect to the server. Please check your internet connection or try again later." : e.getMessage()), player);
                return null;
            });
        }
    }
}
