package com.watermelon0117.aicraft.network;

import com.watermelon0117.aicraft.GPTImageClient;
import com.watermelon0117.aicraft.GPTItemGenerator;
import com.watermelon0117.aicraft.ImageGridProcessor;
import com.watermelon0117.aicraft.blockentities.AICraftingTableBlockEntity;
import com.watermelon0117.aicraft.init.ItemInit;
import com.watermelon0117.aicraft.items.MainItem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Main;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class SGUISelectItemButtonPressedPacket {
    private final BlockPos pos;
    private final String name;
    GPTImageClient imgClient = new GPTImageClient("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");

    public SGUISelectItemButtonPressedPacket(BlockPos pos, String name){
        this.pos=pos;
        this.name=name;
    }
    public SGUISelectItemButtonPressedPacket(FriendlyByteBuf buf){
        this(buf.readBlockPos(), buf.readUtf());
    }
    public void encode(FriendlyByteBuf buf){
        buf.writeBlockPos(pos);
        buf.writeUtf(name);
    }
    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        ServerPlayer player = contextSupplier.get().getSender();
        if (player != null && !player.level.isClientSide) {
            BlockEntity blockEntity=player.level.getBlockEntity(pos);
            if(blockEntity instanceof AICraftingTableBlockEntity be){
                be.setProgress(1);
                be.target=name;
                player.level.sendBlockUpdated(pos, player.level.getBlockState(pos), player.level.getBlockState(pos), Block.UPDATE_ALL);
                player.sendSystemMessage(Component.literal("Start..."));
                imgClient.generateItem(name)
                        .thenAccept(bytes -> {
                            try {
                                if(be.target.contentEquals(name) && be.getProgress() != 0) {
                                    Files.write(Path.of("C:\\achieve\\AICraftingTable\\process\\source.png"), bytes);
                                    BufferedImage txt = ImageGridProcessor.process("C:\\achieve\\AICraftingTable\\process\\source.png");
                                    ImageGridProcessor.saveImage(txt, "C:\\achieve\\AICraftingTable\\temp\\" + name + ".png");
                                    player.sendSystemMessage(Component.literal("Done"));
                                    MainItem.renderer.loadNewFile(name);
                                    ItemStack itemStack = new ItemStack(ItemInit.MAIN_ITEM.get());
                                    itemStack.getOrCreateTag().putString("texture", name);
                                    //itemStack.setHoverName(Component.literal(name).withStyle(style -> style.withItalic(false)));
                                    be.getInventory().setStackInSlot(0, itemStack);
                                    be.setProgress(580);
                                    player.level.sendBlockUpdated(pos, player.level.getBlockState(pos), player.level.getBlockState(pos), Block.UPDATE_ALL);
                                }else{
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
