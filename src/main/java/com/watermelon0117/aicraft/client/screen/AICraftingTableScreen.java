package com.watermelon0117.aicraft.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.watermelon0117.aicraft.AICraftingTable;
import com.watermelon0117.aicraft.gpt.GPTIdeaGenerator2;
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

import java.util.Arrays;

public class AICraftingTableScreen extends AbstractContainerScreen<AICraftingTableMenu> {
    private static final ResourceLocation CRAFTING_TABLE_LOCATION_1 = new ResourceLocation(AICraftingTable.MODID, "textures/gui/ai_crafting_table_1.png");
    private static final ResourceLocation CRAFTING_TABLE_LOCATION_2 = new ResourceLocation(AICraftingTable.MODID, "textures/gui/ai_crafting_table_2.png");
    private Button button, optBtn1, optBtn2, optBtn3;
    private int stage = 1;
    private Recipe currentRecipe;
    private boolean generatingText = false;
    private String errorMessage="";

    public AICraftingTableScreen(AICraftingTableMenu p_98448_, Inventory p_98449_, Component p_98450_) {
        super(p_98448_, p_98449_, p_98450_);
    }

    protected void init() {
        super.init();
        this.titleLabelX = 29;
        this.button = addRenderableWidget(new Button(leftPos + 67, topPos + 34, 26, 17,
                Component.literal(""), this::btnPress) {
            @Override
            public void render(PoseStack p_93657_, int p_93658_, int p_93659_, float p_93660_) {
                if (this.visible) {
                    this.isHovered = p_93658_ >= this.x && p_93659_ >= this.y && p_93658_ < this.x + this.width && p_93659_ < this.y + this.height;
                }
            }
        });
        this.optBtn1 = addRenderableWidget(new Button(leftPos + 98, topPos + 16, 70, 17,
                Component.literal("None"), this::optBtnPress));
        this.optBtn2 = addRenderableWidget(new Button(leftPos + 98, topPos + 33, 70, 17,
                Component.literal("None"), this::optBtnPress));
        this.optBtn3 = addRenderableWidget(new Button(leftPos + 98, topPos + 50, 70, 17,
                Component.literal("None"), this::optBtnPress));
        optBtn1.visible = optBtn2.visible = optBtn3.visible = false;
        if (menu.hasCraftResult)
            setStage2();
        else {
            if (menu.blockEntity.getProgress() > 0)
                setStage2();
            else
                setStage1();
        }
        currentRecipe = new Recipe(menu);
    }

    private void optBtnPress(Button button) {
        String s = button.getMessage().getString();
        PacketHandler.sendToServer(new SSelectIdeaPacket(this.menu.blockEntity.getBlockPos(), s, currentRecipe.getDisplayNames()));
        setStage2();
    }

    public void btnPress(Button button) {
        if(currentRecipe.isEmpty())
            return;
        generatingText = true;
        errorMessage = "";
        optBtn1.visible = optBtn2.visible = optBtn3.visible = false;
        System.out.println("sending packet");
        PacketHandler.sendToServer(new SGenIdeaPacket(this.menu.blockEntity.getBlockPos(), currentRecipe));
    }
    public void setIdeaOpts(String[] recipe, String[] idea) {
        System.out.println("setIdeaOpts");
        System.out.println(Arrays.toString(recipe));
        System.out.println(Arrays.toString(new Recipe(menu).getDisplayNames()));
        if (Arrays.equals(recipe, new Recipe(menu).getDisplayNames())) {
            optBtn1.visible = optBtn2.visible = optBtn3.visible = true;
            optBtn1.setMessage(Component.literal(idea[0]));
            optBtn2.setMessage(Component.literal(idea[1]));
            optBtn3.setMessage(Component.literal(idea[2]));
        } else
            System.out.println("Canceled, not putting ideas");
        generatingText = false;
    }
    public void setIdeaErr(String msg){
        System.out.println("setIdeaErr");
        System.out.println(msg);
        generatingText = false;
        errorMessage = msg;
    }
    private void setStage2() {
        if (stage != 2) {
            //System.out.println("setStage2");
            stage = 2;
            this.button.visible = false;
            this.optBtn1.visible = false;
            this.optBtn2.visible = false;
            this.optBtn3.visible = false;
            generatingText = false;
        }
    }
    private void setStage1() {
        if (stage != 1) {
            //System.out.println("setStage1");
            stage = 1;
            this.button.visible = true;
            this.optBtn1.visible = false;
            this.optBtn2.visible = false;
            this.optBtn3.visible = false;
            generatingText = false;
        }
    }
    public void containerTick() {
        super.containerTick();
        if (menu.hasCraftResult) {
            setStage2();
        } else {
            if (menu.blockEntity.getProgress() > 0)
                setStage2();
            else
                setStage1();
        }
        if (!currentRecipe.equals(new Recipe(menu))) {
            setStage1();
            this.optBtn1.visible = false;
            this.optBtn2.visible = false;
            this.optBtn3.visible = false;
            generatingText = false;
            currentRecipe = new Recipe(menu);
        }
    }
    public void render(PoseStack p_98479_, int p_98480_, int p_98481_, float p_98482_) {
        this.renderBackground(p_98479_);
        if (stage == 1) {
            var slot = this.menu.slots.get(0);
            this.menu.slots.remove(0);
            super.render(p_98479_, p_98480_, p_98481_, p_98482_);
            this.menu.slots.add(0, slot);
        } else {
            super.render(p_98479_, p_98480_, p_98481_, p_98482_);
        }
        this.renderTooltip(p_98479_, p_98480_, p_98481_);
        if (generatingText)
            this.font.draw(p_98479_, Component.literal("Generating"), (float) this.leftPos + 102, (float) this.topPos + 20, 4210752);
        this.font.draw(p_98479_, Component.literal(errorMessage), (float) this.leftPos + 102, (float) this.topPos + 40, 4210752);
    }
    protected void renderBg(PoseStack p_98474_, float p_98475_, int p_98476_, int p_98477_) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        if (stage == 1)
            RenderSystem.setShaderTexture(0, CRAFTING_TABLE_LOCATION_1);
        else if (stage == 2)
            RenderSystem.setShaderTexture(0, CRAFTING_TABLE_LOCATION_2);
        int i = this.leftPos;
        int j = (this.height - this.imageHeight) / 2;
        this.blit(p_98474_, i, j, 0, 0, this.imageWidth, this.imageHeight);
        if (stage == 2)
            this.blit(p_98474_, i + 66, j + 34, 0, 167, this.menu.blockEntity.getProgress() / 10, 16);
        if (stage == 1) {
            if (this.generatingText) {
                this.blit(p_98474_, i + 67, j + 34, 0, 185, 27, 18);
            } else if (this.button.isHoveredOrFocused()) {
                this.blit(p_98474_, i + 67, j + 34, 0, 203, 27, 18);
            }
        }
    }

    @Override
    protected void slotClicked(Slot p_97778_, int p_97779_, int p_97780_, ClickType p_97781_) {
        super.slotClicked(p_97778_,p_97779_,p_97780_,p_97781_);
    }
}
