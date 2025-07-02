package com.watermelon0117.aicraft.menu;

import com.watermelon0117.aicraft.common.RecipeManager;
import com.watermelon0117.aicraft.items.MainItem;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class MyResultSlot extends ResultSlot {
    private final CraftingContainer craftSlots;

    public MyResultSlot(Player p_40166_, CraftingContainer p_40167_, Container p_40168_, int p_40169_, int p_40170_, int p_40171_) {
        super(p_40166_, p_40167_, p_40168_, p_40169_, p_40170_, p_40171_);
        this.craftSlots = p_40167_;
    }

    public void onTake(Player player, ItemStack itemStack) {
        ItemStack[] itemStacks = new ItemStack[9];
        for (int i = 0; i < 9; i++) {
            itemStacks[i] = this.craftSlots.getItem(i);
        }
        ItemStack itemstack2 = RecipeManager.get().match(itemStacks);
        if (MainItem.isMainItem(itemstack2)) {
            for (int i = 0; i < 9; i++) {
                if (!this.craftSlots.getItem(i).isEmpty()) {
                    if (this.craftSlots.getItem(i).is(Items.WATER_BUCKET) ||
                            this.craftSlots.getItem(i).is(Items.LAVA_BUCKET) ||
                            this.craftSlots.getItem(i).is(Items.MILK_BUCKET)
                    )
                        this.craftSlots.setItem(i, new ItemStack(Items.BUCKET));
                    else
                        this.craftSlots.removeItem(i, 1);
                }
            }
        } else {
            super.onTake(player, itemStack);
        }
    }
}
