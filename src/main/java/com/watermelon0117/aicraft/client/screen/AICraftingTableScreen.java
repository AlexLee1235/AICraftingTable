package com.watermelon0117.aicraft.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.watermelon0117.aicraft.AICraftingTable;
import com.watermelon0117.aicraft.gpt.GPTItemGenerator;
import com.watermelon0117.aicraft.gpt.GPTItemGenerator2;
import com.watermelon0117.aicraft.gpt.GPTItemGenerator3;
import com.watermelon0117.aicraft.menu.Recipe;
import com.watermelon0117.aicraft.menu.AICraftingTableMenu;
import com.watermelon0117.aicraft.menu.RecipeShapeMatcher;
import com.watermelon0117.aicraft.network.PacketHandler;
import com.watermelon0117.aicraft.network.SGUISelectItemButtonPressedPacket;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

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
    }

    private void optBtnPress(Button button) {
        String s = button.getMessage().getString();
        PacketHandler.sendToServer(new SGUISelectItemButtonPressedPacket(this.menu.blockEntity.getBlockPos(), s));
        setStage2();
    }

    public void btnPress(Button button) {
        GPTItemGenerator3 generator = new GPTItemGenerator3();
        currentRecipe = new Recipe(menu);
        if(currentRecipe.isEmpty())
            return;
        generatingText = true;
        errorMessage = "";
        optBtn1.visible = optBtn2.visible = optBtn3.visible = false;
        /*
        generator.generate(currentRecipe).thenAccept(results -> {
            Recipe recipe2 = new Recipe(menu);
            if (currentRecipe.equals(recipe2)) {
                optBtn1.visible = optBtn2.visible = optBtn3.visible = true;
                optBtn1.setMessage(Component.literal(results[0]));
                optBtn2.setMessage(Component.literal(results[1]));
                optBtn3.setMessage(Component.literal(results[2]));
            } else
                System.out.println("Canceled");
            generatingText = false;
        }).exceptionally(e -> {
            e.printStackTrace();
            generatingText = false;
            errorMessage = "Error";
            return null;
        });*/
        if(RecipeShapeMatcher.matchesAnyToolOrArmorShape(currentRecipe.items)){
            optBtn1.visible = optBtn2.visible = optBtn3.visible = true;
            optBtn1.setMessage(Component.literal("hi"));
            optBtn2.setMessage(Component.literal(""));
            optBtn3.setMessage(Component.literal(""));
        }
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
            currentRecipe = null;
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
            currentRecipe = null;
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
        if (currentRecipe != null) {
            if (!currentRecipe.equals(new Recipe(menu))) {
                setStage1();
                this.optBtn1.visible = false;
                this.optBtn2.visible = false;
                this.optBtn3.visible = false;
                generatingText = false;
                currentRecipe = null;
            }
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
}
