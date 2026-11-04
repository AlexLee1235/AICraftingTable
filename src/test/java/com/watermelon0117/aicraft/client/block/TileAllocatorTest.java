package com.watermelon0117.aicraft.client.block;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TileAllocatorTest {
    @Test
    void anIdKeepsItsTilesAndTheFallbackIsNeverHandedOut() {
        TileAllocator allocator = new TileAllocator();
        int first = allocator.firstTile("Test Stone");
        assertEquals(1, first);
        assertEquals(first, allocator.firstTile("Test Stone"));
        assertEquals(first + TileAllocator.FACES_PER_BLOCK, allocator.firstTile("Another"));
        assertEquals(2 * TileAllocator.FACES_PER_BLOCK, allocator.usedTiles());
    }

    @Test
    void everyBlockFitsInTheAtlasThenAllocationStopsWithoutMovingAnyone() {
        TileAllocator allocator = new TileAllocator();
        int last = 0;
        for (int i = 0; i < TileAllocator.MAX_BLOCKS; i++) {
            last = allocator.firstTile("block " + i);
            assertNotEquals(TileAllocator.NO_TILES, last);
        }
        assertTrue(last + TileAllocator.FACES_PER_BLOCK <= TileAllocator.TILE_COUNT, "last tile " + last);
        assertEquals(1365, TileAllocator.MAX_BLOCKS);

        assertEquals(TileAllocator.NO_TILES, allocator.firstTile("one too many"));
        assertEquals(TileAllocator.NO_TILES, allocator.firstTile("one too many"));
        assertEquals(1, allocator.firstTile("block 0"));
        assertEquals(TileAllocator.MAX_BLOCKS * TileAllocator.FACES_PER_BLOCK, allocator.usedTiles());
    }

    @Test
    void chunkWorkersAskingAtOnceGetDistinctTiles() {
        TileAllocator allocator = new TileAllocator();
        int[] tiles = IntStream.range(0, 4000).parallel().map(i -> allocator.firstTile("block " + i % 1000)).toArray();
        Set<Integer> distinct = new HashSet<>();
        for (int tile : tiles) {
            distinct.add(tile);
        }
        assertEquals(1000, distinct.size());
        assertEquals(1000 * TileAllocator.FACES_PER_BLOCK, allocator.usedTiles());
    }
}
