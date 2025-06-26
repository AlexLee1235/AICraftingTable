package com.watermelon0117.aicraft.recipes;

import com.watermelon0117.aicraft.FileUtil;
import com.watermelon0117.aicraft.init.ItemInit;
import net.minecraft.world.item.ItemStack;

import java.io.*;
import java.util.*;

public class RecipeManager {

    /*──────────────────────────── File & Storage ───────────────────────────*/

    private static File FILE;

    /** key → crafted-item-name */
    private static final Map<String, String> recipeMap = new HashMap<>();

    private RecipeManager() {}               // no instances

    /*────────────────────── Canonical key builders ─────────────────────────*/

    /** Lower-case helper that converts <null> → "" (blank). */
    private static String canon(String s) {
        return s == null ? "" : s.toLowerCase(Locale.ROOT);
    }

    /** Trim empty outer rows/columns, then anchor pattern at (0,0). */
    private static String[] normaliseShaped(String[] r) {
        if (r.length != 9) throw new IllegalArgumentException("Needs 9 slots");

        String[] g = Arrays.stream(r).map(RecipeManager::canon).toArray(String[]::new);

        int minR = 3, minC = 3, maxR = -1, maxC = -1;
        for (int y = 0; y < 3; ++y)
            for (int x = 0; x < 3; ++x)
                if (!g[y * 3 + x].isBlank()) {
                    minR = Math.min(minR, y);  maxR = Math.max(maxR, y);
                    minC = Math.min(minC, x);  maxC = Math.max(maxC, x);
                }
        // empty pattern → return as-is (should never be stored)
        if (maxR == -1) return g;

        String[] out = new String[9];
        Arrays.fill(out, "");
        for (int y = minR; y <= maxR; ++y)
            for (int x = minC; x <= maxC; ++x)
                out[(y - minR) * 3 + (x - minC)] = g[y * 3 + x];
        return out;
    }

    private static String toShapedKey(String[] recipe) {
        if (recipe.length != 9)
            throw new IllegalArgumentException("Recipe must be 9 items.");

        // Canonical copy: lower-case every non-empty token
        String[] g = new String[9];
        for (int i = 0; i < 9; i++) {
            String s = recipe[i];
            g[i] = (s != null && !s.equalsIgnoreCase("empty"))
                    ? s.toLowerCase(Locale.ROOT)
                    : "empty";
        }

        /* bounding box of real ingredients */
        int minR = 3, minC = 3, maxR = -1, maxC = -1;
        for (int r = 0; r < 3; r++)
            for (int c = 0; c < 3; c++)
                if (!g[r * 3 + c].equals("empty")) {
                    if (r < minR) minR = r;  if (r > maxR) maxR = r;
                    if (c < minC) minC = c;  if (c > maxC) maxC = c;
                }

        // completely empty grid
        if (maxR == -1) return "shaped:empty,empty,empty,empty,empty,empty,empty,empty,empty";

        /* shift bounding-box into the upper-left corner of a fresh 3×3 grid */
        String[] out = new String[9];
        Arrays.fill(out, "empty");
        for (int r = minR; r <= maxR; r++)
            for (int c = minC; c <= maxC; c++)
                out[(r - minR) * 3 + (c - minC)] = g[r * 3 + c];

        return "shaped:" + String.join(",", out);
    }

    private static String toShapelessKey(String[] recipe) {
        if (recipe.length != 9)
            throw new IllegalArgumentException("Recipe must be 9 items.");
        List<String> list = Arrays.asList(recipe.clone());
        list.replaceAll(s -> s == null ? "" : s.toLowerCase(Locale.ROOT));
        list.sort(String::compareTo);
        return "shapeless:" + String.join(",", list);
    }

    /*──────────────────────────── Public API ───────────────────────────────*/

    /** 1️⃣  Try to craft; returns EMPTY if nothing matches. */
    public static ItemStack match(String[] grid) {
        String name = recipeMap.get(toShapedKey(grid));
        if (name == null) name = recipeMap.get(toShapelessKey(grid));
        if (name == null) return ItemStack.EMPTY;

        return SpecialItemManager.getItem(name);
    }

    /** 2️⃣  Load recipes from disk; silently ignore corrupt lines. */
    public static void loadFromFile() {
        FILE = FileUtil.getRecipeFile();
        recipeMap.clear();
        if (!FILE.exists()) return;

        try (BufferedReader br = new BufferedReader(new FileReader(FILE))) {
            String ln;
            while ((ln = br.readLine()) != null) {
                String[] p = ln.split("=", 3);
                if (p.length != 3) continue;

                String type   = p[0].trim();
                String name   = p[1].trim();
                String[] item = p[2].split(",", -1);
                if (item.length != 9) continue;

                String key = type.equals("shapeless")
                        ? toShapelessKey(item)
                        : toShapedKey(item);
                recipeMap.put(key, name);
            }
        } catch (IOException ignored) {}
    }

    /** 3️⃣  Add recipe (`shapeless==true` ⇒ shapeless). */
    public static void addRecipe(String name, String[] grid, boolean shapeless) {
        recipeMap.put(shapeless ? toShapelessKey(grid) : toShapedKey(grid), name);
        saveToFile();
    }

    public static List<String[]> getRecipesForItem(ItemStack itemStack) {
        String itemName = canon(strip(itemStack.getDisplayName().getString()));
        List<String[]> result = new ArrayList<>();

        for (Map.Entry<String, String> entry : recipeMap.entrySet()) {
            if (canon(entry.getValue()).equals(itemName)) {
                String key = entry.getKey();
                String[] items = key.substring(key.indexOf(':') + 1).split(",", -1);
                result.add(items);
            }
        }

        return result;
    }

    /*──────────────────────────── File helpers ─────────────────────────────*/

    private static void saveToFile() {
        ensureFileExists();
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(FILE))) {
            for (var e : recipeMap.entrySet()) {
                String type  = e.getKey().startsWith("shapeless:") ? "shapeless" : "shaped";
                String items = e.getKey().substring(e.getKey().indexOf(':') + 1);
                bw.write(type + '=' + e.getValue() + '=' + items);
                bw.newLine();
            }
        } catch (IOException ex) {
            throw new RuntimeException("Failed writing recipes", ex);
        }
    }

    private static void ensureFileExists() {
        try {
            File dir = FILE.getParentFile();
            if (dir != null && !dir.exists() && !dir.mkdirs())
                throw new IOException("mkdirs failed: " + dir);
            if (!FILE.exists() && !FILE.createNewFile())
                throw new IOException("createNewFile failed: " + FILE);
        } catch (IOException ex) {
            throw new RuntimeException("Cannot create recipe file", ex);
        }
    }
    private static String strip(String s) {
        return s.replace("[", "").replace("]", "");
    }
}