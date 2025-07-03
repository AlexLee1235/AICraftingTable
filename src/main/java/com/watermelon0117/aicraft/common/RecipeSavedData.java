package com.watermelon0117.aicraft.common;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.List;

public class RecipeSavedData extends SavedData {
    private static final String NAME = "aicraft_recipes";
    public final List<RecipeManager.Recipe> recipes = new ArrayList<>();
    @Override public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (RecipeManager.Recipe r : recipes) {
            CompoundTag rt = new CompoundTag();
            rt.putBoolean("Shapeless", r.shapeless);
            rt.put("Result", r.result.save(new CompoundTag()));

            ListTag grid = new ListTag();
            for (ItemStack s : r.grid) grid.add(s.save(new CompoundTag()));
            rt.put("Grid", grid);

            list.add(rt);
        }
        tag.put("Recipes", list);
        return tag;
    }
    static RecipeSavedData load(CompoundTag tag) {
        RecipeSavedData d = new RecipeSavedData();
        d.recipes.clear();
        if (!tag.contains("Recipes", Tag.TAG_LIST)) return null;

        ListTag list = tag.getList("Recipes", Tag.TAG_COMPOUND);
        for (Tag t : list) {
            CompoundTag rt = (CompoundTag) t;
            boolean shapeless = rt.getBoolean("Shapeless");
            ItemStack result = ItemStack.of(rt.getCompound("Result"));
            if (result.isEmpty()) continue;

            ListTag gridTag = rt.getList("Grid", Tag.TAG_COMPOUND);
            if (gridTag.size() != 9) continue;

            ItemStack[] grid = new ItemStack[9];
            for (int i = 0; i < 9; ++i) grid[i] = ItemStack.of(gridTag.getCompound(i));

            d.recipes.add(new RecipeManager.Recipe(result, grid, shapeless));
        }
        dedupe(d.recipes);
        return d;
    }

    /* singleton getter */
    static RecipeSavedData get(MinecraftServer srv) {
        ServerLevel level = srv.overworld();
        return level.getDataStorage().computeIfAbsent(
                RecipeSavedData::load, RecipeSavedData::new, NAME);
    }

    private static void dedupe(List<RecipeManager.Recipe> list) {
        List<RecipeManager.Recipe> unique = new ArrayList<>();
        outer:
        for (RecipeManager.Recipe r : list) {
            for (RecipeManager.Recipe u : unique) {
                if (sameRecipe(r, u)) continue outer;   // already have this one
            }
            unique.add(r);                              // first time we see it
        }
        list.clear();
        list.addAll(unique);
    }
    private static boolean sameRecipe(RecipeManager.Recipe a, RecipeManager.Recipe b) {
        if (a.shapeless != b.shapeless) return false;
        if (!ItemStack.isSameItemSameTags(a.result, b.result) ||
                a.result.getCount() != b.result.getCount())        return false;
        for (int i = 0; i < 9; ++i) {
            if (!ItemStack.isSameItemSameTags(a.grid[i], b.grid[i]) ||
                    a.grid[i].getCount() != b.grid[i].getCount())  return false;
        }
        return true;
    }
}
