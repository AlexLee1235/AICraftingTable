package com.watermelon0117.aicraft.recipes;


import com.watermelon0117.aicraft.FileUtil;
import com.watermelon0117.aicraft.init.ItemInit;
import com.watermelon0117.aicraft.items.MainItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public final class RecipeManager {
    public static boolean sameItem(ItemStack a, ItemStack b) {
        if (a == null || b == null || a.isEmpty() || b.isEmpty())
            return false;
        if (MainItem.isSameMainItem(a, b)) {
            String idA = MainItem.getID(a);
            String idB = MainItem.getID(b);
            return idA != null && idA.equals(idB);          // both non-null & equal
        }
        return ItemStack.isSame(a, b);
    }

    /* ────────────── internal state & helpers ────────────── */
    private static final List<Recipe> RECIPES = new ArrayList<>(256);
    private static final String EMPTY_TOKEN = "empty";

    private RecipeManager() { }

    private static Path recipeFile() { return FileUtil.getRecipeFile().toPath(); }

    private static String stackToToken(ItemStack st) {
        if (st == null || st.isEmpty()) return EMPTY_TOKEN;
        if (MainItem.isMainItem(st)) {
            String id = MainItem.getID(st);
            return "aicraftitem:" + id;
        } else {
            ResourceLocation id = ForgeRegistries.ITEMS.getKey(st.getItem());
            return id == null ? EMPTY_TOKEN : id.toString();
        }
    }
    private static ItemStack tokenToStack(String token) {
        if (token.equals(EMPTY_TOKEN)) return ItemStack.EMPTY;
        if (token.startsWith("aicraftitem:")) {
            return SpecialItemManager.getItem(token.split(":")[1]);
        } else {
            Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(token));
            return item == null ? ItemStack.EMPTY : new ItemStack(item);
        }
    }

    /* ───────────── recipe record ───────────── */

    private static final class Recipe {
        final boolean shapeless;
        final ItemStack result;          // Count == 1
        final ItemStack[] shaped;        // len 9 if shaped
        final List<ItemStack> items;     // shapeless list

        Recipe(ItemStack res, ItemStack[] shapedNorm) {
            shapeless = false; result = res; shaped = shapedNorm; items = null;
        }
        Recipe(ItemStack res, List<ItemStack> list) {
            shapeless = true;  result = res; shaped = null;       items = list;
        }

        boolean matchesShaped(ItemStack[] norm) {
            for (int i = 0; i < 9; ++i) {
                ItemStack a = shaped[i], b = norm[i];
                if (a.isEmpty() != b.isEmpty()) return false;
                if (!a.isEmpty() && !sameItem(a, b)) return false;
            }
            return true;
        }
        boolean matchesShapeless(ItemStack[] grid) {
            List<ItemStack> pool = new ArrayList<>();
            for (ItemStack g : grid) if (g != null && !g.isEmpty()) pool.add(g);

            if (pool.size() != items.size()) return false;
            boolean[] used = new boolean[pool.size()];

            outer: for (ItemStack need : items) {
                for (int i = 0; i < pool.size(); ++i)
                    if (!used[i] && sameItem(pool.get(i), need)) {
                        used[i] = true; continue outer;
                    }
                return false;
            }
            return true;
        }
        String inputTokenString() {
            if (shapeless) {
                List<String> t = new ArrayList<>();
                for (ItemStack s : items) t.add(stackToToken(s));
                while (t.size() < 9) t.add(EMPTY_TOKEN);
                t.sort(String::compareTo);
                return String.join(",", t);
            }
            String[] tok = Arrays.stream(shaped).map(RecipeManager::stackToToken).toArray(String[]::new);
            return String.join(",", tok);
        }
    }

    /* ───────────── normalise shaped grid ───────────── */

    private static ItemStack[] normaliseShaped(ItemStack[] src) {
        if (src.length != 9) throw new IllegalArgumentException("grid must have 9 slots");
        ItemStack[] g = new ItemStack[9];
        for (int i = 0; i < 9; ++i) g[i] = (src[i] == null ? ItemStack.EMPTY : src[i]);

        int minR = 3, minC = 3, maxR = -1, maxC = -1;
        for (int r = 0; r < 3; ++r)
            for (int c = 0; c < 3; ++c)
                if (!g[r * 3 + c].isEmpty()) {
                    if (r < minR) minR = r;  if (r > maxR) maxR = r;
                    if (c < minC) minC = c;  if (c > maxC) maxC = c;
                }

        ItemStack[] out = new ItemStack[9];
        Arrays.fill(out, ItemStack.EMPTY);
        if (maxR != -1)
            for (int r = minR; r <= maxR; ++r)
                for (int c = minC; c <= maxC; ++c)
                    out[(r - minR) * 3 + (c - minC)] = g[r * 3 + c];
        return out;
    }

    /* ───────────── public API ───────────── */

    public static ItemStack match(ItemStack[] grid) {
        if (grid.length != 9) return ItemStack.EMPTY;
        ItemStack[] norm = normaliseShaped(grid);

        for (Recipe r : RECIPES)
            if (r.shapeless ? r.matchesShapeless(grid) : r.matchesShaped(norm))
                return r.result.copy();
        return ItemStack.EMPTY;
    }

    public static synchronized void loadFromFile() {
        RECIPES.clear();
        Path f = recipeFile();
        if (!Files.exists(f)) return;

        try (BufferedReader br = Files.newBufferedReader(f, StandardCharsets.UTF_8)) {
            for (String ln; (ln = br.readLine()) != null; ) {
                String[] p = ln.split("=", 3); if (p.length != 3) continue;
                boolean shapeless = p[0].trim().equals("shapeless");
                ItemStack result  = tokenToStack(p[1].trim()); if (result.isEmpty()) continue;
                String[] slotTok  = p[2].split(",", -1);      if (slotTok.length != 9) continue;

                if (shapeless) {
                    List<ItemStack> list = new ArrayList<>();
                    for (String t : slotTok) { ItemStack s = tokenToStack(t.trim()); if (!s.isEmpty()) list.add(s); }
                    if (!list.isEmpty()) RECIPES.add(new Recipe(result, list));
                } else {
                    ItemStack[] shaped = new ItemStack[9];
                    for (int i = 0; i < 9; ++i) shaped[i] = tokenToStack(slotTok[i].trim());
                    RECIPES.add(new Recipe(result, shaped));
                }
            }
        } catch (IOException ignored) { }
    }

    public static synchronized void addRecipe(ItemStack result,
                                              ItemStack[] grid,
                                              boolean shapeless) {

        Objects.requireNonNull(result); Objects.requireNonNull(grid);
        if (grid.length != 9) throw new IllegalArgumentException("grid must have 9 slots");

        ItemStack resCopy = result.copy(); resCopy.setCount(1);

        if (shapeless) {
            List<ItemStack> list = new ArrayList<>();
            for (ItemStack s : grid) if (s != null && !s.isEmpty()) list.add(s.copy());
            if (list.isEmpty()) throw new IllegalArgumentException("no ingredients");
            RECIPES.add(new Recipe(resCopy, list));
        } else {
            RECIPES.add(new Recipe(resCopy, normaliseShaped(grid)));
        }
        save();
    }

    public static void removeItem(String id) {
        if (id == null) return;

        Iterator<Recipe> it = RECIPES.iterator();
        while (it.hasNext()) {
            Recipe recipe = it.next();

            // Check result item
            if (MainItem.isMainItem(recipe.result) && id.equals(MainItem.getID(recipe.result))) {
                it.remove();
                continue;
            }

            // Check shapeless ingredients
            if (recipe.shapeless) {
                for (ItemStack item : recipe.items) {
                    if (MainItem.isMainItem(item) && id.equals(MainItem.getID(item))) {
                        it.remove();
                        break;
                    }
                }
                continue;
            }

            // Check shaped grid
            for (ItemStack item : recipe.shaped) {
                if (MainItem.isMainItem(item) && id.equals(MainItem.getID(item))) {
                    it.remove();
                    break;
                }
            }
        }
    }


    public static List<ItemStack[]> getRecipesForItem(ItemStack target) {
        if (target == null || target.isEmpty()) return List.of();
        List<ItemStack[]> out = new ArrayList<>();

        for (Recipe r : RECIPES)
            if (sameItem(r.result, target)) {
                if (r.shapeless) {
                    List<ItemStack> tmp = new ArrayList<>(r.items);
                    while (tmp.size() < 9) tmp.add(ItemStack.EMPTY);
                    tmp.sort(Comparator.comparing(RecipeManager::stackToToken));
                    out.add(tmp.toArray(new ItemStack[9]));
                } else {
                    out.add(Arrays.copyOf(r.shaped, 9));
                }
            }
        return out;
    }
    public static boolean itemIsShapeless(ItemStack target){
        if (target == null || target.isEmpty()) return false;
        for (Recipe r : RECIPES)
            if (sameItem(r.result, target)) {
                return r.shapeless;
            }
        return false;
    }

    /* ───────────── persistence ───────────── */

    private static void save() {
        Path f = recipeFile();
        try {
            Files.createDirectories(f.getParent());
            try (BufferedWriter bw = Files.newBufferedWriter(
                    f, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {

                for (Recipe r : RECIPES) {
                    String type   = r.shapeless ? "shapeless" : "shaped";
                    String result = stackToToken(r.result);
                    String items  = r.inputTokenString();
                    bw.write(type + '=' + result + '=' + items);
                    bw.newLine();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("writing recipes failed", e);
        }
    }
}
