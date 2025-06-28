package com.watermelon0117.aicraft.network;

import com.watermelon0117.aicraft.gpt.GPTItemGenerator;
import com.watermelon0117.aicraft.blockentities.AICraftingTableBlockEntity;
import com.watermelon0117.aicraft.gpt.GPTItemGenerator;
import com.watermelon0117.aicraft.gpt.GPTItemGenerator2;
import com.watermelon0117.aicraft.recipes.Recipe;
import com.watermelon0117.aicraft.recipes.RecipeManager;
import com.watermelon0117.aicraft.recipes.SpecialItemManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SSelectIdeaPacket {
    private final BlockPos pos;
    private final String id;
    private final String name;
    private final ItemStack[] recipe;
    private final boolean overrride;
    private static int incrementID = 0;

    public SSelectIdeaPacket(BlockPos pos, String id, String name, ItemStack[] recipe, boolean overrride) {
        this.pos = pos;
        this.id = id;
        this.name = name;
        this.recipe = recipe;
        this.overrride = overrride;
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
        this.overrride = buf.readBoolean();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeUtf(id);
        buf.writeUtf(name);
        for (int i = 0; i < 9; i++) {
            buf.writeItemStack(recipe[i], true);
        }
        buf.writeBoolean(overrride);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        GPTItemGenerator2 generator = new GPTItemGenerator2();
        ServerPlayer player = contextSupplier.get().getSender();
        if (player != null && !player.level.isClientSide) {
            BlockEntity blockEntity = player.level.getBlockEntity(pos);
            if (blockEntity instanceof AICraftingTableBlockEntity be) {
                if (!SpecialItemManager.getItem(id).isEmpty() && !overrride) {  //use exist item
                    ItemStack stack = SpecialItemManager.getItem(id);
                    RecipeManager.addRecipe(stack, recipe, RecipeManager.itemIsShapeless(stack));
                    be.getInventory().setStackInSlot(0, SpecialItemManager.getItem(id));
                    be.setProgress(580);
                    player.level.sendBlockUpdated(pos, player.level.getBlockState(pos), player.level.getBlockState(pos), Block.UPDATE_ALL);
                } else {
                    be.setProgress(1);
                    int tId = incrementID++;
                    be.taskID = tId;
                    player.level.sendBlockUpdated(pos, player.level.getBlockState(pos), player.level.getBlockState(pos), Block.UPDATE_ALL);
                    player.sendSystemMessage(Component.literal("Start..."));
                    generator.generate(id, name, new Recipe(recipe), be, b -> b.taskID == tId && b.getProgress() != 0).thenAccept(itemStack -> {
                                if (be.taskID == tId && be.getProgress() != 0) {
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
}
