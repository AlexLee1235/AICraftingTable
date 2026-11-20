package com.watermelon0117.aicraft.client.block;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Hands each id three tiles of the reserved atlas space, the first time anything asks for it.
 * Chunk workers ask too, because a chunk can arrive before the texture does.
 */
public final class TileAllocator {
    public static final int TILES_PER_ROW = 64;
    public static final int TILE_COUNT = TILES_PER_ROW * TILES_PER_ROW;
    public static final int FACES_PER_BLOCK = 3;
    public static final int FALLBACK_TILE = 0;
    public static final int MAX_BLOCKS = (TILE_COUNT - 1) / FACES_PER_BLOCK;
    public static final int NO_TILES = -1;

    private final Map<String, Integer> firstTiles = new ConcurrentHashMap<>();
    private final AtomicInteger blocks = new AtomicInteger();

    /** @return the first of the id's three tiles, or {@link #NO_TILES} when the atlas space has run out */
    public int firstTile(String id) {
        Integer tile = firstTiles.computeIfAbsent(id, key -> {
            int block = blocks.getAndUpdate(count -> count < MAX_BLOCKS ? count + 1 : count);
            return block < MAX_BLOCKS ? 1 + block * FACES_PER_BLOCK : null;
        });
        return tile == null ? NO_TILES : tile;
    }

    /** @return how many tiles after the fallback have been handed out */
    public int usedTiles() {
        return blocks.get() * FACES_PER_BLOCK;
    }
}
