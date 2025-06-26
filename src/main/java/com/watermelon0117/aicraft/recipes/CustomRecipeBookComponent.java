package com.watermelon0117.aicraft.recipes;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.watermelon0117.aicraft.menu.AICraftingTableMenu;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import net.minecraft.ChatFormatting;
import net.minecraft.client.ClientRecipeBook;
import net.minecraft.client.Minecraft;
import net.minecraft.client.RecipeBookCategories;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StateSwitchingButton;
import net.minecraft.client.gui.components.Widget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.recipebook.*;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.resources.language.LanguageInfo;
import net.minecraft.client.resources.language.LanguageManager;
import net.minecraft.client.searchtree.SearchRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundRecipeBookChangeSettingsPacket;
import net.minecraft.recipebook.PlaceRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.inventory.RecipeBookType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public class CustomRecipeBookComponent extends GuiComponent implements PlaceRecipe<Ingredient>, Widget, GuiEventListener, NarratableEntry, RecipeShownListener {
    protected static final ResourceLocation RECIPE_BOOK_LOCATION = new ResourceLocation("textures/gui/recipe_book.png");
    public static final int IMAGE_WIDTH = 147;
    public static final int IMAGE_HEIGHT = 166;
    private static final int OFFSET_X_POSITION = 86;
    private int xOffset;
    private int width;
    private int height;
    protected final GhostRecipe ghostRecipe = new GhostRecipe();
    protected AICraftingTableMenu menu;
    protected Minecraft minecraft;
    private final CustomRecipeBookPage recipeBookPage = new CustomRecipeBookPage();
    private int timesInventoryChanged;
    private boolean visible;

    public void init(int p_100310_, int p_100311_, Minecraft p_100312_, boolean p_100313_, AICraftingTableMenu p_100314_) {
        this.minecraft = p_100312_;
        this.width = p_100310_;
        this.height = p_100311_;
        this.menu = p_100314_;
        p_100312_.player.containerMenu = p_100314_;
        this.timesInventoryChanged = p_100312_.player.getInventory().getTimesChanged();
        this.visible = false;
        if (this.visible) {
            this.initVisuals();
        }

        p_100312_.keyboardHandler.setSendRepeatsToGui(true);
    }

    public void initVisuals() {
        this.xOffset = 86;
        int i = (this.width - 147) / 2 - this.xOffset;
        int j = (this.height - 166) / 2;
        this.recipeBookPage.init(this.minecraft, i, j);
        this.recipeBookPage.addListener(this);

        this.updateCollections();
    }

    public boolean changeFocus(boolean p_100372_) {
        return false;
    }

    public int updateScreenPosition(int p_181402_, int p_181403_) {
        int i;
        if (this.isVisible()) {
            i = 177 + (p_181402_ - p_181403_ - 200) / 2;
        } else {
            i = (p_181402_ - p_181403_) / 2;
        }

        return i;
    }

    public void toggleVisibility() {
        this.setVisible(!this.isVisible());
    }

    public boolean isVisible() {
        return this.visible;
    }

    protected void setVisible(boolean p_100370_) {
        if (p_100370_) {
            this.initVisuals();
        }

        this.visible = p_100370_;
        if (!p_100370_) {
            this.recipeBookPage.setInvisible();
        }
    }

    public void slotClicked(@Nullable Slot p_100315_) {
        if (p_100315_ != null && p_100315_.index < 10) {
            this.ghostRecipe.clear();
        }

    }

    private void updateCollections() {
        this.recipeBookPage.updateCollections(list);
    }

    public void tick() {
        if (this.isVisible()) {
            if (this.timesInventoryChanged != this.minecraft.player.getInventory().getTimesChanged()) {
                this.timesInventoryChanged = this.minecraft.player.getInventory().getTimesChanged();
            }

        }
    }

    public void render(PoseStack p_100319_, int p_100320_, int p_100321_, float p_100322_) {
        if (this.isVisible()) {
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
        this.ghostRecipe.render(p_100323_, this.minecraft, p_100324_, p_100325_, p_100326_, p_100327_);
    }

    public boolean mouseClicked(double p_100294_, double p_100295_, int p_100296_) {
        if (this.isVisible() && !this.minecraft.player.isSpectator()) {
            if (this.recipeBookPage.mouseClicked(p_100294_, p_100295_, p_100296_, (this.width - 147) / 2 - this.xOffset, (this.height - 166) / 2, 147, 166)) {
                net.minecraft.world.item.crafting.Recipe<?> recipe = this.recipeBookPage.getLastClickedRecipe();
                RecipeCollection recipecollection = this.recipeBookPage.getLastClickedRecipeCollection();
                if (recipe != null && recipecollection != null) {
                    if (!recipecollection.isCraftable(recipe) && this.ghostRecipe.getRecipe() == recipe) {
                        return false;
                    }

                    this.ghostRecipe.clear();
                    this.minecraft.gameMode.handlePlaceRecipe(this.minecraft.player.containerMenu.containerId, recipe, Screen.hasShiftDown());
                    if (!this.isOffsetNextToMainGUI()) {
                        this.setVisible(false);
                    }
                }

                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public boolean hasClickedOutside(double p_100298_, double p_100299_, int p_100300_, int p_100301_, int p_100302_, int p_100303_, int p_100304_) {
        if (!this.isVisible()) {
            return true;
        } else {
            boolean flag = p_100298_ < (double)p_100300_ || p_100299_ < (double)p_100301_ || p_100298_ >= (double)(p_100300_ + p_100302_) || p_100299_ >= (double)(p_100301_ + p_100303_);
            boolean flag1 = (double)(p_100300_ - 147) < p_100298_ && p_100298_ < (double)p_100300_ && (double)p_100301_ < p_100299_ && p_100299_ < (double)(p_100301_ + p_100303_);
            return flag && !flag1;
        }
    }

    public boolean isMouseOver(double p_100353_, double p_100354_) {
        return false;
    }


    private boolean isOffsetNextToMainGUI() {
        return this.xOffset == 86;
    }

    public void recipesUpdated() {
        if (this.isVisible()) {
            this.updateCollections();
        }

    }

    public void recipesShown(List<net.minecraft.world.item.crafting.Recipe<?>> p_100344_) {
        for(net.minecraft.world.item.crafting.Recipe<?> recipe : p_100344_) {
            this.minecraft.player.removeRecipeHighlight(recipe);
        }

    }

    public void setupGhostRecipe(Recipe<?> p_100316_, List<Slot> p_100317_) {
        ItemStack itemstack = p_100316_.getResultItem();
        this.ghostRecipe.setRecipe(p_100316_);
        this.ghostRecipe.addIngredient(Ingredient.of(itemstack), (p_100317_.get(0)).x, (p_100317_.get(0)).y);
        this.placeRecipe(3,3,0, p_100316_, p_100316_.getIngredients().iterator(), 0);
    }

    public void addItemToSlot(Iterator<Ingredient> p_100338_, int p_100339_, int p_100340_, int p_100341_, int p_100342_) {
        Ingredient ingredient = p_100338_.next();
        if (!ingredient.isEmpty()) {
            Slot slot = this.menu.slots.get(p_100339_);
            this.ghostRecipe.addIngredient(ingredient, slot.x, slot.y);
        }

    }

    public NarratableEntry.NarrationPriority narrationPriority() {
        return NarratableEntry.NarrationPriority.NONE;
    }

    public void updateNarration(NarrationElementOutput p_170046_) {

    }
}
