package com.watermelon0117.aicraft.client.screen;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import com.watermelon0117.aicraft.menu.AICraftingTableMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookPage;
import net.minecraft.client.gui.screens.recipebook.RecipeButton;
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
    private AICraftingTableMenu menu;
    public ItemStack itemStack=ItemStack.EMPTY;

    public CustomRecipeButton() {
        super(0, 0, 25, 25, CommonComponents.EMPTY);
    }

    public void init(ItemStack itemStack, AICraftingTableMenu menu) {
        this.itemStack=itemStack;
        this.menu=menu;
    }

    public void setPosition(int p_100475_, int p_100476_) {
        this.setX(p_100475_);
        this.setY(p_100476_);
    }

    @Override
    protected void renderWidget(GuiGraphics p_100484_, int p_100485_, int p_100486_, float p_100487_) {
        Minecraft minecraft = Minecraft.getInstance();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, RECIPE_BOOK_LOCATION);
        int i = 29;
        if (!this.menu.canCraft(this.itemStack)) {
            i += 25;
        }
        int j = 206;
        p_100484_.blit(RECIPE_BOOK_LOCATION, this.getX(), this.getY(), i, j, this.width, this.height);
        int k = 4;
        p_100484_.renderFakeItem(itemStack, this.getX() + k, this.getY() + k);
    }
    public List<Component> getTooltipText(Screen p_100478_) {
        return List.of(itemStack.getHoverName());
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput p_170060_) {}

    public int getWidth() {
        return 25;
    }

    protected boolean isValidClickButton(int p_100473_) {
        return p_100473_ == 0 || p_100473_ == 1;
    }
}
