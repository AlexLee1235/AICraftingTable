package com.watermelon0117.aicraft.common;


import com.watermelon0117.aicraft.items.MainItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;


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

    static final class Recipe {
        final boolean shapeless;
        final ItemStack result;
        final ItemStack[] grid; // len 9

        Recipe(ItemStack res, ItemStack[] grid, boolean shapeless) {
            this.shapeless = shapeless;
            this.result = res; this.grid = grid;
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

    public static ItemStack match(ItemStack[] g) {
        return RecipeSavedData.get().recipes.stream()
                .filter(r -> r.match(g)).map(r -> r.result.copy())
                .findFirst().orElse(ItemStack.EMPTY);
    }

    public static synchronized void addRecipe(ItemStack res, ItemStack[] g, boolean shapeless) {
        ItemStack[] stored = shapeless ? sortShapeless(g) : Arrays.copyOf(g, 9);
        RecipeSavedData data = RecipeSavedData.get();
        ItemStack resCopy=res.copy();
        resCopy.setCount(1);
        data.recipes.add(new Recipe(resCopy, stored, shapeless));
        data.setDirty();
    }

    public static void removeItem(String id) {
        if (id == null) return;
        RecipeSavedData data = RecipeSavedData.get();
        data.recipes.removeIf(r ->
                (MainItem.isMainItem(r.result) && id.equals(MainItem.getID(r.result))) ||
                        Arrays.stream(r.grid).anyMatch(s -> MainItem.isMainItem(s) && id.equals(MainItem.getID(s)))
        );
        data.setDirty();
    }

    public static List<ItemStack[]> getRecipesForItem(ItemStack t) {
        if (t == null || t.isEmpty()) return List.of();
        List<ItemStack[]> out = new ArrayList<>();
        RecipeSavedData.get().recipes.forEach(r -> {
            if (ItemStack.isSameItemSameTags(r.result, t))
                out.add(Arrays.copyOf(r.grid, 9));
        });
        return out;
    }

    public static boolean itemIsShapeless(ItemStack t) {
        return t != null && !t.isEmpty() &&
                RecipeSavedData.get().recipes.stream()
                        .anyMatch(r -> r.shapeless && ItemStack.isSameItemSameTags(r.result, t));
    }

    private RecipeManager() {}
}