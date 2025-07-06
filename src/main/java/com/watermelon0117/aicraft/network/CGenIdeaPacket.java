package com.watermelon0117.aicraft.network;

import com.watermelon0117.aicraft.client.screen.AICraftingTableScreen;
import com.watermelon0117.aicraft.gpt.ItemIdeas;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class CGenIdeaPacket {
    private final BlockPos pos;
    private final ItemStack[] recipe;
    private final ItemIdeas ideas;
    private final boolean err;
    private final String errMsg;

    public CGenIdeaPacket(BlockPos pos, ItemStack[] recipe, ItemIdeas ideas, boolean err, String errMsg) {
        this.pos = pos;
        this.recipe = recipe;
        this.ideas = ideas;
        this.err = err;
        this.errMsg = errMsg;
    }

    public CGenIdeaPacket(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        ItemStack[] recipe = new ItemStack[9];
        for (int i = 0; i < 9; i++) {
            recipe[i] = buf.readItem();
        }
        this.recipe = recipe;
        this.ideas = new ItemIdeas(buf);
        this.err = buf.readBoolean();
        this.errMsg = buf.readUtf();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        for (int i = 0; i < 9; i++) {
            buf.writeItemStack(recipe[i], true);
        }
        ideas.write(buf);
        buf.writeBoolean(err);
        buf.writeUtf(errMsg);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        if (Minecraft.getInstance().screen instanceof AICraftingTableScreen screen) {
            screen.handleIdeaPacket(recipe, ideas, err, errMsg);
        }
    }
}
