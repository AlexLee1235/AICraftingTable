package com.watermelon0117.aicraft.client.block;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
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
    void cellsComeFromTheEndAndAnIdKeepsItsCell() {
        TileAllocator allocator = new TileAllocator();
        int last = TileAllocator.TILE_COUNT - TileAllocator.TILES_PER_ROW - TileAllocator.CELL_TILES;
        assertEquals(last, allocator.cellTile("Big Plant"));
        assertEquals(last, allocator.cellTile("Big Plant"));
        assertEquals(last - TileAllocator.CELL_TILES, allocator.cellTile("Another Plant"));
        assertEquals(1, allocator.firstTile("Test Stone"), "blocks still start after the fallback");
        assertArrayEquals(new int[]{last - TileAllocator.CELL_TILES, last}, allocator.usedCellTiles());
    }

    /** Every tile a block or a cell holds, which must never be held twice. */
    private static void claim(Set<Integer> held, int first, boolean cell) {
        int[] tiles = cell
                ? new int[]{first, first + 1, first + TileAllocator.TILES_PER_ROW, first + TileAllocator.TILES_PER_ROW + 1}
                : new int[]{first, first + 1, first + 2};
        for (int tile : tiles) {
            assertTrue(tile > TileAllocator.FALLBACK_TILE && tile < TileAllocator.TILE_COUNT, "tile " + tile);
            assertTrue(held.add(tile), "tile " + tile + " held twice");
        }
    }

    @Test
    void blocksAndCellsShareTheSpaceUntilTheyMeetWithoutOverlapping() {
        TileAllocator allocator = new TileAllocator();
        Set<Integer> held = new HashSet<>();
        int blocks = 0, cells = 0;
        boolean blocksFull = false, cellsFull = false;
        for (int i = 0; !(blocksFull && cellsFull); i++) {
            if (!blocksFull) {
                int first = allocator.firstTile("block " + i);
                if (first == TileAllocator.NO_TILES) {
                    blocksFull = true;
                } else {
                    claim(held, first, false);
                    blocks++;
                }
            }
            if (!cellsFull && i % 2 == 0) {
                int first = allocator.cellTile("plant " + i);
                if (first == TileAllocator.NO_TILES) {
                    cellsFull = true;
                } else {
                    claim(held, first, true);
                    cells++;
                }
            }
        }
        assertTrue(held.size() > TileAllocator.TILE_COUNT - 200, "the space is nearly all used: " + held.size());
        assertEquals(blocks * TileAllocator.FACES_PER_BLOCK, allocator.usedTiles());
        assertEquals(cells, allocator.usedCellTiles().length);
        assertEquals(TileAllocator.NO_TILES, allocator.firstTile("one too many"));
        assertEquals(TileAllocator.NO_TILES, allocator.cellTile("one too many plants"));
        assertEquals(1, allocator.firstTile("block 0"), "a full space moves no one");
    }

    @Test
    void chunkWorkersAskingForBlocksAndCellsAtOnceGetDistinctSpace() {
        TileAllocator allocator = new TileAllocator();
        int[] tiles = IntStream.range(0, 4000).parallel()
                .map(i -> i % 2 == 0 ? allocator.firstTile("block " + i % 1000) : allocator.cellTile("plant " + i % 1000))
                .toArray();
        Set<Integer> held = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            if (i % 2 == 0) {
                claim(held, allocator.firstTile("block " + i), false);
            } else {
                claim(held, allocator.cellTile("plant " + i), true);
            }
        }
        assertEquals(500 * TileAllocator.FACES_PER_BLOCK, allocator.usedTiles());
        assertEquals(500, allocator.usedCellTiles().length);
        assertEquals(4000, tiles.length);
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
