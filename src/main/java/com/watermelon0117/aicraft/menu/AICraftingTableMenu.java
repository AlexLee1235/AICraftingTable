package com.watermelon0117.aicraft.menu;

import com.watermelon0117.aicraft.blockentities.AICraftingTableBlockEntity;
import com.watermelon0117.aicraft.init.BlockInit;
import com.watermelon0117.aicraft.init.MenuInit;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

public class AICraftingTableMenu extends AbstractContainerMenu {
    private final AICraftingTableBlockEntity blockEntity;
    private final ContainerLevelAccess levelAccess;
    //Client Constructor
    public AICraftingTableMenu(int id, Inventory inventory, FriendlyByteBuf buf){
        this(id, inventory, inventory.player.level.getBlockEntity(buf.readBlockPos()));
    }
    //Server Constructor
    public AICraftingTableMenu(int id, Inventory inventory, BlockEntity blockEntity){
        super(MenuInit.MAIN_MENU.get(), id);
        if(blockEntity instanceof AICraftingTableBlockEntity be){
            this.blockEntity=be;
        }else {
            throw new IllegalStateException("AICraftingTableMenu be");
        }
        this.levelAccess=ContainerLevelAccess.create(be.getLevel(), be.getBlockPos());
    }

    @Override
    public ItemStack quickMoveStack(Player p_38941_, int p_38942_) {
        return null;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(levelAccess, player, BlockInit.AI_CRAFTING_TABLE.get());
    }
}
