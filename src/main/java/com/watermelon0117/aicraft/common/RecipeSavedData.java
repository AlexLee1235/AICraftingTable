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
    private static MinecraftServer SERVER;
    final List<RecipeManager.Recipe> recipes = new ArrayList<>();

    /** new world – no recipes */
    RecipeSavedData() {}

    /** load from NBT */
    private RecipeSavedData(CompoundTag tag) { readFromNBT(tag); }

    public static void init(MinecraftServer srv) {
        SERVER = srv;
    }

    /* encode entire list */
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

    /* decode list */
    private void readFromNBT(CompoundTag tag) {
        recipes.clear();
        if (!tag.contains("Recipes", Tag.TAG_LIST)) return;

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

            recipes.add(new RecipeManager.Recipe(result, grid, shapeless));
        }
    }

    /* singleton getter */
    static RecipeSavedData get() {
        ServerLevel level = SERVER.overworld();
        return level.getDataStorage().computeIfAbsent(
                RecipeSavedData::new, RecipeSavedData::new, NAME);
    }
}
