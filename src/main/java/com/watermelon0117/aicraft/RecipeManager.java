package com.watermelon0117.aicraft;

import java.io.*;
import java.util.*;

public class RecipeManager {
    private static final File FILE = new File("C:\\achieve\\AICraftingTable\\recipes.txt");
    private static final Map<String, String> recipeMap = new HashMap<>();

    private static String toKey(String[] recipe) {
        if (recipe.length != 9)
            throw new IllegalArgumentException("Recipe must be 9 items.");
        return String.join(",", recipe).toLowerCase();
    }

    /** 1. Match a recipe and return crafted item name */
    public static String match(String[] recipe) {
        return recipeMap.get(toKey(recipe));
    }

    /** 2. Load all recipes from file (silently fails if error) */
    public static void loadFromFile() {
        recipeMap.clear();
        if (!FILE.exists()) return;

        try (BufferedReader reader = new BufferedReader(new FileReader(FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("=", 2);
                if (parts.length != 2) continue;

                String name = parts[0].trim();
                String[] items = parts[1].split(",", -1);
                if (items.length != 9) continue;

                recipeMap.put(String.join(",", items).toLowerCase(), name);
            }
        } catch (IOException ignored) {
            // silently fail
        }
    }

    /** 3. Add a recipe and save to file (throws RuntimeException on error) */
    public static void addRecipe(String name, String[] recipe) {
        if (recipe.length != 9)
            throw new IllegalArgumentException("Recipe must be 9 items.");

        recipeMap.put(toKey(recipe), name);
        saveToFile();
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

    /** Save all recipes to file (throws RuntimeException on failure) */
    private static void saveToFile() {
        ensureFileExists();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(FILE))) {
            for (Map.Entry<String, String> entry : recipeMap.entrySet()) {
                writer.write(entry.getValue() + "=" + entry.getKey());
                writer.newLine();
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to write recipe file", e);
        }
    }
}
