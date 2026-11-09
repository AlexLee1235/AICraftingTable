package com.watermelon0117.aicraft.common;

import com.watermelon0117.aicraft.items.MainItem;
import com.watermelon0117.aicraft.menu.MyCraftingMenu;
import com.watermelon0117.aicraft.network.CSyncRecipesPacket;
import com.watermelon0117.aicraft.network.CSyncSpecialItemsPacket;
import com.watermelon0117.aicraft.network.PacketHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Main;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.RepairItemRecipe;
import net.minecraft.world.level.Level;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.util.thread.EffectiveSide;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class RecipeManager {
    /* ── helpers ── */
    private static String key(ItemStack s) {
        return s.isEmpty() ? "" :
                ForgeRegistries.ITEMS.getKey(s.getItem()) + (s.hasTag() ? s.getTag().toString() : "");
    }

    private static ItemStack[] sortShapeless(ItemStack[] in) {
        return Arrays.stream(in)
                .filter(s -> s != null && !s.isEmpty())
                .sorted(Comparator.comparing(RecipeManager::key))
                .map(ItemStack::copy)
                .limit(9)
                .toArray(n -> {
                    ItemStack[] a = new ItemStack[9];
                    Arrays.fill(a, ItemStack.EMPTY);
                    return a;
                });
    }
    private static ItemStack[] normShaped(ItemStack[] g) {
        int minR = 3, minC = 3, maxR = -1, maxC = -1;
        for (int i = 0; i < 9; i++)
            if (!g[i].isEmpty()) {
                int r = i / 3, c = i % 3;
                minR = Math.min(minR, r); maxR = Math.max(maxR, r);
                minC = Math.min(minC, c); maxC = Math.max(maxC, c);
            }
        ItemStack[] o = new ItemStack[9]; Arrays.fill(o, ItemStack.EMPTY);
        if (maxR < 0) return o;
        for (int r = minR; r <= maxR; r++)
            for (int c = minC; c <= maxC; c++)
                o[(r - minR) * 3 + (c - minC)] = g[r * 3 + c];
        return o;
    }

    /* ── recipe POJO ── */

    public static final class Recipe {
        public final boolean shapeless;
        public final ItemStack result;
        public final ItemStack[] grid; // len 9

        public Recipe(ItemStack res, ItemStack[] grid, boolean shapeless) {
            this.shapeless = shapeless;
            this.result = res;
            this.grid = grid;
        }

        boolean match(ItemStack[] in) {
            if (shapeless) {
                ItemStack[] cmp = sortShapeless(in);
                for (int i = 0; i < 9; i++)
                    if (!ItemStack.isSameItemSameTags(grid[i], cmp[i]))
                        return false;
                return true;
            } else {
                ItemStack[] a = normShaped(grid);
                ItemStack[] b = normShaped(in);
                for (int i = 0; i < 9; i++)
                    if (!ItemStack.isSameItemSameTags(a[i], b[i]))
                        return false;
                return true;
            }
        }

        /** This recipe with every stack of the id, made or used, swapped for the item as it is now. */
        private Recipe withItem(String id, ItemStack item) {
            ItemStack[] swapped = new ItemStack[9];
            for (int i = 0; i < 9; i++)
                swapped[i] = id.equals(MainItem.getID(grid[i])) ? item.copyWithCount(1) : grid[i];
            // A shapeless grid is kept sorted by item and tags, and the swap can change both.
            return new Recipe(id.equals(MainItem.getID(result)) ? item.copy() : result,
                    shapeless ? sortShapeless(swapped) : swapped, shapeless);
        }
    }

    /* ── public API (identical names / params) ── */
    public static ItemStack callRecipeManager(CraftingContainer container, Level level){
        ItemStack result;
        Optional<CraftingRecipe> optional = level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, container, level);
        if (optional.isPresent()) {
            CraftingRecipe craftingrecipe = optional.get();
            result = craftingrecipe.assemble(container, level.registryAccess());
            if (craftingrecipe instanceof RepairItemRecipe && MainItem.isMainItem(result)) {
                //already has 2 item and are all main item
                ItemStack stack1 = null, stack2 = null;
                for (int i = 0; i < 9; i++)
                    if (!container.getItem(i).isEmpty())
                        stack1 = container.getItem(i);
                for (int i = 8; i >= 0; i--)
                    if (!container.getItem(i).isEmpty())
                        stack2 = container.getItem(i);
                if (MainItem.getID(stack1).contentEquals(MainItem.getID(stack2))) {
                    ItemStack ret = stack1.copy();
                    ret.setDamageValue(Math.min(stack1.getMaxDamage(),
                            stack1.getDamageValue() + stack2.getDamageValue() - stack1.getMaxDamage()));
                    return ret;
                }
                // All AI items are meta_item, so vanilla takes a tool and any other AI item for a repair. They may still be an AI recipe.
                return matchGrid(container);
            }
        } else {
            result = matchGrid(container);
        }
        return result;
    }
    private static ItemStack matchGrid(CraftingContainer container) {
        ItemStack[] itemStacks = new ItemStack[9];
        for (int i = 0; i < 9; i++)
            itemStacks[i] = container.getItem(i);
        return RecipeManager.get().match(itemStacks);
    }
    public ItemStack match(ItemStack[] g) {
        return backing().stream()
                .filter(r -> r.match(g)).map(r -> r.result.copy())
                .findFirst().orElse(ItemStack.EMPTY);
    }

    /**
     * Makes this grid give the result, with the result's count, and every other recipe that makes or uses the result's id follow it.
     * Matching takes the first recipe that fits, so a regenerated result appended after the old one would never come out.
     * Ingredients match by item and tags, and regenerating can turn an item into a block or back, so a recipe left holding the old stack would stop fitting.
     */
    public void putRecipe(ItemStack result, ItemStack[] g, boolean shapeless) {
        checkServer();
        String id = MainItem.getID(result);
        List<Recipe> recipes = backing();
        recipes.removeIf(r -> Objects.equals(id, MainItem.getID(r.result)) && r.match(g));
        ItemStack[] stored = shapeless ? sortShapeless(g) : Arrays.copyOf(g, 9);
        for (int i = 0; i < stored.length; i++) {
            if (!stored[i].isEmpty()) {
                stored[i] = stored[i].copy(); // make our own copy first
                stored[i].setCount(1);        // …then force stack-size to 1
            }
        }
        recipes.add(new Recipe(result.copy(), stored, shapeless));
        recipes.replaceAll(r -> r.withItem(id, result));
        dirtyAndSync();
    }

    /** Every recipe that makes or uses the item's id follows it, as in {@link #putRecipe}, for a regeneration with no grid. Each still makes as many as before. */
    public void updateItem(ItemStack item) {
        checkServer();
        String id = MainItem.getID(item);
        backing().replaceAll(r -> r.withItem(id, item.copyWithCount(r.result.getCount())));
        dirtyAndSync();
    }

    /** @return how many the first recipe of this id makes, or 1 if it has none */
    public int craftedCount(String id) {
        return backing().stream()
                .filter(r -> Objects.equals(id, MainItem.getID(r.result)))
                .findFirst().map(r -> r.result.getCount()).orElse(1);
    }

    public void removeItem(String id) {
        checkServer();
        backing().removeIf(r -> (MainItem.isMainItem(r.result) && id.equals(MainItem.getID(r.result))) ||
                        Arrays.stream(r.grid).anyMatch(s -> MainItem.isMainItem(s) && id.equals(MainItem.getID(s))));
        dirtyAndSync();
    }

    public List<ItemStack[]> getRecipesForItem(ItemStack t) {
        if (t == null || t.isEmpty()) return List.of();
        List<ItemStack[]> out = new ArrayList<>();
        backing().forEach(r -> {
            if (ItemStack.isSameItemSameTags(r.result, t))
                out.add(Arrays.copyOf(r.grid, 9));
        });
        return out;
    }

    public boolean itemIsShapeless(ItemStack t) {
        return t != null && !t.isEmpty() && backing().stream()
                        .anyMatch(r -> r.shapeless && ItemStack.isSameItemSameTags(r.result, t));
    }
    // Syncing part
    public static RecipeManager get() {
        if (!isServer())
            return RecipeManager.ClientSide.INSTANCE;          // client singleton
        return RecipeManager.ServerSide.INSTANCE;              // server singleton (initialised in FMLServerStartingEvent)
    }
    private List<RecipeManager.Recipe> backing() {
        return !isServer() ? ClientSide.CACHE : ServerSide.data().recipes;
    }

    private void dirtyAndSync() {
        if (isServer()) {
            ServerSide.data().setDirty();
            PacketHandler.sendToAllClients(new CSyncRecipesPacket(ServerSide.data().recipes));
        }
    }

    private static void checkServer() {
        if (!isServer())
            throw new UnsupportedOperationException("Cannot mutate RecipeManager on the client");
    }
    private static boolean isServer() {
        return EffectiveSide.get() == LogicalSide.SERVER;
    }
    public static final class ClientSide {
        private static final RecipeManager INSTANCE = new RecipeManager();
        public static final List<RecipeManager.Recipe> CACHE = new ArrayList<>();
        /* Packet handler fills the cache */
        public static void refill(List<RecipeManager.Recipe> fresh) {
            CACHE.clear();
            CACHE.addAll(fresh);
        }
    }
    public static final class ServerSide {
        private static RecipeManager INSTANCE;
        private static MinecraftServer SERVER;
        public static void init(MinecraftServer srv) {
            SERVER = srv;
            INSTANCE = new RecipeManager();
        }
        public static RecipeSavedData data() {
            return RecipeSavedData.get(SERVER);
        }
    }
}
