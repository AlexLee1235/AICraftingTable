package com.watermelon0117.aicraft.recipes;

import com.watermelon0117.aicraft.init.ItemInit;
import net.minecraft.world.item.ItemStack;

import java.io.*;
import java.util.*;

public class RecipeManager {
    private static final File FILE = new File("C:\\achieve\\AICraftingTable\\data\\recipes.txt");
    private static final Map<String, String> recipeMap = new HashMap<>();

    private static String toShapedKey(String[] recipe) {
        if (recipe.length != 9)
            throw new IllegalArgumentException("Recipe must be 9 items.");
        return "shaped:" + String.join(",", recipe).toLowerCase();
    }

    private static String toShapelessKey(String[] recipe) {
        if (recipe.length != 9)
            throw new IllegalArgumentException("Recipe must be 9 items.");
        List<String> list = Arrays.asList(recipe.clone());
        list.replaceAll(String::toLowerCase);
        list.sort(String::compareTo); // sort ingredients
        return "shapeless:" + String.join(",", list);
    }

    /** 1. Match shaped or shapeless recipe */
    public static ItemStack match(String[] recipe) {
        String name = recipeMap.get(toShapedKey(recipe));
        if (name == null) {
            name = recipeMap.get(toShapelessKey(recipe));
        }
        if (name != null) {
            ItemStack itemstack = new ItemStack(ItemInit.MAIN_ITEM.get());
            itemstack.getOrCreateTag().putString("texture", name);
            return itemstack;
        }
        return ItemStack.EMPTY;
    }

    /** 2. Load all recipes from file (silently fails if error) */
    public static void loadFromFile() {
        recipeMap.clear();
        if (!FILE.exists()) return;

        try (BufferedReader reader = new BufferedReader(new FileReader(FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("=", 3);
                if (parts.length != 3) continue;

                String type = parts[0].trim(); // "shaped" or "shapeless"
                String name = parts[1].trim();
                String[] items = parts[2].split(",", -1);
                if (items.length != 9) continue;

                String key = (type.equals("shaped") ? toShapedKey(items) : toShapelessKey(items));
                recipeMap.put(key, name);
            }
        } catch (IOException ignored) {
            // silently fail
        }
    }

    /** 3a. Add shaped recipe */
    public static void addRecipe(String name, String[] recipe, boolean shpaeless) {
        if(shpaeless)
            recipeMap.put(toShapelessKey(recipe), name);
        else
            recipeMap.put(toShapedKey(recipe), name);
        saveToFile();
    }

    /** Save all recipes to file (throws RuntimeException on failure) */
    private static void saveToFile() {
        ensureFileExists();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(FILE))) {
            for (Map.Entry<String, String> entry : recipeMap.entrySet()) {
                String key = entry.getKey();
                String type = key.startsWith("shapeless:") ? "shapeless" : "shaped";
                String items = key.substring(key.indexOf(':') + 1);
                writer.write(type + "=" + entry.getValue() + "=" + items);
                writer.newLine();
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to write recipe file", e);
        }
    }

    /** Ensure file and parent directories exist (throws RuntimeException on failure) */
    private static void ensureFileExists() {
        try {
            File parent = FILE.getParentFile();
            if (parent != null && !parent.exists()) {
                if (!parent.mkdirs()) {
                    throw new RuntimeException("Failed to create directory: " + parent);
                }
            }
            if (!FILE.exists()) {
                if (!FILE.createNewFile()) {
                    throw new RuntimeException("Failed to create file: " + FILE);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("I/O error while creating file", e);
        }
    }
}

