package com.watermelon0117.aicraft.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.watermelon0117.aicraft.menu.AICraftingTableMenu;
import com.watermelon0117.aicraft.network.PacketHandler;
import com.watermelon0117.aicraft.network.SPlaceRecipePacket;
import com.watermelon0117.aicraft.recipes.RecipeManager;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.Widget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.recipebook.*;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.recipebook.PlaceRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.List;

public class CustomRecipeBookComponent extends GuiComponent implements Widget, GuiEventListener, NarratableEntry {
    protected static final ResourceLocation RECIPE_BOOK_LOCATION = new ResourceLocation("textures/gui/recipe_book.png");
    private static final Component SEARCH_HINT = Component.translatable("gui.recipebook.search_hint").withStyle(ChatFormatting.ITALIC).withStyle(ChatFormatting.GRAY);
    public static final int IMAGE_WIDTH = 147;
    public static final int IMAGE_HEIGHT = 166;
    private static final int OFFSET_X_POSITION = 86;
    private static final Component ONLY_CRAFTABLES_TOOLTIP = Component.translatable("gui.recipebook.toggleRecipes.craftable");
    private static final Component ALL_RECIPES_TOOLTIP = Component.translatable("gui.recipebook.toggleRecipes.all");
    private int xOffset;
    private int width;
    private int height;
    Minecraft minecraft;
    private AICraftingTableMenu menu;
    protected final CustomGhostRecipe ghostRecipe = new CustomGhostRecipe();
    private final CustomRecipeBookPage recipeBookPage = new CustomRecipeBookPage();
    public boolean visible;

    public void init(int p_100310_, int p_100311_, Minecraft p_100312_, AICraftingTableMenu menu) {
        this.xOffset = 86;
        this.minecraft=p_100312_;
        this.menu=menu;
        this.width = p_100310_;
        this.height = p_100311_;
        int i = (this.width - 147) / 2 - this.xOffset;
        int j = (this.height - 166) / 2;
        this.recipeBookPage.init(this.minecraft, i, j, menu);

    }
    public void slotClicked(@Nullable Slot p_100315_) {
        if (p_100315_ != null && p_100315_.index < 10) {
            this.ghostRecipe.clear();
        }
    }
    public void render(PoseStack p_100319_, int p_100320_, int p_100321_, float p_100322_) {
        if (this.visible) {
            p_100319_.pushPose();
            p_100319_.translate(0.0D, 0.0D, 100.0D);
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.setShaderTexture(0, RECIPE_BOOK_LOCATION);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            int i = (this.width - 147) / 2 - this.xOffset;
            int j = (this.height - 166) / 2;
            this.blit(p_100319_, i, j, 1, 1, 147, 166);

            this.recipeBookPage.render(p_100319_, i, j, p_100320_, p_100321_, p_100322_);
            p_100319_.popPose();
        }
    }
    public void renderGhostRecipe(PoseStack p_100323_, int p_100324_, int p_100325_, boolean p_100326_, float p_100327_) {
        this.ghostRecipe.render(p_100323_, this.minecraft, p_100324_, p_100325_, false, p_100327_);
    }
    public boolean mouseClicked(double p_100294_, double p_100295_, int p_100296_) {
        if (this.visible && !this.minecraft.player.isSpectator()) {
            if (this.recipeBookPage.mouseClicked(p_100294_, p_100295_, p_100296_, (this.width - 147) / 2 - this.xOffset, (this.height - 166) / 2, 147, 166)) {
                ItemStack itemStack = this.recipeBookPage.getLastClickedRecipe();
                if (itemStack != null && !itemStack.isEmpty()) {
                    this.ghostRecipe.clear();
                    var recipes = RecipeManager.getRecipesForItem(itemStack);
                    if (recipes.isEmpty()) //todo: add multi recipe support
                        return true;
                    PacketHandler.sendToServer(new SPlaceRecipePacket(this.menu.blockEntity.getBlockPos(),
                            recipes.get(0), false));
                }

                return true;
            }
        }
        return false;
    }


    public boolean hasClickedOutside(double p_100298_, double p_100299_, int p_100300_, int p_100301_, int p_100302_, int p_100303_, int p_100304_) {
        if (!this.visible) {
            return true;
        } else {
            boolean flag = p_100298_ < (double)p_100300_ || p_100299_ < (double)p_100301_ || p_100298_ >= (double)(p_100300_ + p_100302_) || p_100299_ >= (double)(p_100301_ + p_100303_);
            boolean flag1 = (double)(p_100300_ - 147) < p_100298_ && p_100298_ < (double)p_100300_ && (double)p_100301_ < p_100299_ && p_100299_ < (double)(p_100301_ + p_100303_);
            return flag && !flag1;
        }
    }
    public void setupGhostRecipe(ItemStack[] recipe) {
        this.ghostRecipe.setRecipe(recipe);
    }
    public void toggleVisibility() {
        this.visible = !this.visible;
    }
    public int updateScreenPosition(int p_181402_, int p_181403_) {
        int i;
        if (this.visible) {
            i = 177 + (p_181402_ - p_181403_ - 200) / 2;
        } else {
            i = (p_181402_ - p_181403_) / 2;
        }

        return i;
    }

    public NarratableEntry.NarrationPriority narrationPriority() {
        return NarratableEntry.NarrationPriority.NONE;
    }
    public void updateNarration(NarrationElementOutput p_170046_) {}
}

