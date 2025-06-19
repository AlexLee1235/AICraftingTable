package com.watermelon0117.aicraft.menu;

import com.watermelon0117.aicraft.menu.AICraftingTableMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

public class CraftingSlotItemHandler extends SlotItemHandler {
    private final AICraftingTableMenu menu;
    public CraftingSlotItemHandler(AICraftingTableMenu menu, IItemHandler itemHandler, int index, int xPosition, int yPosition) {
        super(itemHandler, index, xPosition, yPosition);
        this.menu=menu;
    }

    @Override
    public void set(@NotNull ItemStack stack) {
        super.set(stack);
        this.menu.slotsChanged();
    }
}
