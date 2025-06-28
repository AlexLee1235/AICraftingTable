package com.watermelon0117.aicraft.client.screen;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.PoseStack;
import com.watermelon0117.aicraft.menu.AICraftingTableMenu;
import com.watermelon0117.aicraft.recipes.SpecialItemManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.StateSwitchingButton;
import net.minecraft.client.gui.screens.recipebook.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.stats.RecipeBook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class CustomRecipeBookPage {
    protected static final ResourceLocation RECIPE_BOOK_LOCATION = new ResourceLocation("textures/gui/recipe_book.png");
    public static final int ITEMS_PER_PAGE = 20;
    private final List<CustomRecipeButton> buttons = Lists.newArrayListWithCapacity(20);
    @Nullable
    private CustomRecipeButton hoveredButton;
    private Minecraft minecraft;
    private StateSwitchingButton forwardButton;
    private StateSwitchingButton backButton;
    private int totalPages;
    private int currentPage;
    private AICraftingTableMenu menu;
    @Nullable
    private ItemStack lastClickedRecipe;

    public CustomRecipeBookPage() {
        for(int i = 0; i < 20; ++i) {
            this.buttons.add(new CustomRecipeButton());
        }
        this.totalPages = (int)Math.ceil((double)(SpecialItemManager.getAllItems().size()) / 20.0D);
    }

    public void init(Minecraft p_100429_, int p_100430_, int p_100431_, AICraftingTableMenu menu) {
        this.minecraft = p_100429_;
        this.menu=menu;

        for(int i = 0; i < this.buttons.size(); ++i) {
            this.buttons.get(i).setPosition(p_100430_ + 11 + 25 * (i % 5), p_100431_ + 31 + 25 * (i / 5));
        }

        this.forwardButton = new StateSwitchingButton(p_100430_ + 93, p_100431_ + 137, 12, 17, false);
        this.forwardButton.initTextureValues(1, 208, 13, 18, RECIPE_BOOK_LOCATION);
        this.backButton = new StateSwitchingButton(p_100430_ + 38, p_100431_ + 137, 12, 17, true);
        this.backButton.initTextureValues(1, 208, 13, 18, RECIPE_BOOK_LOCATION);
        this.updateButtonsForPage();
    }

    private void updateButtonsForPage() {
        int i = 20 * this.currentPage;
        ArrayList<ItemStack> allItems= SpecialItemManager.getAllItems();
        for(int j = 0; j < this.buttons.size(); ++j) {
            CustomRecipeButton recipebutton = this.buttons.get(j);
            if (i + j < allItems.size()) {
                ItemStack itemStack = allItems.get(i + j);
                recipebutton.init(itemStack, this.menu);
                recipebutton.visible = true;
            } else {
                recipebutton.visible = false;
            }
        }

        this.updateArrowButtons();
    }

    private void updateArrowButtons() {
        this.forwardButton.visible = this.totalPages > 1 && this.currentPage < this.totalPages - 1;
        this.backButton.visible = this.totalPages > 1 && this.currentPage > 0;
    }

    public void render(PoseStack p_100422_, int p_100423_, int p_100424_, int p_100425_, int p_100426_, float p_100427_) {
        if (this.totalPages > 1) {
            String s = this.currentPage + 1 + "/" + this.totalPages;
            int i = this.minecraft.font.width(s);
            this.minecraft.font.draw(p_100422_, s, (float)(p_100423_ - i / 2 + 73), (float)(p_100424_ + 141), -1);
        }

        this.hoveredButton = null;

        for(CustomRecipeButton recipebutton : this.buttons) {
            recipebutton.render(p_100422_, p_100425_, p_100426_, p_100427_);
            if (recipebutton.visible && recipebutton.isHoveredOrFocused()) {
                this.hoveredButton = recipebutton;
            }
        }

        this.backButton.render(p_100422_, p_100425_, p_100426_, p_100427_);
        this.forwardButton.render(p_100422_, p_100425_, p_100426_, p_100427_);
    }
    public void renderTooltip(PoseStack p_100418_, int p_100419_, int p_100420_) {
        if (this.minecraft.screen != null && this.hoveredButton != null) {
            this.minecraft.screen.renderComponentTooltip(p_100418_, this.hoveredButton.getTooltipText(this.minecraft.screen), p_100419_, p_100420_, this.hoveredButton.itemStack);
        }

    }

    public boolean mouseClicked(double p_100410_, double p_100411_, int p_100412_, int p_100413_, int p_100414_, int p_100415_, int p_100416_) {
        this.lastClickedRecipe = null;
        if (this.forwardButton.mouseClicked(p_100410_, p_100411_, p_100412_)) {
            ++this.currentPage;
            this.updateButtonsForPage();
            return true;
        } else if (this.backButton.mouseClicked(p_100410_, p_100411_, p_100412_)) {
            --this.currentPage;
            this.updateButtonsForPage();
            return true;
        } else {
            for(CustomRecipeButton recipebutton : this.buttons) {
                if (recipebutton.mouseClicked(p_100410_, p_100411_, p_100412_)) {
                    this.lastClickedRecipe = recipebutton.itemStack;
                    return true;
                }
            }
        }
        return false;
    }
    @Nullable
    public ItemStack getLastClickedRecipe() {
        return this.lastClickedRecipe;
    }
}
