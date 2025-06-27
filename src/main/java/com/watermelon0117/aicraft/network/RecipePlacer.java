package com.watermelon0117.aicraft.network;

import com.watermelon0117.aicraft.menu.AICraftingTableMenu;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class RecipePlacer {
    private static final int RESULT_SLOT = 0;
    private static final int GRID_FIRST  = 1;
    private static final int GRID_LAST   = 9;
    private static final int INV_FIRST   = 10;
    private static final int INV_LAST    = 46;           // inclusive

    private RecipePlacer() {}

    /* ────────────────────────────────────────────────────────────────── */
    /* 1. Clear the grid – return every item to the player inventory.    */
    /*    Returns false if even one stack cannot be moved back.          */
    /* ────────────────────────────────────────────────────────────────── */
    public static boolean clearGridToInventory(ServerPlayer player, AICraftingTableMenu menu) {
        // 1-a. wipe the result slot right away
        menu.getSlot(RESULT_SLOT).set(ItemStack.EMPTY);
        // 1-b. move each grid stack back into the inventory
        for (int grid = GRID_FIRST; grid <= GRID_LAST; ++grid) {
            Slot gridSlot = menu.getSlot(grid);
            ItemStack stack = gridSlot.getItem();
            if (stack.isEmpty()) continue;
            /* menu.moveItemStackTo tries to merge the stack into the given
               range and returns false if *nothing* could be moved.        */
            if (!menu.moveItemStackTo(stack, INV_FIRST, INV_LAST + 1, false)) {
                return false;                          // no space – abort
            }
            gridSlot.setChanged();
        }
        return true;
    }

    /* ────────────────────────────────────────────────────────────────── */
    /* 2. Place the supplied 3 × 3 recipe pattern.                        */
    /*    Returns false if the player lacks any ingredient.              */
    /* ────────────────────────────────────────────────────────────────── */
    public static boolean placeRecipePattern(ServerPlayer player, ItemStack[] pattern) {
        if (pattern.length != 9)
            throw new IllegalArgumentException("Pattern must have length 9");

        AbstractContainerMenu menu = player.containerMenu;

        for (int i = 0; i < 9; ++i) {
            ItemStack want = pattern[i];
            if (want.isEmpty()) continue;              // blank slot in recipe

            int invSlotIdx = findMatchingInventorySlot(menu, want);
            if (invSlotIdx == -1) return false;        // missing ingredient

            Slot invSlot  = menu.getSlot(invSlotIdx);
            Slot gridSlot = menu.getSlot(GRID_FIRST + i);

            /* Remove ONE item from the inventory stack */
            invSlot.remove(1);

            /* Put a single-item copy into the crafting grid */
            ItemStack placed = want.copy();
            placed.setCount(1);
            gridSlot.set(placed);

            invSlot.setChanged();
            gridSlot.setChanged();
        }
        return true;
    }
    /* ────────────────────────────────────────────────────────────────── */
    /* Helper: locate an inventory slot whose stack isSameItemSameTags   */
    /* ────────────────────────────────────────────────────────────────── */
    private static int findMatchingInventorySlot(AbstractContainerMenu menu, ItemStack template) {
        for (int i = INV_FIRST; i <= INV_LAST; ++i) {
            ItemStack inv = menu.getSlot(i).getItem();
            if (!inv.isEmpty() && ItemStack.isSameItemSameTags(inv, template)) {
                return i;
            }
        }
        return -1;
    }
}
