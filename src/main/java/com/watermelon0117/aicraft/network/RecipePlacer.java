package com.watermelon0117.aicraft.network;

import com.watermelon0117.aicraft.menu.AICraftingTableMenu;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class RecipePlacer {
    private RecipePlacer() {}

    /* ────────────────────────────────────────────────────────────────── */
    /* 1. Clear the grid – return every item to the player inventory.    */
    /*    Returns false if even one stack cannot be moved back.          */
    /* ────────────────────────────────────────────────────────────────── */
    public static boolean clearGridToInventory(ServerPlayer player, AICraftingTableMenu menu) {
        // 1-a. wipe the result slot right away
        menu.getSlot(AICraftingTableMenu.RESULT_SLOT).set(ItemStack.EMPTY);
        // 1-b. move each grid stack back into the inventory
        for (int grid = AICraftingTableMenu.CRAFT_SLOT_START; grid < AICraftingTableMenu.CRAFT_SLOT_END; ++grid) {
            Slot gridSlot = menu.getSlot(grid);
            ItemStack stack = gridSlot.getItem();
            if (stack.isEmpty()) continue;

            player.getInventory().placeItemBackInInventory(stack, false);
            menu.slots.get(grid).set(ItemStack.EMPTY);
        }
        return true;
    }

    /* ────────────────────────────────────────────────────────────────── */
    /* 2. Place the supplied 3 × 3 recipe pattern.                        */
    /*    Returns false if the player lacks any ingredient.              */
    /* ────────────────────────────────────────────────────────────────── */
    public static void placeRecipePattern(ServerPlayer player, ItemStack[] pattern) {
        if (pattern.length != 9)
            throw new IllegalArgumentException("Pattern must have length 9");

        AbstractContainerMenu menu = player.containerMenu;

        for (int i = 0; i < 9; ++i) {
            ItemStack want = pattern[i];
            if (want.isEmpty()) continue;              // blank slot in recipe

            int invSlotIdx = findMatchingInventorySlot(menu, want);
            if (invSlotIdx == -1) return;        // missing ingredient

            Slot invSlot = menu.getSlot(invSlotIdx);
            Slot gridSlot = menu.getSlot(AICraftingTableMenu.CRAFT_SLOT_START + i);

            invSlot.remove(1);
            ItemStack placed = want.copy();
            placed.setCount(Math.min(gridSlot.getItem().getCount() + 1, gridSlot.getItem().getMaxStackSize()));
            gridSlot.set(placed);

            invSlot.setChanged();
            gridSlot.setChanged();
        }
    }
    public static boolean alreadyHas(ServerPlayer player, ItemStack[] pattern) {
        AbstractContainerMenu menu = player.containerMenu;
        for (int i = 0; i < 9; i++)
            if(!ItemStack.isSameItemSameTags(pattern[i], menu.getSlot(AICraftingTableMenu.CRAFT_SLOT_START + i).getItem())) {
                return false;
            }
        return true;
    }
    public static void forcePlaceRecipePattern(ServerPlayer player, ItemStack[] pattern) {
        if (pattern.length != 9)
            throw new IllegalArgumentException("Pattern must have length 9");
        AbstractContainerMenu menu = player.containerMenu;
        for (int i = 0; i < 9; ++i) {
            Slot gridSlot = menu.getSlot(AICraftingTableMenu.CRAFT_SLOT_START + i);
            ItemStack placed=pattern[i].copy();
            placed.setCount(Math.min(gridSlot.getItem().getCount()+1, gridSlot.getItem().getMaxStackSize()));
            gridSlot.set(placed);
            gridSlot.setChanged();
        }
    }
    /* ────────────────────────────────────────────────────────────────── */
    /* Helper: locate an inventory slot whose stack isSameItemSameTags   */
    /* ────────────────────────────────────────────────────────────────── */
    private static int findMatchingInventorySlot(AbstractContainerMenu menu, ItemStack template) {
        for (int i = AICraftingTableMenu.INV_SLOT_START; i < AICraftingTableMenu.USE_ROW_SLOT_END; ++i) {
            ItemStack inv = menu.getSlot(i).getItem();
            if (!inv.isEmpty() && ItemStack.isSameItemSameTags(inv, template)) {
                return i;
            }
        }
        return -1;
    }
}
