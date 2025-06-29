package com.watermelon0117.aicraft.menu;

import com.watermelon0117.aicraft.init.BlockInit;
import com.watermelon0117.aicraft.items.MainItem;
import com.watermelon0117.aicraft.recipes.Recipe;
import com.watermelon0117.aicraft.common.RecipeManager;
import com.watermelon0117.aicraft.blockentities.AICraftingTableBlockEntity;
import com.watermelon0117.aicraft.init.MenuInit;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.RepairItemRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.*;

public class AICraftingTableMenu extends AbstractContainerMenu {
    public static final int RESULT_SLOT = 0;
    private static final int CRAFT_SLOT_START = 1;
    private static final int CRAFT_SLOT_END = 10;
    private static final int INV_SLOT_START = 10;
    private static final int INV_SLOT_END = 37;
    private static final int USE_ROW_SLOT_START = 37;
    private static final int USE_ROW_SLOT_END = 46;
    private final ContainerLevelAccess access;
    private final Player player;
    public final AICraftingTableBlockEntity blockEntity;
    public boolean hasCraftResult;

    public Recipe currentRecipe;
    //Client Constructor
    public AICraftingTableMenu(int id, Inventory inventory, FriendlyByteBuf buf){
        this(id, inventory, inventory.player.level.getBlockEntity(buf.readBlockPos()));
    }
    //Server Constructor
    public AICraftingTableMenu(int id, Inventory inventory, BlockEntity blockEntity) {
        super(MenuInit.MAIN_MENU.get(), id);
        if (blockEntity instanceof AICraftingTableBlockEntity be) {
            this.blockEntity = be;
        } else {
            throw new IllegalStateException("AICraftingTableMenu be");
        }

        this.access = ContainerLevelAccess.create(be.getLevel(), be.getBlockPos());
        this.player = inventory.player;

        be.getOptional().ifPresent(itemStackHandler -> {
            this.addSlot(new ResultSlotItemHandler(this, player, itemStackHandler, 0, 132, 35));
            for (int i = 0; i < 3; ++i) {
                for (int j = 0; j < 3; ++j) {
                    this.addSlot(new CraftingSlotItemHandler(this, itemStackHandler, j + i * 3 + 1, 9 + j * 18, 17 + i * 18));
                }
            }
        });


        for (int k = 0; k < 3; ++k) {
            for (int i1 = 0; i1 < 9; ++i1) {
                this.addSlot(new Slot(inventory, i1 + k * 9 + 9, 8 + i1 * 18, 84 + k * 18));
            }
        }

        for (int l = 0; l < 9; ++l) {
            this.addSlot(new Slot(inventory, l, 8 + l * 18, 142));
        }
        this.hasCraftResult = !this.getSlot(0).getItem().isEmpty();
        currentRecipe=new Recipe(this);
    }

