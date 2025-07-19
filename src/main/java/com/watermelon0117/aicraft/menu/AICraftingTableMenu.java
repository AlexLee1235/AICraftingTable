package com.watermelon0117.aicraft.menu;

import com.watermelon0117.aicraft.init.BlockInit;
import com.watermelon0117.aicraft.items.MainItem;
import com.watermelon0117.aicraft.common.ItemStackArray;
import com.watermelon0117.aicraft.common.RecipeManager;
import com.watermelon0117.aicraft.blockentities.AICraftingTableBlockEntity;
import com.watermelon0117.aicraft.init.MenuInit;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.RepairItemRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.*;

public class AICraftingTableMenu extends AbstractContainerMenu {
    public static final int RESULT_SLOT = 0;
    public static final int CRAFT_SLOT_START = 1;
    public static final int CRAFT_SLOT_END = 10;
    public static final int INV_SLOT_START = 10;
    public static final int INV_SLOT_END = 37;
    public static final int USE_ROW_SLOT_START = 37;
    public static final int USE_ROW_SLOT_END = 46;
    private final ContainerLevelAccess access;
    private final Player player;
    public final AICraftingTableBlockEntity blockEntity;
    public boolean hasCraftResult;

    public ItemStackArray currentRecipe;
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
        this.hasCraftResult = !this.getSlot(0).getItem().isEmpty();
        currentRecipe=new ItemStackArray(this);
    }

    private ItemStack delegateQuickMoveStack(Player player, int slotId){
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotId);
        if (slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();
            if (slotId == 0) {  //shift take result
                this.access.execute((p_39378_, p_39379_) -> {
                    itemstack1.getItem().onCraftedBy(itemstack1, p_39378_, player);
                });
                if (!this.moveItemStackTo(itemstack1, 10, 46, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickCraft(itemstack1, itemstack);
            } else if (slotId >= 10 && slotId < 46) {  //if click in inventory and hot bar
                if (!this.moveItemStackTo(itemstack1, 1, 10, false)) {
                    if (slotId < 37) {  //swap between inventory and hot bar
                        if (!this.moveItemStackTo(itemstack1, 37, 46, false)) {
                            return ItemStack.EMPTY;
                        }
                    } else if (!this.moveItemStackTo(itemstack1, 10, 37, false)) {
                        return ItemStack.EMPTY;
                    }
                }
            } else if (!this.moveItemStackTo(itemstack1, 10, 46, false)) {
                //click in material, if inv full return
                return ItemStack.EMPTY;
            }

            if (itemstack1.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (itemstack1.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, itemstack);
            if (slotId == 0) {
                player.drop(itemstack1, false);
            }
        }
        //System.out.println("return"+itemstack.getDisplayName());
        return itemstack;
    }
    public ItemStack quickMoveStack(Player player, int slotId) {
        ItemStackArray recipe= ItemStackArray.deepCopy(currentRecipe);
        ItemStack itemStack=delegateQuickMoveStack(player,slotId);
        handleInterrupt(recipe,this,player.level,player);
        return itemStack;
    }

    @Override
    public boolean stillValid(Player p_38874_) {
        return stillValid(access, p_38874_, BlockInit.AI_CRAFTING_TABLE.get());
    }

    private static CraftingContainer getDummyContainer(AICraftingTableMenu menu){
        CraftingContainer craftingContainer = new CraftingContainer(menu, 3,3);
        for (int i = 0; i < 9; i++) {
            craftingContainer.setItem(i, menu.blockEntity.getInventory().getStackInSlot(i + 1).copy());
        }
        return craftingContainer;
    }

    protected static void slotChangedCraftingGrid(AICraftingTableMenu menu, Level level, Player player) {
        if (!level.isClientSide) {
            ServerPlayer serverplayer = (ServerPlayer)player;
            ItemStack itemstack = RecipeManager.callRecipeManager(getDummyContainer(menu), level);

            menu.blockEntity.getInventory().setStackInSlot(0,itemstack);
            menu.setRemoteSlot(0, itemstack);
            serverplayer.connection.send(new ClientboundContainerSetSlotPacket(menu.containerId, menu.incrementStateId(), 0, itemstack));
        }
    }
    private void handleInterrupt(ItemStackArray currentRecipe, AbstractContainerMenu menu, Level level, Player player) {
        if (!level.isClientSide) {
            ServerPlayer serverplayer = (ServerPlayer) player;
            if (this.blockEntity.getProgress() > 0) {
                if (!currentRecipe.equals(new ItemStackArray(this))) {
                    System.out.println("stop by handleInterrupt");
                    this.blockEntity.setProgress(0);
                    this.blockEntity.getInventory().setStackInSlot(0, ItemStack.EMPTY);
                    menu.setRemoteSlot(0, ItemStack.EMPTY);
                    serverplayer.connection.send(new ClientboundContainerSetSlotPacket(menu.containerId, menu.incrementStateId(), 0, ItemStack.EMPTY));
                }
            }
        }
    }

    public void slotsChanged() {
        this.access.execute((level, pos) -> {
            handleInterrupt(currentRecipe, this, level, this.player);
            slotChangedCraftingGrid(this, level, this.player);
            currentRecipe=new ItemStackArray(this);
        });
    }
    public boolean canCraft(ItemStack itemStack) {
        List<ItemStack[]> recipes = RecipeManager.get().getRecipesForItem(itemStack);
        for (var recipe : recipes)
            if (canCraftRecipe(recipe))
                return true;
        return false;
    }
    public boolean canCraftRecipe(ItemStack[] recipe) {
        if (recipe == null || recipe.length != 9)
            throw new IllegalArgumentException("recipe must contain exactly 9 entries");
        /* 1 ── Copy every non-empty recipe slot into a “still-needed” list. */
        List<ItemStack> need = new ArrayList<>();
        for (ItemStack s : recipe) {
            if (s != null && !s.isEmpty()) need.add(s.copy());   // we’ll shrink() later
        }
        /* 2 ── Walk container slots 1-46, paying down those needs. */
        for (int slot = 1; slot < this.slots.size() && !need.isEmpty(); ++slot) {
            ItemStack inv = this.getSlot(slot).getItem();
            if (inv.isEmpty()) continue;
            int invLeft = inv.getCount();
            // Consume this inventory stack against *all* matching needs
            for (Iterator<ItemStack> it = need.iterator(); it.hasNext() && invLeft > 0; ) {
                ItemStack req = it.next();
                if (!ItemStack.isSameItemSameTags(inv, req)) continue;               // different ingredient

                int take = Math.min(invLeft, req.getCount());
                invLeft -= take;
                req.shrink(take);                                // pay down this need
                if (req.isEmpty()) it.remove();                  // fully satisfied
            }
        }
        return need.isEmpty();                                   // true ⇒ everything found
    }
    public boolean moveItemStackTo(ItemStack stack, int start, int end, boolean reverse) { //make it public
        return super.moveItemStackTo(stack, start, end, reverse);
    }
}
