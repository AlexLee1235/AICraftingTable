package com.watermelon0117.aicraft.client.screen;

import com.watermelon0117.aicraft.AICraftingTable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class OptionsComponent implements Renderable, GuiEventListener, NarratableEntry {
    private static final ResourceLocation BUTTONS_LOCATION = new ResourceLocation(
            AICraftingTable.MODID, "textures/gui/options_3_buttons.png");
    private static final int BUTTON_GROUP_WIDTH = 69;
    private static final int BUTTON_GROUP_HEIGHT = 53;

    private Button optBtn1, optBtn2, optBtn3;
    public boolean visible = false;
    public OnPressNum optBtnPress;
    public String[] idea;
    private boolean focused;

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
        optBtn1.setPosition(leftPos + 98, topPos + 16);
        optBtn2.setPosition(leftPos + 98, topPos + 33);
        optBtn3.setPosition(leftPos + 98, topPos + 50);
    }

    @Override
    public void render(GuiGraphics p_94669_, int p_94670_, int p_94671_, float p_94672_) {
        if (this.visible) {
            int textureY = getHoveredButtonIndex(p_94670_, p_94671_) * BUTTON_GROUP_HEIGHT;
            p_94669_.blit(BUTTONS_LOCATION, optBtn1.getX(), optBtn1.getY(),
                    0, textureY, BUTTON_GROUP_WIDTH, BUTTON_GROUP_HEIGHT);
            optBtn1.render(p_94669_, p_94670_, p_94671_, p_94672_);
            optBtn2.render(p_94669_, p_94670_, p_94671_, p_94672_);
            optBtn3.render(p_94669_, p_94670_, p_94671_, p_94672_);
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
    public boolean mouseClicked(double p_94737_, double p_94738_, int p_94739_) {
        if (this.visible) {
            if (optBtn1.mouseClicked(p_94737_, p_94738_, p_94739_)) {
                setFocusedButton(optBtn1);
                return true;
            }
            if (optBtn2.mouseClicked(p_94737_, p_94738_, p_94739_)) {
                setFocusedButton(optBtn2);
                return true;
            }
            if (optBtn3.mouseClicked(p_94737_, p_94738_, p_94739_)) {
                setFocusedButton(optBtn3);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.visible && this.focused) {
            if (optBtn1.isFocused()) return optBtn1.keyPressed(keyCode, scanCode, modifiers);
            if (optBtn2.isFocused()) return optBtn2.keyPressed(keyCode, scanCode, modifiers);
            if (optBtn3.isFocused()) return optBtn3.keyPressed(keyCode, scanCode, modifiers);
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
        if (!focused) {
            setFocusedButton(null);
        } else if (!optBtn1.isFocused() && !optBtn2.isFocused() && !optBtn3.isFocused()) {
            setFocusedButton(optBtn1);
        }
    }

    private void setFocusedButton(Button button) {
        optBtn1.setFocused(button == optBtn1);
        optBtn2.setFocused(button == optBtn2);
        optBtn3.setFocused(button == optBtn3);
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

    /** Keeps button interaction while the parent component renders the shared background. */
    private static final class TextOnlyButton extends Button {
        private TextOnlyButton(int x, int y, int width, int height, OnPress onPress) {
            super(x, y, width, height, Component.empty(), onPress, supplier -> supplier.get());
        }

        @Override
        protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
            Minecraft minecraft = Minecraft.getInstance();
            int textColor = getFGColor() | Mth.ceil(alpha * 255.0F) << 24;
            guiGraphics.drawCenteredString(minecraft.font, getMessage(),
                    getX() + getWidth() / 2, getY() + (getHeight() - 8) / 2,
                    textColor);
        }
    }
}
