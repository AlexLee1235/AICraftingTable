package com.watermelon0117.aicraft.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.watermelon0117.aicraft.AICraftingTable;
import com.watermelon0117.aicraft.GPTItemGenerator;
import com.watermelon0117.aicraft.menu.AICraftingTableMenu;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.EnchantmentScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;

import java.io.IOException;
import java.util.List;

public class AICraftingTableScreen extends AbstractContainerScreen<AICraftingTableMenu> {
    private static final ResourceLocation CRAFTING_TABLE_LOCATION = new ResourceLocation(AICraftingTable.MODID, "textures/gui/ai_crafting_table.png");
    private Button button;
    private String test="hi";
    public AICraftingTableScreen(AICraftingTableMenu p_98448_, Inventory p_98449_, Component p_98450_) {
        super(p_98448_, p_98449_, p_98450_);
    }

    protected void init() {
        super.init();
        this.titleLabelX = 29;
        this.button=addRenderableWidget(new Button(leftPos+65,topPos+37,15,11,
                Component.literal("hi"),this::btnPress));
    }

    public void btnPress(Button button){
        System.out.println("hi");
        test="bye";
        /*GPTItemGenerator generator=new GPTItemGenerator();
        try {
            generator.generate(new String[]{"Iron Ingot", "Iron Ingot", "Iron Ingot",
                    "Iron Ingot", "Stick", "Iron Ingot",
                    "empty", "Stick", "empty"});
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }*/
    }

    public void containerTick() {
        super.containerTick();
    }

    public void render(PoseStack p_98479_, int p_98480_, int p_98481_, float p_98482_) {
        this.renderBackground(p_98479_);
        super.render(p_98479_, p_98480_, p_98481_, p_98482_);
        this.renderTooltip(p_98479_, p_98480_, p_98481_);
        this.font.draw(p_98479_, Component.literal(test), (float)this.leftPos, (float)(this.topPos+40), 4210752);

    }

    protected void renderBg(PoseStack p_98474_, float p_98475_, int p_98476_, int p_98477_) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, CRAFTING_TABLE_LOCATION);
        int i = this.leftPos;
        int j = (this.height - this.imageHeight) / 2;
        this.blit(p_98474_, i, j, 0, 0, this.imageWidth, this.imageHeight);
    }

    protected boolean isHovering(int p_98462_, int p_98463_, int p_98464_, int p_98465_, double p_98466_, double p_98467_) {
        return super.isHovering(p_98462_, p_98463_, p_98464_, p_98465_, p_98466_, p_98467_);
    }

    public boolean mouseClicked(double p_98452_, double p_98453_, int p_98454_) {
        return super.mouseClicked(p_98452_, p_98453_, p_98454_);
    }

    protected boolean hasClickedOutside(double p_98456_, double p_98457_, int p_98458_, int p_98459_, int p_98460_) {
        boolean flag = p_98456_ < (double)p_98458_ || p_98457_ < (double)p_98459_ || p_98456_ >= (double)(p_98458_ + this.imageWidth) || p_98457_ >= (double)(p_98459_ + this.imageHeight);
        return true;
    }

    protected void slotClicked(Slot p_98469_, int p_98470_, int p_98471_, ClickType p_98472_) {
        super.slotClicked(p_98469_, p_98470_, p_98471_, p_98472_);
    }



    public void removed() {
        super.removed();
    }

}
