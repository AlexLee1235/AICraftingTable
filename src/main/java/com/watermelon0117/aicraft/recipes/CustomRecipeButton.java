package com.watermelon0117.aicraft.recipes;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookPage;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.stats.RecipeBook;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;

import java.util.List;

public class CustomRecipeButton extends AbstractWidget {
    private static final ResourceLocation RECIPE_BOOK_LOCATION = new ResourceLocation("textures/gui/recipe_book.png");
    private static final int BACKGROUND_SIZE = 25;
    public static final int TICKS_TO_SWAP = 30;
    private float time;
    private int currentIndex;

    public CustomRecipeButton() {
        super(0, 0, 25, 25, CommonComponents.EMPTY);
    }

    public void init(RecipeCollection p_100480_, CustomRecipeBookPage p_100481_) {

    }

    public void setPosition(int p_100475_, int p_100476_) {
        this.x = p_100475_;
        this.y = p_100476_;
    }

    public void renderButton(PoseStack p_100484_, int p_100485_, int p_100486_, float p_100487_) {
        if (!Screen.hasControlDown()) {
            this.time += p_100487_;
        }

        Minecraft minecraft = Minecraft.getInstance();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, RECIPE_BOOK_LOCATION);
        int i = 29;
        if (!this.collection.hasCraftable()) {
            i += 25;
        }

        int j = 206;
        if (this.collection.getRecipes(false).size() > 1) {
            j += 25;
        }

        this.blit(p_100484_, this.x, this.y, i, j, this.width, this.height);
        List<net.minecraft.world.item.crafting.Recipe<?>> list = this.getOrderedRecipes();
        this.currentIndex = Mth.floor(this.time / 30.0F) % list.size();
        ItemStack itemstack = list.get(this.currentIndex).getResultItem();
        int k = 4;
        minecraft.getItemRenderer().renderAndDecorateFakeItem(itemstack, this.x + k, this.y + k);
    }

    private List<net.minecraft.world.item.crafting.Recipe<?>> getOrderedRecipes() {
        List<net.minecraft.world.item.crafting.Recipe<?>> list = this.collection.getDisplayRecipes(true);
        if (!this.book.isFiltering(this.menu)) {
            list.addAll(this.collection.getDisplayRecipes(false));
        }

        return list;
    }

    public boolean isOnlyOption() {
        return this.getOrderedRecipes().size() == 1;
    }

    public net.minecraft.world.item.crafting.Recipe<?> getRecipe() {
        List<Recipe<?>> list = this.getOrderedRecipes();
        return list.get(this.currentIndex);
    }


    public void updateNarration(NarrationElementOutput p_170060_) {}

    public int getWidth() {
        return 25;
    }

    protected boolean isValidClickButton(int p_100473_) {
        return p_100473_ == 0 || p_100473_ == 1;
    }
}
