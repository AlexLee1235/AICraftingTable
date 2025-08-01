package com.watermelon0117.aicraft.network;

import com.watermelon0117.aicraft.blockentities.AICraftingTableBlockEntity;
import com.watermelon0117.aicraft.menu.AICraftingTableMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SPlaceRecipePacket {
    private final BlockPos pos;
    private final ItemStack[] recipe;
    private final boolean shift;

    public SPlaceRecipePacket(BlockPos pos, ItemStack[] recipe, boolean shift) {
        this.pos = pos;
        this.recipe = recipe;
        this.shift = shift;
    }

    public SPlaceRecipePacket(FriendlyByteBuf buf) {
        ItemStack[] recipe = new ItemStack[9];
        this.pos = buf.readBlockPos();
        for (int i = 0; i < 9; i++) {
            recipe[i] = buf.readItem();
        }
        this.recipe = recipe;
        this.shift = buf.readBoolean();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        for (int i = 0; i < 9; i++) {
            buf.writeItemStack(recipe[i], true);
        }
        buf.writeBoolean(shift);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        ServerPlayer player = contextSupplier.get().getSender();
        if (player != null && !player.level.isClientSide) {
            BlockEntity blockEntity = player.level.getBlockEntity(pos);
            if (blockEntity instanceof AICraftingTableBlockEntity be) {
                placeRecipe(be, player);
                player.level.sendBlockUpdated(pos, player.level.getBlockState(pos), player.level.getBlockState(pos), Block.UPDATE_ALL);
            }
        }
    }
    private void placeRecipe(AICraftingTableBlockEntity be, ServerPlayer player) {
        AbstractContainerMenu menu1 = player.containerMenu;
        if (!(menu1 instanceof AICraftingTableMenu menu))
            return;
        if (player.isCreative() && shift) {
            if(!RecipePlacer.alreadyHas(player, recipe))
                for (int i = 0; i < 9; i++)
                    menu.slots.get(i+1).set(ItemStack.EMPTY);
            RecipePlacer.forcePlaceRecipePattern(player, recipe);
        } else if (menu.canCraftRecipe(recipe)) {
            if(RecipePlacer.alreadyHas(player, recipe)){
                RecipePlacer.placeRecipePattern(player, recipe);
            } else if (RecipePlacer.clearGridToInventory(player, menu)) {
                RecipePlacer.placeRecipePattern(player, recipe);
            }
        } else {
            if (RecipePlacer.clearGridToInventory(player, menu)) {
                PacketHandler.sendToPlayer(new CPlaceGhostRecipePacket(pos, recipe), player);
            }
        }
        menu.broadcastChanges();
    }
}
