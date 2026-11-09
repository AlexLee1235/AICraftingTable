package com.watermelon0117.aicraft.gpt.opanai;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.*;

public final class RecipeShapeMatcher {
    private static final List<Map.Entry<String, String>> PATTERNS = List.of(
            Map.entry("Pickaxe", "III S  S "),
            Map.entry("Axe", "II IS  S "),
            Map.entry("Axe", " II SI S "),
            Map.entry("Shovel", " I  S  S "),
            Map.entry("Hoe", "II  S  S "),
            Map.entry("Hoe", " II S  S "),
            Map.entry("Sword", " I  I  S "),

            Map.entry("Helmet",    "IIII I   "),
            Map.entry("Chestplate","I IIIIIII"),
            Map.entry("Leggings",  "IIII II I"),
            Map.entry("Boots",     "I II I   ")
    );
    private static final List<Map.Entry<String, String>> BLOCK_PATTERNS = List.of(
            Map.entry("Slab", "III      "),
            Map.entry("Stairs", "I  II III"),
            Map.entry("Stairs", "  I IIIII"),
            Map.entry("Wall", "IIIIII   "),
            Map.entry("Fence", "ISIISI   ")
    );
    /** Four of one ingredient in a square, which is no vanilla shape with a model and a count of its own. */
    private static final List<Map.Entry<String, String>> SQUARE_PATTERNS = List.of(Map.entry("2x2", "II II    "));

    public record Match(String shapeName, String material) {
    }

    /*───────────────────────────── Public API ─────────────────────────────*/

    /**
     * Returns the first matching (shape, material) pair among the tools and armor, or {@code null} if none.
     */
    public static Match match(String[] grid) {
        return match(grid, PATTERNS);
    }

    /** @return the slab, stairs, wall or fence layout of the grid, as the texture server's {@code matchBlockShape}, or null */
    public static Match matchBlockShape(String[] grid) {
        return match(grid, BLOCK_PATTERNS);
    }

    public static boolean isSquare(String[] grid) {
        return match(grid, SQUARE_PATTERNS) != null;
    }

    private static Match match(String[] grid, List<Map.Entry<String, String>> patterns) {
        for (Map.Entry<String, String> entry : patterns) {
            for (String shifted : shiftPattern(entry.getValue())) {
                String mat = tryMatch(grid, shifted);
                if (!mat.isEmpty()) {
                    return new Match(entry.getKey(), mat);
                }
            }
        }
        return null;
    }

    /*───────────────────────────── Internals ──────────────────────────────*/

    private static String tryMatch(String[] grid, String pattern) {
        String ingot = "";
        String stick = "Stick";
        for (int i = 0; i < 9; i++) {
            char expected = pattern.charAt(i);
            String actual = grid[i];
            switch (expected) {
                case ' ' -> {
                    if (!actual.contentEquals("empty")) return "";
                }
                case 'S' -> {
                    if (!actual.contentEquals(stick))
                        return "";
                }
                case 'I' -> {
                    if (actual.contentEquals("empty")) return "";
                    if (ingot.isEmpty()) ingot = actual;
                    else if (!actual.contentEquals(ingot))
                        return "";
                }
            }
        }
        return ingot;      // non-empty only on a full match
    }

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
}