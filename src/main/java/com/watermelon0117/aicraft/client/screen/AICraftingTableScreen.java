package com.watermelon0117.aicraft.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.watermelon0117.aicraft.AICraftingTable;
import com.watermelon0117.aicraft.gpt.ItemIdeas;
import com.watermelon0117.aicraft.init.ItemInit;
import com.watermelon0117.aicraft.items.MainItem;
import com.watermelon0117.aicraft.network.SGenIdeaPacket;
import com.watermelon0117.aicraft.recipes.Recipe;
import com.watermelon0117.aicraft.menu.AICraftingTableMenu;
import com.watermelon0117.aicraft.network.PacketHandler;
import com.watermelon0117.aicraft.network.SSelectIdeaPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.resources.language.LanguageInfo;
import net.minecraft.client.resources.language.LanguageManager;
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
    private static final ResourceLocation RECIPE_BUTTON_LOCATION = new ResourceLocation("textures/gui/recipe_button.png");
    private Button mainBtn, reBtn;
    private ImageButton bookBtn;
    public CustomRecipeBookComponent recipeBookComponent = new CustomRecipeBookComponent();
    private OptionsComponent options;
    private enum State{
        INITIAL,
        GENERATING,
        GENERATED,
        PROGRESS
    }
    private State state;
    private Recipe currentRecipe;
    private ItemIdeas itemIdeas;
    private String errorMessage = "";
    private static int session=0;

    public AICraftingTableScreen(AICraftingTableMenu p_98448_, Inventory p_98449_, Component p_98450_) {
        super(p_98448_, p_98449_, p_98450_);
        session++;
    }

    private void createWidgets() {
        mainBtn = addRenderableWidget(new Button(leftPos + 67, topPos + 34, 26, 17,
                Component.empty(), this::btnPress) {
            @Override
            public void render(PoseStack p_93657_, int p_93658_, int p_93659_, float p_93660_) {
                if (this.visible)
                    this.isHovered = p_93658_ >= this.x && p_93659_ >= this.y && p_93658_ < this.x + this.width && p_93659_ < this.y + this.height;
            }
        });
        reBtn=addWidget(new Button(leftPos + 155, topPos + 49, 6, 6,
                Component.empty(), this::reBtnPress));
    }

    protected void init() {
        super.init();
        titleLabelX = 29;
        createWidgets();
        this.recipeBookComponent.init(this.width, this.height, this.minecraft, menu);
        options = addRenderableWidget(new OptionsComponent());
        options.init(leftPos, topPos, this::optBtnPress);
        bookBtn = this.addRenderableWidget(new ImageButton(this.leftPos + 70, this.topPos + 56, 20, 18, 0, 0, 19, RECIPE_BUTTON_LOCATION, (p_98484_) -> {
            this.recipeBookComponent.toggleVisibility();
            this.leftPos = this.recipeBookComponent.updateScreenPosition(this.width, this.imageWidth);
            updateWidgetPos();
        }));
        this.addWidget(this.recipeBookComponent);
        this.setInitialFocus(this.recipeBookComponent);
        if (menu.hasCraftResult || menu.blockEntity.getProgress() > 0)
            setProgress();
        else
            setInitial();
        currentRecipe = new Recipe(menu);
    }
    private void updateWidgetPos() {
        bookBtn.setPosition(this.leftPos + 70, this.topPos + 56);
        options.updateWidgetPos(leftPos, topPos);
        mainBtn.x = leftPos + 67;
        mainBtn.y = topPos + 34;
        reBtn.x = leftPos + 155;
        reBtn.y = topPos + 49;
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
            throw new IllegalStateException("setGenerating");
        options.visible = false;
        errorMessage = "";
        state = State.GENERATING;
    }
    private void setGenerated(ItemIdeas idea) {
        if (state != State.GENERATING)
            throw new IllegalStateException("setGenerating");
        options.visible = true;
        options.setMessage(idea.names);
        this.itemIdeas=idea;
        state = State.GENERATED;
    }

    private void reBtnPress(Button button) {
        ItemStack itemStack = menu.slots.get(0).getItem();
        if (MainItem.isMainItem(itemStack)) {
            String id = MainItem.getID(itemStack);
            String name = ItemInit.MAIN_ITEM.get().getName(itemStack).getString();
            PacketHandler.sendToServer(new SSelectIdeaPacket(menu.blockEntity.getBlockPos(), id, name, currentRecipe.items,true));
            setProgress();
        } else
            System.out.println("Can't regen because no item");
    }

    private void optBtnPress(Button button, int i) {
        if (state == State.GENERATED) {
            String id=itemIdeas.id[i];
            String name=itemIdeas.names[i];
            PacketHandler.sendToServer(new SSelectIdeaPacket(menu.blockEntity.getBlockPos(), id, name, currentRecipe.items,false));
            setProgress();
        }
    }

    public void btnPress(Button button) {
        if (state == State.INITIAL || state == State.GENERATED) {
            if (currentRecipe.isEmpty())
                return;
            setGenerating();
            PacketHandler.sendToServer(new SGenIdeaPacket(menu.blockEntity.getBlockPos(), currentRecipe, getLanguage(), session));
        }
    }
    public boolean mouseClicked(double p_98452_, double p_98453_, int p_98454_) {
        if (this.recipeBookComponent.mouseClicked(p_98452_, p_98453_, p_98454_)) {
            this.setFocused(this.recipeBookComponent);
            return true;
        } else {
            return super.mouseClicked(p_98452_, p_98453_, p_98454_);
        }
    }
    public void handleIdeaPacket(String[] recipe, ItemIdeas idea, boolean err, String errMsg) {
        if (state == State.GENERATING) {
            if (err) {
                errorMessage = errMsg;
                setInitial();
            } else {
                if (Arrays.equals(recipe, new Recipe(menu).getDisplayNames())) {
                    setGenerated(idea);
                } else
                    System.out.println("Canceled, not putting ideas");
            }
        }
    }

    public void containerTick() {
        super.containerTick();
        if (!currentRecipe.equals(new Recipe(menu))) {
            if (menu.hasCraftResult)
                setProgress();
            else
                setInitial();
            currentRecipe = new Recipe(menu);
        }
    }

    public void render(PoseStack p_98479_, int p_98480_, int p_98481_, float p_98482_) {
        renderBackground(p_98479_);
        if (state != State.PROGRESS) {
            var slot = menu.slots.get(0);
            menu.slots.remove(0);
            this.recipeBookComponent.render(p_98479_, p_98480_, p_98481_, p_98482_);
            super.render(p_98479_, p_98480_, p_98481_, p_98482_);
            this.recipeBookComponent.renderGhostRecipe(p_98479_, this.leftPos, this.topPos, true, p_98482_);
            menu.slots.add(0, slot);
        } else {
            this.recipeBookComponent.render(p_98479_, p_98480_, p_98481_, p_98482_);
            super.render(p_98479_, p_98480_, p_98481_, p_98482_);
            this.recipeBookComponent.renderGhostRecipe(p_98479_, this.leftPos, this.topPos, true, p_98482_);
        }
        renderTooltip(p_98479_, p_98480_, p_98481_);
        this.recipeBookComponent.renderTooltip(p_98479_, this.leftPos, this.topPos, p_98480_, p_98481_);
        if (state == State.GENERATING)
            font.draw(p_98479_, Component.literal("Generating"), (float) leftPos + 102, (float) topPos + 20, 4210752);
        font.draw(p_98479_, Component.literal(errorMessage), (float) leftPos + 102, (float) topPos + 40, 4210752);
    }

    protected void renderBg(PoseStack p_98474_, float p_98475_, int p_98476_, int p_98477_) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        if (state == State.INITIAL || state == State.GENERATING || state == State.GENERATED)
            RenderSystem.setShaderTexture(0, CRAFTING_TABLE_LOCATION_1);
        else if (state == State.PROGRESS)
            RenderSystem.setShaderTexture(0, CRAFTING_TABLE_LOCATION_2);
        int i = leftPos;
        int j = (height - imageHeight) / 2;
        blit(p_98474_, i, j, 0, 0, imageWidth, imageHeight);
        if (state == State.PROGRESS)  //draw progress bar
            blit(p_98474_, i + 66, j + 34, 0, 167, menu.blockEntity.getProgress() / 10, 16);

        if (state == State.GENERATING) {  //locked button
            blit(p_98474_, i + 67, j + 34, 0, 185, 27, 18);
        } else if (mainBtn.isHoveredOrFocused()) {
            blit(p_98474_, i + 67, j + 34, 0, 203, 27, 18);
        }
    }

    @Override
    protected void slotClicked(Slot p_97778_, int p_97779_, int p_97780_, ClickType p_97781_) {
        super.slotClicked(p_97778_, p_97779_, p_97780_, p_97781_);
        this.recipeBookComponent.slotClicked(p_97778_);
    }
    protected boolean hasClickedOutside(double p_98456_, double p_98457_, int p_98458_, int p_98459_, int p_98460_) {
        boolean flag = p_98456_ < (double)p_98458_ || p_98457_ < (double)p_98459_ || p_98456_ >= (double)(p_98458_ + this.imageWidth) || p_98457_ >= (double)(p_98459_ + this.imageHeight);
        return this.recipeBookComponent.hasClickedOutside(p_98456_, p_98457_, this.leftPos, this.topPos, this.imageWidth, this.imageHeight, p_98460_) && flag;
    }
    private static String getLanguage(){
        LanguageManager langManager = Minecraft.getInstance().getLanguageManager();
        LanguageInfo currentLang = langManager.getSelected();
        return currentLang.getCode(); // returns like "en_us"
    }
}
