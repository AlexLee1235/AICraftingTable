package com.watermelon0117.aicraft.network;

import com.watermelon0117.aicraft.gpt.GPTItemGenerator;
import com.watermelon0117.aicraft.blockentities.AICraftingTableBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SSelectIdeaPacket {
    private final BlockPos pos;
    private final String name;
    private final String[] recipe;
    private static int incrementID = 0;

    public SSelectIdeaPacket(BlockPos pos, String name, String[] recipe) {
        this.pos = pos;
        this.name = name;
        this.recipe = recipe;
    }

    public SSelectIdeaPacket(FriendlyByteBuf buf) {
        String[] recipe = new String[9];
        this.pos = buf.readBlockPos();
        this.name = buf.readUtf();
        for (int i = 0; i < 9; i++) {
            recipe[i] = buf.readUtf();
        }
        this.recipe = recipe;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeUtf(name);
        for (int i = 0; i < 9; i++) {
            buf.writeUtf(recipe[i]);
        }
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        GPTItemGenerator generator = new GPTItemGenerator();
        ServerPlayer player = contextSupplier.get().getSender();
        if (player != null && !player.level.isClientSide) {
            BlockEntity blockEntity = player.level.getBlockEntity(pos);
            if (blockEntity instanceof AICraftingTableBlockEntity be) {
                be.setProgress(1);
                int id = incrementID++;
                be.taskID = id;
                player.level.sendBlockUpdated(pos, player.level.getBlockState(pos), player.level.getBlockState(pos), Block.UPDATE_ALL);
                player.sendSystemMessage(Component.literal("Start..."));
                generator.generate(name, recipe, be, b -> b.taskID == id && b.getProgress() != 0).thenAccept(itemStack -> {
                            if (be.taskID == id && be.getProgress() != 0) {
                                player.sendSystemMessage(Component.literal("Done"));
                                be.getInventory().setStackInSlot(0, itemStack);
                                be.setProgress(580);
                                player.level.sendBlockUpdated(pos, player.level.getBlockState(pos), player.level.getBlockState(pos), Block.UPDATE_ALL);
                            } else {
                                System.out.println("Canceled, not putting image");
                            }
                        })
                        .exceptionally(ex -> {
                            ex.printStackTrace();
                            be.setProgress(0);
                            player.level.sendBlockUpdated(pos, player.level.getBlockState(pos), player.level.getBlockState(pos), Block.UPDATE_ALL);
                            //todo: show error in screen
                            return null;
                        });
            }
        }
    }
}
