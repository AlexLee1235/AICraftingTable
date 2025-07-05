package com.watermelon0117.aicraft.common;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.*;

public final class RecipeShapeMatcher {
    private static final List<Map.Entry<String, String>> PATTERNS = List.of(
            // Tools
            Map.entry("Pickaxe", "III S  S "),
            Map.entry("Axe", "II IS  S "),
            Map.entry("Axe", " II SI S "),
            Map.entry("Shovel", " I  S  S "),
            Map.entry("Hoe", "II  S  S "),
            Map.entry("Hoe", " II S  S "),
            Map.entry("Sword", " I  I  S ")

            // Uncomment to re-enable armour
            // Map.entry("Helmet",    "IIII I   "),
            // Map.entry("Chestplate","I IIIIIII"),
            // Map.entry("Leggings",  "IIII II I"),
            // Map.entry("Boots",     "I II I   ")
    );

    public record Match(String shapeName, ItemStack material) {
        public String materialName() {
            return strip(material().getDisplayName().getString());
        }
    }

    /*───────────────────────────── Public API ─────────────────────────────*/

    /**
     * Returns the first matching (shape, material) pair, or {@code null} if none.
     */
    public static Match match(ItemStackArray grid) {
        for (Map.Entry<String, String> entry : PATTERNS) {
            for (String shifted : shiftPattern(entry.getValue())) {
                ItemStack mat = tryMatch(grid.items, shifted);
                if (!mat.isEmpty()) {
                    return new Match(entry.getKey(), mat);
                }
            }
        }
        return null;
    }

    /*───────────────────────────── Internals ──────────────────────────────*/

    private static ItemStack tryMatch(ItemStack[] grid, String pattern) {
        ItemStack ingot = ItemStack.EMPTY;
        ItemStack stick = new ItemStack(Items.STICK);

        for (int i = 0; i < 9; i++) {
            char expected = pattern.charAt(i);
            ItemStack actual = grid[i];

            switch (expected) {
                case ' ' -> {
                    if (!actual.isEmpty()) return ItemStack.EMPTY;
                }
                case 'S' -> {
                    if (actual.isEmpty() || !ItemStack.isSameItemSameTags(actual, stick))
                        return ItemStack.EMPTY;
                }
                case 'I' -> {
                    if (actual.isEmpty()) return ItemStack.EMPTY;
                    if (ingot.isEmpty()) ingot = actual;
                    else if (!ItemStack.isSameItemSameTags(actual, ingot))
                        return ItemStack.EMPTY;
                }
            }
        }
        return ingot;      // non-empty only on a full match
    }

    /**
     * Generate every valid shift of the compact pattern inside a 3 × 3 grid.
     */
    private static List<String> shiftPattern(String pattern) {
        List<String> out = new ArrayList<>(9);
        char[][] src = new char[3][3];

        for (int i = 0; i < 9; i++) src[i / 3][i % 3] = pattern.charAt(i);

        int minX = 3, minY = 3, maxX = -1, maxY = -1;
        for (int y = 0; y < 3; y++)
            for (int x = 0; x < 3; x++)
                if (src[y][x] != ' ') {
                    minX = Math.min(minX, x);
                    maxX = Math.max(maxX, x);
                    minY = Math.min(minY, y);
                    maxY = Math.max(maxY, y);
                }

        int w = maxX - minX + 1, h = maxY - minY + 1;
        char[][] shape = new char[h][w];
        for (int y = 0; y < h; y++)
            System.arraycopy(src[minY + y], minX, shape[y], 0, w);

        for (int dy = 0; dy <= 3 - h; dy++)
            for (int dx = 0; dx <= 3 - w; dx++) {
                StringBuilder sb = new StringBuilder(9).append("         "); // 9 spaces
                for (int y = 0; y < h; y++)
                    for (int x = 0; x < w; x++)
                        sb.setCharAt((y + dy) * 3 + (x + dx), shape[y][x]);
                out.add(sb.toString());
            }
        return out;
    }

    private static String strip(String s) {
        return s.replace("[", "").replace("]", "");
    }
}