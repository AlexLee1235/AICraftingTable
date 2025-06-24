package com.watermelon0117.aicraft.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.watermelon0117.aicraft.AICraftingTable;
import com.watermelon0117.aicraft.gpt.GPTIdeaGenerator2;
import com.watermelon0117.aicraft.init.ItemInit;
import com.watermelon0117.aicraft.network.SGenIdeaPacket;
import com.watermelon0117.aicraft.recipes.Recipe;
import com.watermelon0117.aicraft.menu.AICraftingTableMenu;
import com.watermelon0117.aicraft.network.PacketHandler;
import com.watermelon0117.aicraft.network.SSelectIdeaPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.Arrays;

public class AICraftingTableScreen extends AbstractContainerScreen<AICraftingTableMenu> {
    private static final ResourceLocation CRAFTING_TABLE_LOCATION_1 = new ResourceLocation(AICraftingTable.MODID, "textures/gui/ai_crafting_table_1.png");
    private static final ResourceLocation CRAFTING_TABLE_LOCATION_2 = new ResourceLocation(AICraftingTable.MODID, "textures/gui/ai_crafting_table_2.png");
    private Button mainBtn, optBtn1, optBtn2, optBtn3, reBtn;
    private int stage = 1;
    private Recipe currentRecipe;
    private boolean generatingText = false;
    private String errorMessage = "";

    public AICraftingTableScreen(AICraftingTableMenu p_98448_, Inventory p_98449_, Component p_98450_) {
        super(p_98448_, p_98449_, p_98450_);
    }

    private void createWidgets() {
        mainBtn = addRenderableWidget(new Button(leftPos + 67, topPos + 34, 26, 17,
                Component.literal(""), this::btnPress) {
            @Override
            public void render(PoseStack p_93657_, int p_93658_, int p_93659_, float p_93660_) {
                if (this.visible) {
                    this.isHovered = p_93658_ >= this.x && p_93659_ >= this.y && p_93658_ < this.x + this.width && p_93659_ < this.y + this.height;
                }
            }
        });
        optBtn1 = addRenderableWidget(new Button(leftPos + 98, topPos + 16, 70, 17,
                Component.literal("None"), this::optBtnPress));
        optBtn2 = addRenderableWidget(new Button(leftPos + 98, topPos + 33, 70, 17,
                Component.literal("None"), this::optBtnPress));
        optBtn3 = addRenderableWidget(new Button(leftPos + 98, topPos + 50, 70, 17,
                Component.literal("None"), this::optBtnPress));
        optBtn1.visible = optBtn2.visible = optBtn3.visible = false;
        reBtn = addWidget(new Button(leftPos + 155, topPos + 49, 6, 6,
                Component.literal(""), this::reBtnPress));
    }

    protected void init() {
        super.init();
        titleLabelX = 29;
        createWidgets();
        if (menu.hasCraftResult || menu.blockEntity.getProgress() > 0)
            setStage2();
        else
            setStage1();
        currentRecipe = new Recipe(menu);
    }

    private void reBtnPress(Button button) {
        ItemStack itemStack = menu.slots.get(0).getItem();
        if (itemStack.is(ItemInit.MAIN_ITEM.get()) || itemStack.is(ItemInit.MAIN_FOOD_ITEM.get())) {
            String s = strip(itemStack.getDisplayName().getString());
            PacketHandler.sendToServer(new SSelectIdeaPacket(menu.blockEntity.getBlockPos(), s, currentRecipe.getDisplayNames()));
            setStage2();
        } else
            System.out.println("Can't regen because no item");
    }

    private void optBtnPress(Button button) {
        String s = button.getMessage().getString();
        PacketHandler.sendToServer(new SSelectIdeaPacket(menu.blockEntity.getBlockPos(), s, currentRecipe.getDisplayNames()));
        setStage2();
    }

    public void btnPress(Button button) {
        if (currentRecipe.isEmpty())
            return;
        generatingText = true;
        errorMessage = "";
        hideOpts();
        PacketHandler.sendToServer(new SGenIdeaPacket(menu.blockEntity.getBlockPos(), currentRecipe));
    }

    public void setIdeaOpts(String[] recipe, String[] idea) {
        if (Arrays.equals(recipe, new Recipe(menu).getDisplayNames())) {
            showOpts();
            optBtn1.setMessage(Component.literal(idea[0]));
            optBtn2.setMessage(Component.literal(idea[1]));
            optBtn3.setMessage(Component.literal(idea[2]));
        } else
            System.out.println("Canceled, not putting ideas");
        generatingText = false;
    }

    public void setIdeaErr(String msg) {
        System.out.println(msg);
        generatingText = false;
        errorMessage = msg;
    }

    private void setStage2() {
        if (stage != 2) {
            //System.out.println("setStage2");
            stage = 2;
            mainBtn.visible = false;
            hideOpts();
            generatingText = false;
        }
    }

    private void setStage1() {
        if (stage != 1) {
            //System.out.println("setStage1");
            stage = 1;
            mainBtn.visible = true;
            hideOpts();
            generatingText = false;
        }
    }

    public void containerTick() {
        super.containerTick();
        if (menu.hasCraftResult || menu.blockEntity.getProgress() > 0) {
            setStage2();
        } else {
            setStage1();
        }
        if (!currentRecipe.equals(new Recipe(menu))) {
            setStage1();
            hideOpts();
            generatingText = false;
            currentRecipe = new Recipe(menu);
        }
    }

    private void showOpts() {
        optBtn1.visible = optBtn2.visible = optBtn3.visible = true;
    }

    private void hideOpts() {
        optBtn1.visible = optBtn2.visible = optBtn3.visible = false;
    }

    public void render(PoseStack p_98479_, int p_98480_, int p_98481_, float p_98482_) {
        renderBackground(p_98479_);
        if (stage == 1) {
            var slot = menu.slots.get(0);
            menu.slots.remove(0);
            super.render(p_98479_, p_98480_, p_98481_, p_98482_);
            menu.slots.add(0, slot);
        } else {
            super.render(p_98479_, p_98480_, p_98481_, p_98482_);
        }
        renderTooltip(p_98479_, p_98480_, p_98481_);
        if (generatingText)
            font.draw(p_98479_, Component.literal("Generating"), (float) leftPos + 102, (float) topPos + 20, 4210752);
        font.draw(p_98479_, Component.literal(errorMessage), (float) leftPos + 102, (float) topPos + 40, 4210752);
    }

    protected void renderBg(PoseStack p_98474_, float p_98475_, int p_98476_, int p_98477_) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        if (stage == 1)
            RenderSystem.setShaderTexture(0, CRAFTING_TABLE_LOCATION_1);
        else if (stage == 2)
            RenderSystem.setShaderTexture(0, CRAFTING_TABLE_LOCATION_2);
        int i = leftPos;
        int j = (height - imageHeight) / 2;
        blit(p_98474_, i, j, 0, 0, imageWidth, imageHeight);
        if (stage == 2)
            blit(p_98474_, i + 66, j + 34, 0, 167, menu.blockEntity.getProgress() / 10, 16);
        if (stage == 1) {
            if (generatingText) {
                blit(p_98474_, i + 67, j + 34, 0, 185, 27, 18);
            } else if (mainBtn.isHoveredOrFocused()) {
                blit(p_98474_, i + 67, j + 34, 0, 203, 27, 18);
            }
        }
    }

    private static String strip(String s) {
        return s.replace("[", "").replace("]", "");
    }
}
