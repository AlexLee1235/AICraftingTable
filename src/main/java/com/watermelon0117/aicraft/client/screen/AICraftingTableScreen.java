package com.watermelon0117.aicraft.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.watermelon0117.aicraft.AICraftingTable;
import com.watermelon0117.aicraft.gpt.ItemIdeas;
import com.watermelon0117.aicraft.init.ItemInit;
import com.watermelon0117.aicraft.items.MainItem;
import com.watermelon0117.aicraft.network.SGenIdeaPacket;
import com.watermelon0117.aicraft.common.ItemStackArray;
import com.watermelon0117.aicraft.menu.AICraftingTableMenu;
import com.watermelon0117.aicraft.network.PacketHandler;
import com.watermelon0117.aicraft.network.SSelectIdeaPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.resources.language.LanguageManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class AICraftingTableScreen extends AbstractContainerScreen<AICraftingTableMenu> {
    private static final ResourceLocation CRAFTING_TABLE_LOCATION_1 = new ResourceLocation(AICraftingTable.MODID, "textures/gui/ai_crafting_table_1.png");
    private static final ResourceLocation CRAFTING_TABLE_LOCATION_2 = new ResourceLocation(AICraftingTable.MODID, "textures/gui/ai_crafting_table_2.png");
    private static final ResourceLocation RECIPE_BUTTON_LOCATION = new ResourceLocation("textures/gui/recipe_button.png");

    private enum State {INITIAL, GENERATING, GENERATED, PROGRESS}

    private State state;
    private ItemStackArray currentRecipe;
    private ItemIdeas itemIdeas;
    private boolean error = false;
    private boolean first = true;

    private Button mainBtn, reBtn;
    private ImageButton bookBtn;
    private OptionsComponent options;
    public final CustomRecipeBookComponent recipeBookComponent = new CustomRecipeBookComponent();

    public AICraftingTableScreen(AICraftingTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void init() {
        super.init();
        titleLabelX = 29;
        mainBtn = addRenderableWidget(new InvisibleButton(leftPos + 67, topPos + 34, 26, 17, this::btnPress));
        reBtn = addRenderableWidget(new InvisibleButton(leftPos + 155, topPos + 49, 6, 6, this::reBtnPress));

        recipeBookComponent.init(width, height, minecraft, menu);
        options = addRenderableWidget(new OptionsComponent());
        options.init(leftPos, topPos, this::optBtnPress);

        bookBtn = addRenderableWidget(new ImageButton(leftPos + 70, topPos + 56, 20, 18, 0, 0, 19, RECIPE_BUTTON_LOCATION, btn -> {
            recipeBookComponent.toggleVisibility();
            leftPos = recipeBookComponent.updateScreenPosition(width, imageWidth);
            updateWidgetPos();
        }));

        addWidget(recipeBookComponent);
        setInitialFocus(recipeBookComponent);

        if (first) {
            if (menu.hasCraftResult || menu.blockEntity.getProgress() > 0) setProgress();
            else setInitial();
            currentRecipe = new ItemStackArray(menu);
            first = false;
        }
    }

    private void updateWidgetPos() {
        bookBtn.setPosition(leftPos + 70, topPos + 56);
        options.updateWidgetPos(leftPos, topPos);
        mainBtn.setPosition(leftPos + 67, topPos + 34);
        reBtn.setPosition(leftPos + 155, topPos + 49);
    }

    private void setInitial() {
        mainBtn.visible = true;
        options.visible = false;
        state = State.INITIAL;
    }

    private void setProgress() {
        mainBtn.visible = false;
        options.visible = false;
        state = State.PROGRESS;
    }

    private void setGenerating() {
        if (state != State.INITIAL && state != State.GENERATING && state != State.GENERATED)
            throw new IllegalStateException();
        options.visible = false;
        error = false;
        state = State.GENERATING;
    }

    private void setGenerated(ItemIdeas idea) {
        if (state != State.GENERATING)
            throw new IllegalStateException();
        options.setMessage(idea.names);
        this.itemIdeas = idea;
        state = State.GENERATED;
    }

    private void reBtnPress(Button button) {
        ItemStack itemStack = menu.slots.get(0).getItem();
        if (MainItem.isMainItem(itemStack)) {
            String id = MainItem.getID(itemStack);
            String name = ItemInit.MAIN_ITEM.get().getName(itemStack).getString();
            PacketHandler.sendToServer(new SSelectIdeaPacket(menu.blockEntity.getBlockPos(), id, name, currentRecipe.items, true));
            setProgress();
            // send feedback
        } else
            System.out.println("Can't regen because no item");
    }

    private void optBtnPress(Button button, int i) {
        if (state == State.GENERATED) {
            String id = itemIdeas.id[i];
            String name = itemIdeas.names[i];
            PacketHandler.sendToServer(new SSelectIdeaPacket(menu.blockEntity.getBlockPos(), id, name, currentRecipe.items, false));
            setProgress();
        }
    }

    private void btnPress(Button button) {
        if (state == State.INITIAL || state == State.GENERATED) {
            if (currentRecipe.isEmpty()) return;
            setGenerating();
            PacketHandler.sendToServer(new SGenIdeaPacket(menu.blockEntity.getBlockPos(), currentRecipe, getLanguage()));
        }
    }

    public void handleIdeaPacket(ItemStack[] recipe, ItemIdeas idea, boolean err, String errMsg) {
        if (state == State.GENERATING) {
            if (err) {
                Minecraft.getInstance().player.sendSystemMessage(Component.literal("An error occurred when using the AI crafting table.").withStyle(ChatFormatting.RED));
                Minecraft.getInstance().player.sendSystemMessage(Component.literal(errMsg).withStyle(ChatFormatting.RED));
                error = true;
                setInitial();
            } else if (new ItemStackArray(menu).equals(new ItemStackArray(recipe))) {
                setGenerated(idea);
            } else {
                System.out.println("Canceled, not putting ideas");
            }
        }
    }

    @Override
    public void containerTick() {
        super.containerTick();
        if (!currentRecipe.equals(new ItemStackArray(menu))) {
            if (menu.hasCraftResult) setProgress();
            else setInitial();
            currentRecipe = new ItemStackArray(menu);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        renderBackground(guiGraphics);
        if (state != State.PROGRESS) {
            Slot slot = menu.slots.get(0);
            menu.slots.remove(0);
            super.render(guiGraphics, mouseX, mouseY, partialTicks);
            menu.slots.add(0, slot);
        } else
            super.render(guiGraphics, mouseX, mouseY, partialTicks);

        recipeBookComponent.render(guiGraphics, mouseX, mouseY, partialTicks);
        recipeBookComponent.renderGhostRecipe(guiGraphics, leftPos, topPos, true, partialTicks);
        renderTooltip(guiGraphics, mouseX, mouseY);
        recipeBookComponent.renderTooltip(guiGraphics, leftPos, topPos, mouseX, mouseY);

        if (state == State.GENERATING)
            guiGraphics.drawString(this.font, Component.literal("Generating"), leftPos + 102, topPos + 20, 4210752, false);
        if (error)
            guiGraphics.drawString(this.font, Component.literal("Error"), leftPos + 102, topPos + 20, 4210752, false);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        ResourceLocation texture = CRAFTING_TABLE_LOCATION_1;
        if (state == State.INITIAL || state == State.GENERATING || state == State.GENERATED)
            texture = CRAFTING_TABLE_LOCATION_1;
        else if (state == State.PROGRESS)
            texture = CRAFTING_TABLE_LOCATION_2;

        int i = leftPos;
        int j = (height - imageHeight) / 2;
        guiGraphics.blit(texture, i, j, 0, 0, imageWidth, imageHeight);

        if (state == State.PROGRESS)  //progress arrow
            guiGraphics.blit(texture, i + 66, j + 34, 0, 167, menu.blockEntity.getProgress() / 10, 16);
        if (state == State.GENERATING)   //locked button
            guiGraphics.blit(texture, i + 67, j + 34, 0, 185, 27, 18);
        else if (mainBtn.isHoveredOrFocused())   //hovered button
            guiGraphics.blit(texture, i + 67, j + 34, 0, 203, 27, 18);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (recipeBookComponent.mouseClicked(mouseX, mouseY, button)) {
            setFocused(recipeBookComponent);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void resize(Minecraft minecraft, int width, int height) {
        String[] msg = options.visible ? options.idea : null;
        super.init(minecraft, width, height);
        leftPos = recipeBookComponent.updateScreenPosition(this.width, imageWidth);
        updateWidgetPos();
        if (msg != null) options.setMessage(msg);
    }

    @Override
    protected void slotClicked(Slot slot, int slotId, int mouseButton, ClickType type) {
        super.slotClicked(slot, slotId, mouseButton, type);
        recipeBookComponent.slotClicked(slot);
    }

    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int guiLeft, int guiTop, int button) {
        boolean outside = mouseX < guiLeft || mouseY < guiTop || mouseX >= guiLeft + imageWidth || mouseY >= guiTop + imageHeight;
        return recipeBookComponent.hasClickedOutside(mouseX, mouseY, leftPos, topPos, imageWidth, imageHeight, button) && outside;
    }

    private static String getLanguage() {
        LanguageManager langManager = Minecraft.getInstance().getLanguageManager();
        return langManager.getSelected(); // e.g. "en_us"
    }

    private static class InvisibleButton extends Button {
        protected InvisibleButton(int x, int y, int width, int height, OnPress onPress) {
            super(x, y, width, height, Component.empty(), onPress, supplier -> supplier.get());
        }

        @Override
        protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        }
    }
}
