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
        return d;
    }

    /* singleton getter */
    static RecipeSavedData get(MinecraftServer srv) {
        ServerLevel level = srv.overworld();
        return level.getDataStorage().computeIfAbsent(
                RecipeSavedData::load, RecipeSavedData::new, NAME);
    }
}
