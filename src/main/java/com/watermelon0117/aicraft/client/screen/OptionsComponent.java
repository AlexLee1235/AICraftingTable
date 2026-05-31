package com.watermelon0117.aicraft.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

public class OptionsComponent implements Renderable, GuiEventListener, NarratableEntry {
    private Button optBtn1, optBtn2, optBtn3;
    public boolean visible = false;
    public OnPressNum optBtnPress;
    public String[] idea;
    private boolean focused;

    public void init(int leftPos, int topPos, OnPressNum optBtnPress) {
        optBtn1 = Button.builder(Component.empty(), this::optBtnPress1).bounds(leftPos + 98, topPos + 16, 70, 17).build();
        optBtn2 = Button.builder(Component.empty(), this::optBtnPress2).bounds(leftPos + 98, topPos + 33, 70, 17).build();
        optBtn3 = Button.builder(Component.empty(), this::optBtnPress3).bounds(leftPos + 98, topPos + 50, 70, 18).build();
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
        optBtn1.setPosition(leftPos + 98, topPos + 16);
        optBtn2.setPosition(leftPos + 98, topPos + 33);
        optBtn3.setPosition(leftPos + 98, topPos + 50);
    }

    @Override
    public void render(GuiGraphics p_94669_, int p_94670_, int p_94671_, float p_94672_) {
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

    @Override
    public void setFocused(boolean focused) {
        this.focused = focused;
    }

    @Override
    public boolean isFocused() {
        return this.focused;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return visible && (optBtn1.isMouseOver(mouseX, mouseY) || optBtn2.isMouseOver(mouseX, mouseY) || optBtn3.isMouseOver(mouseX, mouseY));
    }

    public interface OnPressNum {
        void onPress(Button p_93751_, int i);
    }
}
