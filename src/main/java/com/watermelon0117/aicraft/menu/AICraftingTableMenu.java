package com.watermelon0117.aicraft.menu;

import com.watermelon0117.aicraft.blockentities.AICraftingTableBlockEntity;
import com.watermelon0117.aicraft.init.BlockInit;
import com.watermelon0117.aicraft.init.ItemInit;
import com.watermelon0117.aicraft.init.MenuInit;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class AICraftingTableMenu extends AbstractContainerMenu {
    public static final int RESULT_SLOT = 0;
    private static final int CRAFT_SLOT_START = 1;
    private static final int CRAFT_SLOT_END = 10;
    private static final int INV_SLOT_START = 10;
    private static final int INV_SLOT_END = 37;
    private static final int USE_ROW_SLOT_START = 37;
    private static final int USE_ROW_SLOT_END = 46;
    private final CraftingContainer craftSlots = new CraftingContainer(this, 3, 3);
    private final ResultContainer resultSlots = new ResultContainer();
    private final ContainerLevelAccess access;
    private final Player player;
    private final AICraftingTableBlockEntity blockEntity;
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

        this.access = ContainerLevelAccess.create(be.getLevel(), be.getBlockPos());
        this.player = inventory.player;
        this.addSlot(new ResultSlot(inventory.player, this.craftSlots, this.resultSlots, 0, 146, 35));

        for(int i = 0; i < 3; ++i) {
            for(int j = 0; j < 3; ++j) {
                this.addSlot(new Slot(this.craftSlots, j + i * 3, 9 + j * 18, 17 + i * 18));
            }
        }

        for(int k = 0; k < 3; ++k) {
            for(int i1 = 0; i1 < 9; ++i1) {
                this.addSlot(new Slot(inventory, i1 + k * 9 + 9, 8 + i1 * 18, 84 + k * 18));
            }
        }

        for(int l = 0; l < 9; ++l) {
            this.addSlot(new Slot(inventory, l, 8 + l * 18, 142));
        }
    }

    public void removed(Player p_39389_) {
        super.removed(p_39389_);
        this.access.execute((p_39371_, p_39372_) -> {
            this.clearContainer(p_39389_, this.craftSlots);
        });
    }

    public boolean stillValid(Player p_39368_) {
        return true;
    }

    public ItemStack quickMoveStack(Player p_39391_, int p_39392_) {
        return ItemStack.EMPTY;
    }

    public boolean canTakeItemForPickAll(ItemStack p_39381_, Slot p_39382_) {
        return p_39382_.container != this.resultSlots && super.canTakeItemForPickAll(p_39381_, p_39382_);
    }

    public int getResultSlotIndex() {
        return 0;
    }
}
