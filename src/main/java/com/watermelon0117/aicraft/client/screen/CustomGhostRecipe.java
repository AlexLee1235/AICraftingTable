package com.watermelon0117.aicraft.client.screen;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.recipebook.GhostRecipe;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.List;

public class CustomGhostRecipe {
    public ItemStack[] itemStacks;

    public void clear() {
        this.itemStacks = null;
    }

    public void setRecipe(ItemStack[] itemStacks) {
        this.itemStacks = itemStacks;
    }

    public void render(PoseStack p_100150_, Minecraft p_100151_, int p_100152_, int p_100153_, boolean p_100154_, float p_100155_) {
        if(itemStacks==null)
            return;
        for(int i = 0; i < 9; ++i) {
            ItemStack itemStack = this.itemStacks[i];
            int j = i%3*18+9 + p_100152_;
            int k = i/3*18+17 + p_100153_;
            if (i == 0 && p_100154_) {
                GuiComponent.fill(p_100150_, j - 4, k - 4, j + 20, k + 20, 822018048);
            } else {
                if(!itemStack.isEmpty())
                    GuiComponent.fill(p_100150_, j, k, j + 16, k + 16, 822018048);
            }

            ItemRenderer itemrenderer = p_100151_.getItemRenderer();
            itemrenderer.renderAndDecorateFakeItem(itemStack, j, k);
            RenderSystem.depthFunc(516);
            GuiComponent.fill(p_100150_, j, k, j + 16, k + 16, 822083583);
            RenderSystem.depthFunc(515);
            if (i == 0) {
                itemrenderer.renderGuiItemDecorations(p_100151_.font, itemStack, j, k);
            }
        }

    }
}
