package com.watermelon0117.aicraft.menu;

import com.watermelon0117.aicraft.CraftingSlotItemHandler;
import com.watermelon0117.aicraft.ResultSlotItemHandler;
import com.watermelon0117.aicraft.blockentities.AICraftingTableBlockEntity;
import com.watermelon0117.aicraft.init.MenuInit;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.SlotItemHandler;

import java.util.Optional;

public class AICraftingTableMenu extends AbstractContainerMenu {
    public static final int RESULT_SLOT = 0;
    private static final int CRAFT_SLOT_START = 1;
    private static final int CRAFT_SLOT_END = 10;
    private static final int INV_SLOT_START = 10;
    private static final int INV_SLOT_END = 37;
    private static final int USE_ROW_SLOT_START = 37;
    private static final int USE_ROW_SLOT_END = 46;
    private final ContainerLevelAccess access;
    private final Player player;
    private final AICraftingTableBlockEntity blockEntity;
    //Client Constructor
    public AICraftingTableMenu(int id, Inventory inventory, FriendlyByteBuf buf){
        this(id, inventory, inventory.player.level.getBlockEntity(buf.readBlockPos()));
    }
    //Server Constructor
    public AICraftingTableMenu(int id, Inventory inventory, BlockEntity blockEntity) {
        super(MenuInit.MAIN_MENU.get(), id);
        if (blockEntity instanceof AICraftingTableBlockEntity be) {
            this.blockEntity = be;
        } else {
            throw new IllegalStateException("AICraftingTableMenu be");
        }

        this.access = ContainerLevelAccess.create(be.getLevel(), be.getBlockPos());
        this.player = inventory.player;

        be.getOptional().ifPresent(itemStackHandler -> {
            this.addSlot(new ResultSlotItemHandler(this, player, itemStackHandler, 0, 132, 35));
            for (int i = 0; i < 3; ++i) {
                for (int j = 0; j < 3; ++j) {
                    this.addSlot(new CraftingSlotItemHandler(this, itemStackHandler, j + i * 3 + 1, 9 + j * 18, 17 + i * 18));
                }
            }
        });


        for (int k = 0; k < 3; ++k) {
            for (int i1 = 0; i1 < 9; ++i1) {
                this.addSlot(new Slot(inventory, i1 + k * 9 + 9, 8 + i1 * 18, 84 + k * 18));
            }
        }

        for (int l = 0; l < 9; ++l) {
            this.addSlot(new Slot(inventory, l, 8 + l * 18, 142));
        }
    }

    public void removed(Player p_39389_) {
        super.removed(p_39389_);
    }

    public boolean stillValid(Player p_39368_) {
        return true;
    }

    public ItemStack quickMoveStack(Player p_39391_, int p_39392_) {
        return ItemStack.EMPTY;
    }
    private CraftingContainer getDummyContainer(){
        CraftingContainer craftingContainer = new CraftingContainer(this, 3,3);
        for (int i = 0; i < 9; i++) {
            craftingContainer.setItem(i, this.blockEntity.getInventory().getStackInSlot(i + 1));
        }
        return craftingContainer;
    }
    protected void slotChangedCraftingGrid(AbstractContainerMenu menu, Level level, Player player) {
        if (!level.isClientSide) {
            CraftingContainer container=getDummyContainer();
            ServerPlayer serverplayer = (ServerPlayer)player;
            ItemStack itemstack = ItemStack.EMPTY;
            Optional<CraftingRecipe> optional = level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, container, level);
            if (optional.isPresent()) {
                CraftingRecipe craftingrecipe = optional.get();
                itemstack = craftingrecipe.assemble(container);
            }

            this.blockEntity.getInventory().setStackInSlot(0,itemstack);
            menu.setRemoteSlot(0, itemstack);
            serverplayer.connection.send(new ClientboundContainerSetSlotPacket(menu.containerId, menu.incrementStateId(), 0, itemstack));
        }
    }

    public void slotsChanged(SlotItemHandler slotItemHandler) {
        this.access.execute((level, pos) -> {
            slotChangedCraftingGrid(this, level, this.player);
        });
    }

    public int getResultSlotIndex() {
        return 0;
    }

}
