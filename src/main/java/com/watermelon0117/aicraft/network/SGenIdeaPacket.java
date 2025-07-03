package com.watermelon0117.aicraft.network;

import com.watermelon0117.aicraft.gpt.GPTIdeaGenerator4;
import com.watermelon0117.aicraft.gpt.GPTIdeaGenerator5;
import com.watermelon0117.aicraft.gpt.GPTIdeaGenerator6;
import com.watermelon0117.aicraft.gpt.ItemIdeas;
import com.watermelon0117.aicraft.common.ItemStackArray;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SGenIdeaPacket {
    private final BlockPos pos;
    private final ItemStack[] recipe;
    private final String lang;
    private final int session;

    public SGenIdeaPacket(BlockPos pos, ItemStackArray recipe, String lang, int session) {
        this.pos = pos;
        this.recipe = recipe.items;
        this.lang=lang;
        this.session=session;
    }

    public SGenIdeaPacket(FriendlyByteBuf buf) {
        ItemStack[] recipe = new ItemStack[9];
        this.pos = buf.readBlockPos();
        for (int i = 0; i < 9; i++) {
            recipe[i] = buf.readItem();
        }
        this.recipe = recipe;
        this.lang=buf.readUtf();
        this.session=buf.readInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        for (int i = 0; i < 9; i++) {
            buf.writeItemStack(recipe[i], true);
        }
        buf.writeUtf(lang);
        buf.writeInt(session);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        GPTIdeaGenerator6 generator = new GPTIdeaGenerator6();
        ServerPlayer player = contextSupplier.get().getSender();
        if (player != null && !player.level.isClientSide) {
            generator.generate(new ItemStackArray(recipe), lang).thenAccept(results -> {
                PacketHandler.sendToPlayer(
                        new CGenIdeaPacket(pos, new ItemStackArray(recipe).getDisplayNames(), results, false, ""), player);
            }).exceptionally(e -> {
                e.printStackTrace();
                PacketHandler.sendToPlayer(
                        new CGenIdeaPacket(pos, new ItemStackArray(recipe).getDisplayNames(), new ItemIdeas(), true, e.getMessage()), player);
                return null;
            });
        }
    }
}
