package com.watermelon0117.aicraft.menu;

import net.minecraft.world.item.ItemStack;
import java.util.*;

public class RecipeShapeMatcher {
    private static final List<Map.Entry<String, String>> PATTERNS = List.of(
            // Tools
            Map.entry("Pickaxe",   "III S  S "),
            Map.entry("Axe",       "II IS  S "),
            Map.entry("Axe",       " II SI S "),
            Map.entry("Shovel",    " I  S  S "),
            Map.entry("Hoe",       "II  S  S "),
            Map.entry("Hoe",       " II S  S "),
            Map.entry("Sword",     " I  I  S "),

            // Armor
            Map.entry("Helmet",    "IIII I   "),
            Map.entry("Chestplate","I IIIIIII"),
            Map.entry("Leggings",  "IIII II I"),
            Map.entry("Boots",     "I II I   ")
    );

    /**
     * Returns the matched normalized shape name (e.g. "Axe", "Helmet"), or null if no match
     */
    public static String getMatchedToolOrArmorName(ItemStack[] grid) {
        if (grid.length != 9) return null;

        for (Map.Entry<String, String> entry : PATTERNS) {
            for (String shifted : shiftPattern(entry.getValue())) {
                if (matchesPattern(grid, shifted)) {
                    return entry.getKey();
                }
            }
        }

        return null;
    }

    private static boolean matchesPattern(ItemStack[] grid, String pattern) {
        ItemStack ing = null, stick = null;

        for (int i = 0; i < 9; i++) {
            char expected = pattern.charAt(i);
            ItemStack actual = grid[i];

            if (expected == ' ') {
                if (!actual.isEmpty()) return false;
            } else if (expected == 'I') {
                if (actual.isEmpty()) return false;
                if (ing == null) ing = actual;
                else if (!ItemStack.isSameItemSameTags(actual, ing)) return false;
            } else if (expected == 'S') {
                if (actual.isEmpty()) return false;
                if (stick == null) stick = actual;
                else if (!ItemStack.isSameItemSameTags(actual, stick)) return false;
            }
        }

        return true;
    }

    private static List<String> shiftPattern(String pattern) {
        List<String> results = new ArrayList<>();
        char[][] grid = new char[3][3];

        // Fill 3x3 grid from pattern
        for (int i = 0; i < 9; i++) {
            grid[i / 3][i % 3] = pattern.charAt(i);
        }

        // Compute bounds of non-space region
        int minX = 3, maxX = -1, minY = 3, maxY = -1;
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 3; x++) {
                if (grid[y][x] != ' ') {
                    minX = Math.min(minX, x);
                    maxX = Math.max(maxX, x);
                    minY = Math.min(minY, y);
                    maxY = Math.max(maxY, y);
                }
            }
        }

        int width = maxX - minX + 1;
        int height = maxY - minY + 1;

        // Extract bounding box
        char[][] shape = new char[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                shape[y][x] = grid[minY + y][minX + x];
            }
        }

        // Try all shifts within 3x3
        for (int dy = 0; dy <= 3 - height; dy++) {
            for (int dx = 0; dx <= 3 - width; dx++) {
                char[][] shifted = new char[3][3];
                for (char[] row : shifted) Arrays.fill(row, ' ');

                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        shifted[y + dy][x + dx] = shape[y][x];
                    }
                }

                // Flatten to string
                StringBuilder sb = new StringBuilder(9);
                for (int y = 0; y < 3; y++) {
                    for (int x = 0; x < 3; x++) {
                        sb.append(shifted[y][x]);
                    }
                }
                results.add(sb.toString());
            }
        }

        return results;
    }
}
