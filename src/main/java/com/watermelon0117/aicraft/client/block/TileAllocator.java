package com.watermelon0117.aicraft.client.block;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

/**
 * Hands each id its space in the reserved atlas, the first time anything asks for it.
 * Chunk workers ask too, because a chunk can arrive before the texture does.
 *
 * <p>A block takes three tiles in a row, counted up from the first tile after the fallback. A plant whose picture is
 * larger than a tile takes a cell of two by two tiles instead, counted down from the last cell, so the two kinds share
 * the space in any mix. The space is full when they meet. A cell's first tile grows with the cell's number, so the
 * lowest cell handed out is the one the blocks must stay below.
 */
public final class TileAllocator {
    public static final int TILES_PER_ROW = 64;
    public static final int TILE_COUNT = TILES_PER_ROW * TILES_PER_ROW;
    public static final int FACES_PER_BLOCK = 3;
    public static final int FALLBACK_TILE = 0;
    /** How many blocks fit when no plant takes a cell. */
    public static final int MAX_BLOCKS = (TILE_COUNT - 1) / FACES_PER_BLOCK;
    /** A cell is this many tiles wide and high: room for a 32x32 picture. */
    public static final int CELL_TILES = 2;
    public static final int CELLS_PER_ROW = TILES_PER_ROW / CELL_TILES;
    public static final int CELL_COUNT = CELLS_PER_ROW * CELLS_PER_ROW;
    public static final int NO_TILES = -1;

    private final Map<String, Integer> firstTiles = new ConcurrentHashMap<>();
    private final Map<String, Integer> cellTiles = new ConcurrentHashMap<>();
    /** Both counts are read and changed together, so a block and a cell handed out at once cannot overlap. */
    private int blocks;
    private int cells;

    /** @return the first of the id's three tiles, or {@link #NO_TILES} when the atlas space has run out */
    public int firstTile(String id) {
        Integer tile = firstTiles.get(id);
        return tile != null ? tile : newBlock(id);
    }

    /** @return the top left tile of the id's cell, or {@link #NO_TILES} when the atlas space has run out */
    public int cellTile(String id) {
        Integer tile = cellTiles.get(id);
        return tile != null ? tile : newCell(id);
    }

    private synchronized int newBlock(String id) {
        Integer known = firstTiles.get(id);
        if (known != null) {
            return known;
        }
        int first = blocksEnd();
        if (first + FACES_PER_BLOCK > lowestCellTile()) {
            return NO_TILES;
        }
        blocks++;
        firstTiles.put(id, first);
        return first;
    }

    private synchronized int newCell(String id) {
        Integer known = cellTiles.get(id);
        if (known != null) {
            return known;
        }
        int cell = CELL_COUNT - 1 - cells;
        if (cell < 0 || firstTileOfCell(cell) < blocksEnd()) {
            return NO_TILES;
        }
        cells++;
        int tile = firstTileOfCell(cell);
        cellTiles.put(id, tile);
        return tile;
    }

    /** One past the last tile a block has. */
    private int blocksEnd() {
        return 1 + blocks * FACES_PER_BLOCK;
    }

    private int lowestCellTile() {
        return cells == 0 ? TILE_COUNT : firstTileOfCell(CELL_COUNT - cells);
    }

    static int firstTileOfCell(int cell) {
        return cell / CELLS_PER_ROW * CELL_TILES * TILES_PER_ROW + cell % CELLS_PER_ROW * CELL_TILES;
    }

    /** @return how many tiles after the fallback have been handed out to blocks */
    public synchronized int usedTiles() {
        return blocks * FACES_PER_BLOCK;
    }

    /** @return the top left tile of every cell handed out */
    public synchronized int[] usedCellTiles() {
        return IntStream.range(CELL_COUNT - cells, CELL_COUNT).map(TileAllocator::firstTileOfCell).toArray();
    }
}
