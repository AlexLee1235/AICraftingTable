package com.watermelon0117.aicraft.network;

import com.watermelon0117.aicraft.blockentities.AICraftingTableBlockEntity;
import com.watermelon0117.aicraft.gpt.GPTItemGenerator2;
import com.watermelon0117.aicraft.gpt.GPTItemGenerator3;
import com.watermelon0117.aicraft.common.ItemStackArray;
import com.watermelon0117.aicraft.common.RecipeManager;
import com.watermelon0117.aicraft.common.SpecialItemManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SSelectIdeaPacket {
    private final BlockPos pos;
    private final String id;
    private final String name;
    private final ItemStack[] recipe;
    private final boolean override;
    private static int incrementID = 0;

    public SSelectIdeaPacket(BlockPos pos, String id, String name, ItemStack[] recipe, boolean override) {
        this.pos = pos;
        this.id = id;
        this.name = name;
        this.recipe = recipe;
        this.override = override;
    }

    public SSelectIdeaPacket(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.id = buf.readUtf();
        this.name = buf.readUtf();
        ItemStack[] recipe = new ItemStack[9];
        for (int i = 0; i < 9; i++) {
            recipe[i] = buf.readItem();
        }
        this.recipe = recipe;
        this.override = buf.readBoolean();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeUtf(id);
        buf.writeUtf(name);
        for (int i = 0; i < 9; i++) {
            buf.writeItemStack(recipe[i], true);
        }
        buf.writeBoolean(override);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        GPTItemGenerator3 generator = new GPTItemGenerator3();
        ServerPlayer player = contextSupplier.get().getSender();
        if (player != null && !player.level.isClientSide) {
            BlockEntity blockEntity = player.level.getBlockEntity(pos);
            if (blockEntity instanceof AICraftingTableBlockEntity be) {
                if (SpecialItemManager.get().hasItem(id) && !override) {  //use exist item
                    ItemStack stack = SpecialItemManager.get().getItem(id);
                    RecipeManager.get().addRecipe(stack, recipe, RecipeManager.get().itemIsShapeless(stack));
                    be.getInventory().setStackInSlot(0, SpecialItemManager.get().getItem(id));
                    be.setProgress(580);
                    player.level.sendBlockUpdated(pos, player.level.getBlockState(pos), player.level.getBlockState(pos), Block.UPDATE_ALL);
                } else {
                    be.setProgress(1);
                    int tId = incrementID++;
                    be.taskID = tId;
                    player.level.sendBlockUpdated(pos, player.level.getBlockState(pos), player.level.getBlockState(pos), Block.UPDATE_ALL);
                    generator.generate(id, name, new ItemStackArray(recipe), be, b -> b.taskID == tId && b.getProgress() != 0).thenAccept(itemStack -> {
                                if (be.taskID == tId && be.getProgress() != 0) {
                                    be.getInventory().setStackInSlot(0, itemStack);
                                    be.setProgress(580);
                                    player.level.sendBlockUpdated(pos, player.level.getBlockState(pos), player.level.getBlockState(pos), Block.UPDATE_ALL);
                                } else {
                                    System.out.println("Canceled, not putting image");
                                }
                            })
                            .exceptionally(ex -> {
                                System.out.println("hi");
                                ex.printStackTrace();
                                be.setProgress(0);
                                player.level.sendBlockUpdated(pos, player.level.getBlockState(pos), player.level.getBlockState(pos), Block.UPDATE_ALL);
                                sendErrToAll(player.level, ex.getMessage());
                                return null;
                            });
                }
            }
        }
    }
    private static void sendErrToAll(Level level, String msg){
        for (ServerPlayer player : ((ServerLevel)level).getPlayers(p->true)) {
            player.sendSystemMessage(Component.literal("An error occurred when using the AI crafting table.").withStyle(ChatFormatting.RED));
            player.sendSystemMessage(Component.literal(msg).withStyle(ChatFormatting.RED));
        }
    }
}
