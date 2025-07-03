package com.watermelon0117.aicraft.common;


import com.watermelon0117.aicraft.items.MainItem;
import com.watermelon0117.aicraft.network.CSyncRecipesPacket;
import com.watermelon0117.aicraft.network.CSyncSpecialItemsPacket;
import com.watermelon0117.aicraft.network.PacketHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.util.thread.EffectiveSide;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Identical recipe logic, but all persistence is now handled through
 * {@link RecipeSavedData} instead of an external file.
 */
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
    }

    /* ── public API (identical names / params) ── */

    public ItemStack match(ItemStack[] g) {
        return backing().stream()
                .filter(r -> r.match(g)).map(r -> r.result.copy())
                .findFirst().orElse(ItemStack.EMPTY);
    }

    public void addRecipe(ItemStack result, ItemStack[] g, boolean shapeless) {
        ItemStack[] stored = shapeless ? sortShapeless(g) : Arrays.copyOf(g, 9);
        for (int i = 0; i < stored.length; i++) {
            if (!stored[i].isEmpty()) {
                stored[i] = stored[i].copy(); // make our own copy first
                stored[i].setCount(1);        // …then force stack-size to 1
            }
        }
        ItemStack resCopy=result.copy();
        resCopy.setCount(1);
        backing().add(new Recipe(resCopy, stored, shapeless));
        dirtyAndSync();
    }

    public void removeItem(String id) {
        if (id == null) return;
        backing().removeIf(r ->
                (MainItem.isMainItem(r.result) && id.equals(MainItem.getID(r.result))) ||
                        Arrays.stream(r.grid).anyMatch(s -> MainItem.isMainItem(s) && id.equals(MainItem.getID(s)))
        );
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
        return t != null && !t.isEmpty() &&
                backing().stream()
                        .anyMatch(r -> r.shapeless && ItemStack.isSameItemSameTags(r.result, t));
    }
    // Syncing part
    public static RecipeManager get() {
        if (!isServer())
            return RecipeManager.ClientSide.INSTANCE;          // client singleton
        return RecipeManager.ServerSide.INSTANCE;              // server singleton (initialised in FMLServerStartingEvent)
    }
    private List<RecipeManager.Recipe> backing() {
        return !isServer()
                ? ClientSide.CACHE
                : ServerSide.data().recipes;
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