package com.watermelon0117.aicraft.client.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Widget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.recipebook.RecipeShownListener;
import net.minecraft.network.chat.Component;
import net.minecraft.recipebook.PlaceRecipe;
import net.minecraft.world.item.crafting.Ingredient;

public class OptionsComponent extends GuiComponent implements Widget, GuiEventListener, NarratableEntry {
    private Button optBtn1, optBtn2, optBtn3;
    public boolean visible = false;
    public OnPressNum optBtnPress;
    public String[] idea;

    public void init(int leftPos, int topPos, OnPressNum optBtnPress) {
        optBtn1 = new Button(leftPos + 98, topPos + 16, 70, 17,
                Component.empty(), this::optBtnPress1);
        optBtn2 = new Button(leftPos + 98, topPos + 33, 70, 17,
                Component.empty(), this::optBtnPress2);
        optBtn3 = new Button(leftPos + 98, topPos + 50, 70, 18,
                Component.empty(), this::optBtnPress3);
        this.optBtnPress = optBtnPress;
    }

    private void optBtnPress1(Button button) {
        optBtnPress.onPress(button, 0);
    }

    private void optBtnPress2(Button button) {
        optBtnPress.onPress(button, 1);
    }

    private void optBtnPress3(Button button) {
        optBtnPress.onPress(button, 2);
    }

    public void updateWidgetPos(int leftPos, int topPos) {
        optBtn1.x = leftPos + 98;
        optBtn2.x = leftPos + 98;
        optBtn3.x = leftPos + 98;
        optBtn1.y = topPos + 16;
        optBtn2.y = topPos + 33;
        optBtn3.y = topPos + 50;
    }

    @Override
    public void render(PoseStack p_94669_, int p_94670_, int p_94671_, float p_94672_) {
        if (this.visible) {
            optBtn1.render(p_94669_, p_94670_, p_94671_, p_94672_);
            optBtn2.render(p_94669_, p_94670_, p_94671_, p_94672_);
            optBtn3.render(p_94669_, p_94670_, p_94671_, p_94672_);
        }
    }

    @Override
    public boolean mouseClicked(double p_94737_, double p_94738_, int p_94739_) {
        if (this.visible) {
            if (optBtn1.mouseClicked(p_94737_, p_94738_, p_94739_)) return true;
            if (optBtn2.mouseClicked(p_94737_, p_94738_, p_94739_)) return true;
            if (optBtn3.mouseClicked(p_94737_, p_94738_, p_94739_)) return true;
        }
        return false;
    }

    public void setMessage(String[] idea) {
        this.visible = true;
        optBtn1.setMessage(Component.literal(idea[0]));
        optBtn2.setMessage(Component.literal(idea[1]));
        optBtn3.setMessage(Component.literal(idea[2]));
        this.idea = idea;
    }

    @Override
    public NarrationPriority narrationPriority() {
        return NarrationPriority.NONE;
    }

    @Override
    public void updateNarration(NarrationElementOutput p_169152_) {

    }

    public interface OnPressNum {
        void onPress(Button p_93751_, int i);
    }
}
