package com.watermelon0117.aicraft.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.watermelon0117.aicraft.AICraftingTable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Widget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class OptionsComponent extends GuiComponent implements Widget, GuiEventListener, NarratableEntry {
    private static final ResourceLocation BUTTONS_LOCATION = new ResourceLocation(
            AICraftingTable.MODID, "textures/gui/options_3_buttons.png");
    private static final int BUTTON_GROUP_WIDTH = 69;
    private static final int BUTTON_GROUP_HEIGHT = 53;

    private TextOnlyButton optBtn1, optBtn2, optBtn3;
    public boolean visible = false;
    public OnPressNum optBtnPress;
    public String[] idea;

    public void init(int leftPos, int topPos, OnPressNum optBtnPress) {
        optBtn1 = new TextOnlyButton(leftPos + 98, topPos + 16, BUTTON_GROUP_WIDTH, 17, this::optBtnPress1);
        optBtn2 = new TextOnlyButton(leftPos + 98, topPos + 33, BUTTON_GROUP_WIDTH, 17, this::optBtnPress2);
        optBtn3 = new TextOnlyButton(leftPos + 98, topPos + 50, BUTTON_GROUP_WIDTH, 19, this::optBtnPress3);
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
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTicks) {
        if (this.visible) {
            int textureY = getHoveredButtonIndex(mouseX, mouseY) * BUTTON_GROUP_HEIGHT;
            RenderSystem.setShaderTexture(0, BUTTONS_LOCATION);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            blit(poseStack, optBtn1.x, optBtn1.y,
                    0, textureY, BUTTON_GROUP_WIDTH, BUTTON_GROUP_HEIGHT);
            optBtn1.render(poseStack, mouseX, mouseY, partialTicks);
            optBtn2.render(poseStack, mouseX, mouseY, partialTicks);
            optBtn3.render(poseStack, mouseX, mouseY, partialTicks);
        }
    }

    private int getHoveredButtonIndex(int mouseX, int mouseY) {
        if (optBtn1.isMouseOver(mouseX, mouseY)) return 1;
        if (optBtn2.isMouseOver(mouseX, mouseY)) return 2;
        if (optBtn3.isMouseOver(mouseX, mouseY)) return 3;
        if (optBtn1.isActive() && optBtn1.isFocused()) return 1;
        if (optBtn2.isActive() && optBtn2.isFocused()) return 2;
        if (optBtn3.isActive() && optBtn3.isFocused()) return 3;
        return 0;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.visible) {
            if (optBtn1.mouseClicked(mouseX, mouseY, button)) {
                setFocusedButton(optBtn1);
                return true;
            }
            if (optBtn2.mouseClicked(mouseX, mouseY, button)) {
                setFocusedButton(optBtn2);
                return true;
            }
            if (optBtn3.mouseClicked(mouseX, mouseY, button)) {
                setFocusedButton(optBtn3);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.visible) {
            if (optBtn1.isFocused()) return optBtn1.keyPressed(keyCode, scanCode, modifiers);
            if (optBtn2.isFocused()) return optBtn2.keyPressed(keyCode, scanCode, modifiers);
            if (optBtn3.isFocused()) return optBtn3.keyPressed(keyCode, scanCode, modifiers);
        }
        return false;
    }

    @Override
    public boolean changeFocus(boolean forward) {
        if (!this.visible) {
            setFocusedButton(null);
            return false;
        }

        TextOnlyButton[] buttons = {optBtn1, optBtn2, optBtn3};
        int focusedIndex = -1;
        for (int i = 0; i < buttons.length; i++) {
            if (buttons[i].isFocused()) {
                focusedIndex = i;
                break;
            }
        }

        int nextIndex = focusedIndex == -1
                ? (forward ? 0 : buttons.length - 1)
                : focusedIndex + (forward ? 1 : -1);
        if (nextIndex < 0 || nextIndex >= buttons.length) {
            setFocusedButton(null);
            return false;
        }

        setFocusedButton(buttons[nextIndex]);
        return true;
    }

    private void setFocusedButton(TextOnlyButton button) {
        optBtn1.setFocusedState(button == optBtn1);
        optBtn2.setFocusedState(button == optBtn2);
        optBtn3.setFocusedState(button == optBtn3);
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return visible && (optBtn1.isMouseOver(mouseX, mouseY)
                || optBtn2.isMouseOver(mouseX, mouseY)
                || optBtn3.isMouseOver(mouseX, mouseY));
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
    public void updateNarration(NarrationElementOutput narrationElementOutput) {
    }

    public interface OnPressNum {
        void onPress(Button button, int i);
    }

    /** Keeps button interaction while the parent component renders the shared background. */
    private static final class TextOnlyButton extends Button {
        private TextOnlyButton(int x, int y, int width, int height, OnPress onPress) {
            super(x, y, width, height, Component.empty(), onPress);
        }

        private void setFocusedState(boolean focused) {
            super.setFocused(focused);
        }

        @Override
        public void renderButton(PoseStack poseStack, int mouseX, int mouseY, float partialTicks) {
            Minecraft minecraft = Minecraft.getInstance();
            int textColor = getFGColor() | Mth.ceil(alpha * 255.0F) << 24;
            drawCenteredString(poseStack, minecraft.font, getMessage(),
                    x + getWidth() / 2, y + (getHeight() - 8) / 2, textColor);
        }
    }
}