    private ItemStack delegateQuickMoveStack(Player player, int slotId){
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotId);
        if (slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();
            if (slotId == 0) {  //shift take result
                this.access.execute((p_39378_, p_39379_) -> {
                    itemstack1.getItem().onCraftedBy(itemstack1, p_39378_, player);
                });
                if (!this.moveItemStackTo(itemstack1, 10, 46, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickCraft(itemstack1, itemstack);
            } else if (slotId >= 10 && slotId < 46) {  //if click in inventory and hot bar
                if (!this.moveItemStackTo(itemstack1, 1, 10, false)) {
                    if (slotId < 37) {  //swap between inventory and hot bar
                        if (!this.moveItemStackTo(itemstack1, 37, 46, false)) {
                            return ItemStack.EMPTY;
                        }
                    } else if (!this.moveItemStackTo(itemstack1, 10, 37, false)) {
                        return ItemStack.EMPTY;
                    }
                }
            } else if (!this.moveItemStackTo(itemstack1, 10, 46, false)) {
                //click in material, if inv full return
                return ItemStack.EMPTY;
            }

            if (itemstack1.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (itemstack1.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, itemstack);
            if (slotId == 0) {
                player.drop(itemstack1, false);
            }
        }
        //System.out.println("return"+itemstack.getDisplayName());
        return itemstack;
    }
    public ItemStack quickMoveStack(Player player, int slotId) {
        Recipe recipe=new Recipe(currentRecipe);
        ItemStack itemStack=delegateQuickMoveStack(player,slotId);
        handleInterrupt(recipe,this,player.level,player);
        return itemStack;
    }

    @Override
    public boolean stillValid(Player p_38874_) {
        return stillValid(access, p_38874_, BlockInit.AI_CRAFTING_TABLE.get());
    }

    private static CraftingContainer getDummyContainer(AICraftingTableMenu menu){
        CraftingContainer craftingContainer = new CraftingContainer(menu, 3,3);
        for (int i = 0; i < 9; i++) {
            craftingContainer.setItem(i, menu.blockEntity.getInventory().getStackInSlot(i + 1));
        }
        return craftingContainer;
    }
    public static ItemStack callRecipeManager(AICraftingTableMenu menu, Level level){
        CraftingContainer container=getDummyContainer(menu);
        ItemStack itemstack;
        Optional<CraftingRecipe> optional = level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, container, level);
        if (optional.isPresent()) {
            CraftingRecipe craftingrecipe = optional.get();
            itemstack = craftingrecipe.assemble(container);
            if(craftingrecipe instanceof RepairItemRecipe && MainItem.isMainItem(itemstack))
                return ItemStack.EMPTY;
        }else {
            Recipe recipe=new Recipe(menu);
            itemstack=RecipeManager.match(recipe.items);
        }
        return itemstack;
    }
    protected static void slotChangedCraftingGrid(AICraftingTableMenu menu, Level level, Player player) {
        if (!level.isClientSide) {
            ServerPlayer serverplayer = (ServerPlayer)player;
            ItemStack itemstack = callRecipeManager(menu, level);

            menu.blockEntity.getInventory().setStackInSlot(0,itemstack);
            menu.setRemoteSlot(0, itemstack);
            serverplayer.connection.send(new ClientboundContainerSetSlotPacket(menu.containerId, menu.incrementStateId(), 0, itemstack));
        }
    }
    private void handleInterrupt(Recipe currentRecipe, AbstractContainerMenu menu, Level level, Player player) {
        if (!level.isClientSide) {
            ServerPlayer serverplayer = (ServerPlayer) player;
            if (this.blockEntity.getProgress() > 0) {
                if (!currentRecipe.equals(new Recipe(this))) {
                    System.out.println("stop by handleInterrupt");
                    this.blockEntity.setProgress(0);
                    this.blockEntity.getInventory().setStackInSlot(0, ItemStack.EMPTY);
                    menu.setRemoteSlot(0, ItemStack.EMPTY);
                    serverplayer.connection.send(new ClientboundContainerSetSlotPacket(menu.containerId, menu.incrementStateId(), 0, ItemStack.EMPTY));
                }
            }
        }
    }

    public void slotsChanged() {
        this.access.execute((level, pos) -> {
            handleInterrupt(currentRecipe, this, level, this.player);
            slotChangedCraftingGrid(this, level, this.player);
            currentRecipe=new Recipe(this);
        });
    }
    public boolean canCraft(ItemStack itemStack) {
        List<ItemStack[]> recipes = RecipeManager.getRecipesForItem(itemStack);
        for (var recipe : recipes)
            if (canCraftRecipe(recipe))
                return true;
        return false;
    }
    public boolean canCraftRecipe(ItemStack[] recipe)
    {
        if (recipe == null || recipe.length != 9)
            throw new IllegalArgumentException("recipe must contain exactly 9 entries");

        /* ------------------------------------------------------------------
         * 1.  Tally how many of each ingredient the recipe needs.
         *     We key the map with a one-item copy so the count does not
         *     affect equals/hashCode, and we rely on your existing
         *     sameItem(a,b) helper for comparison logic.
         * ------------------------------------------------------------------ */
        Map<ItemStack, Integer> needed = new HashMap<>();
        for (ItemStack s : recipe) {
            if (s == null || s.isEmpty()) continue;
            ItemStack key = s.copy();
            key.setCount(1);             // ignore stack size when hashing
            needed.merge(key, 1, Integer::sum);
        }

        /* ------------------------------------------------------------------
         * 2.  Walk through container slots 1-46, paying down the requirements
         *     as we find matching items.
         * ------------------------------------------------------------------ */
        outer:
        for (int slot = 1; slot < this.slots.size() && !needed.isEmpty(); ++slot) {
            ItemStack inv = this.getSlot(slot).getItem();
            if (inv.isEmpty()) continue;

            // Try to satisfy as many different requirements as this stack allows.
            for (Iterator<Map.Entry<ItemStack, Integer>> it = needed.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<ItemStack, Integer> e = it.next();
                if (!sameItem(inv, e.getKey())) continue;

                int stillNeeded = e.getValue();
                int taken      = Math.min(stillNeeded, inv.getCount());

                if (taken == stillNeeded) {         // completely satisfied this ingredient
                    it.remove();
                    if (needed.isEmpty()) break outer;
                } else {                            // partially satisfied
                    e.setValue(stillNeeded - taken);
                }
            }
        }
        return needed.isEmpty();
    }
    private static boolean sameItem(ItemStack a, ItemStack b) {
        // Example implementation; keep whatever logic you already use.
        if (a == null || b == null) return false;
        if (a.isEmpty() || b.isEmpty()) return false;
        return ItemStack.isSameItemSameTags(a, b);
    }
    public boolean moveItemStackTo(ItemStack stack, int start, int end, boolean reverse) {
        return super.moveItemStackTo(stack, start, end, reverse);
    }
}
