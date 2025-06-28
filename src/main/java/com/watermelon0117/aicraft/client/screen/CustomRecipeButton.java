package com.watermelon0117.aicraft.client.screen;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.watermelon0117.aicraft.menu.AICraftingTableMenu;
import net.minecraft.client.Minecraft;
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
        this.x = p_100475_;
        this.y = p_100476_;
    }

    public void renderButton(PoseStack p_100484_, int p_100485_, int p_100486_, float p_100487_) {
        Minecraft minecraft = Minecraft.getInstance();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, RECIPE_BOOK_LOCATION);
        int i = 29;
        if (!this.menu.canCraft(this.itemStack)) {
            i += 25;
        }
        int j = 206;
        this.blit(p_100484_, this.x, this.y, i, j, this.width, this.height);
        int k = 4;
        minecraft.getItemRenderer().renderAndDecorateFakeItem(itemStack, this.x + k, this.y + k);
    }
    public List<Component> getTooltipText(Screen p_100478_) {
        return List.of(itemStack.getHoverName());
    }

    public void updateNarration(NarrationElementOutput p_170060_) {}

    public int getWidth() {
        return 25;
    }

    protected boolean isValidClickButton(int p_100473_) {
        return p_100473_ == 0 || p_100473_ == 1;
    }
}
