package com.watermelon0117.aicraft.network;

import com.watermelon0117.aicraft.recipes.RecipeManager;
import com.watermelon0117.aicraft.gpt.GPTImageGenerator;
import com.watermelon0117.aicraft.ImageGridProcessor;
import com.watermelon0117.aicraft.blockentities.AICraftingTableBlockEntity;
import com.watermelon0117.aicraft.init.ItemInit;
import com.watermelon0117.aicraft.items.MainItem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Supplier;

public class SGUISelectItemButtonPressedPacket {
    private final BlockPos pos;
    private final String name;
    private final String[] recipe;
    GPTImageGenerator imgClient = new GPTImageGenerator();

    public SGUISelectItemButtonPressedPacket(BlockPos pos, String name, String[] recipe){
        this.pos=pos;
        this.name=name;
        this.recipe=recipe;
    }
    public SGUISelectItemButtonPressedPacket(FriendlyByteBuf buf){
        String[] recipe=new String[9];
        this.pos=buf.readBlockPos();
        this.name=buf.readUtf();
        for (int i = 0; i < 9; i++) {
            recipe[i]=buf.readUtf();
        }
        this.recipe=recipe;
    }
    public void encode(FriendlyByteBuf buf){
        buf.writeBlockPos(pos);
        buf.writeUtf(name);
        for (int i = 0; i < 9; i++) {
            buf.writeUtf(recipe[i]);
        }
    }
    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        ServerPlayer player = contextSupplier.get().getSender();
        if (player != null && !player.level.isClientSide) {
            BlockEntity blockEntity=player.level.getBlockEntity(pos);
            if(blockEntity instanceof AICraftingTableBlockEntity be) {
                be.setProgress(1);
                be.target = name;
                player.level.sendBlockUpdated(pos, player.level.getBlockState(pos), player.level.getBlockState(pos), Block.UPDATE_ALL);
                player.sendSystemMessage(Component.literal("Start..."));
                imgClient.generateItem(name, recipe).thenAccept(bytes -> {
                            try {
                                if (be.target.contentEquals(name) && be.getProgress() != 0) {
                                    Files.write(Path.of("C:\\achieve\\AICraftingTable\\process\\source.png"), bytes);
                                    Files.write(Path.of("C:\\achieve\\AICraftingTable\\image\\" + name + ".png"), bytes);
                                    BufferedImage txt = ImageGridProcessor.process("C:\\achieve\\AICraftingTable\\process\\source.png");
                                    ImageGridProcessor.saveImage(txt, "C:\\achieve\\AICraftingTable\\temp\\" + name + ".png");
                                    player.sendSystemMessage(Component.literal("Done"));
                                    MainItem.renderer.loadNewFile(name);
                                    ItemStack itemStack = new ItemStack(ItemInit.MAIN_ITEM.get());
                                    itemStack.getOrCreateTag().putString("texture", name);
                                    RecipeManager.addRecipe(name, recipe);
                                    be.getInventory().setStackInSlot(0, itemStack);
                                    be.setProgress(580);
                                    player.level.sendBlockUpdated(pos, player.level.getBlockState(pos), player.level.getBlockState(pos), Block.UPDATE_ALL);
                                } else {
                                    System.out.println("Canceled, not putting image");
                                }
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        })
                        .exceptionally(ex -> {
                            ex.printStackTrace();
                            return null;
                        });
            }
        }
    }
}
