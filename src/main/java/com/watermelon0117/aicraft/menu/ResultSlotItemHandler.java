package com.watermelon0117.aicraft.menu;

import com.watermelon0117.aicraft.init.ItemInit;
import com.watermelon0117.aicraft.menu.AICraftingTableMenu;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

public class ResultSlotItemHandler extends SlotItemHandler {
    private final ItemStackHandler craftSlots;
    private final Player player;
    private int removeCount;
    private AICraftingTableMenu menu;

    public ResultSlotItemHandler(AICraftingTableMenu menu, Player player, ItemStackHandler itemStackHandler, int p_40169_, int p_40170_, int p_40171_) {
        super(itemStackHandler, p_40169_, p_40170_, p_40171_);
        this.player = player;
        this.craftSlots = itemStackHandler;
        this.menu=menu;
    }

    public boolean mayPlace(ItemStack p_40178_) {
        return false;
    }

    public ItemStack remove(int p_40173_) {
        if (this.hasItem()) {
            this.removeCount += Math.min(p_40173_, this.getItem().getCount());
        }
        return super.remove(p_40173_);
    }

    protected void onQuickCraft(ItemStack p_40180_, int p_40181_) {
        this.removeCount += p_40181_;
        this.checkTakeAchievements(p_40180_);
    }

    protected void onSwapCraft(int p_40183_) {
        this.removeCount += p_40183_;
    }

    protected void checkTakeAchievements(ItemStack p_40185_) {
        if (this.removeCount > 0) {
            p_40185_.onCraftedBy(this.player.level, this.player, this.removeCount);
        }

        this.removeCount = 0;
    }
    private NonNullList<ItemStack> callRecipeManager(){
        CraftingContainer craftingContainer = new CraftingContainer(menu, 3,3);
        for (int i = 0; i < 9; i++)
            craftingContainer.setItem(i, this.craftSlots.getStackInSlot(i+1));
        NonNullList<ItemStack> nonnulllist = player.level.getRecipeManager().getRemainingItemsFor(RecipeType.CRAFTING, craftingContainer, player.level);
        return nonnulllist;
    }
    public void onTake(Player p_150638_, ItemStack p_150639_) {
        if(p_150639_.is(ItemInit.MAIN_ITEM.get())){
            for (int i = 0; i < 9; i++) {
                if(!this.craftSlots.getStackInSlot(i + 1).isEmpty())
                    this.craftSlots.getStackInSlot(i + 1).shrink(1);
            }
        }else {
            this.checkTakeAchievements(p_150639_);
            net.minecraftforge.common.ForgeHooks.setCraftingPlayer(p_150638_);
            NonNullList<ItemStack> nonnulllist = callRecipeManager();
            net.minecraftforge.common.ForgeHooks.setCraftingPlayer(null);
            for (int i = 0; i < nonnulllist.size(); ++i) {
                ItemStack itemstack = this.craftSlots.getStackInSlot(i + 1);
                ItemStack itemstack1 = nonnulllist.get(i);
                if (!itemstack.isEmpty()) {
                    this.craftSlots.getStackInSlot(i + 1).shrink(1);
                    itemstack = this.craftSlots.getStackInSlot(i + 1);
                }

                if (!itemstack1.isEmpty()) {
                    if (itemstack.isEmpty()) {
                        this.craftSlots.setStackInSlot(i + 1, itemstack1);
                    } else if (ItemStack.isSame(itemstack, itemstack1) && ItemStack.tagMatches(itemstack, itemstack1)) {
                        itemstack1.grow(itemstack.getCount());
                        this.craftSlots.setStackInSlot(i + 1, itemstack1);
                    } else if (!this.player.getInventory().add(itemstack1)) {
                        this.player.drop(itemstack1, false);
                    }
                }
            }
        }
        this.menu.blockEntity.setProgress(0);
        this.menu.slotsChanged();
    }

    @Override
    public void set(@NotNull ItemStack stack) {
        super.set(stack);
        menu.hasCraftResult=!stack.isEmpty();
    }

}
