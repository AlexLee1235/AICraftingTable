package com.watermelon0117.aicraft.common;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlockTextureStoreTest {
    @TempDir
    Path folder;

    private static BlockFaces faces(int size, int fill) {
        byte[] top = new byte[size], front = new byte[size], side = new byte[size];
        java.util.Arrays.fill(top, (byte) fill);
        java.util.Arrays.fill(front, (byte) (fill + 1));
        java.util.Arrays.fill(side, (byte) (fill + 2));
        return new BlockFaces(top, front, side);
    }

    @Test
    void idsWithSpacesAndDotsComeBackWithTheirOwnFaces() throws IOException {
        BlockFaces first = faces(10, 1), second = faces(20, 5);
        BlockTextureStore.save(folder, "Mr. Stone+ 2-A", first);
        BlockTextureStore.save(folder, "Plain", second);

        Map<String, BlockFaces> loaded = BlockTextureStore.loadAll(folder);

        assertEquals(2, loaded.size());
        assertArrayEquals(first.top(), loaded.get("Mr. Stone+ 2-A").top());
        assertArrayEquals(first.front(), loaded.get("Mr. Stone+ 2-A").front());
        assertArrayEquals(first.side(), loaded.get("Mr. Stone+ 2-A").side());
        assertArrayEquals(second.side(), loaded.get("Plain").side());
    }

    @Test
    void anIdMissingAFaceAndUnrelatedFilesAreLeftOut() throws IOException {
        BlockTextureStore.save(folder, "Whole", faces(4, 1));
        Files.write(folder.resolve("Partial#top.png"), new byte[]{1});
        Files.write(folder.resolve("notes.txt"), new byte[]{1});
        Files.write(folder.resolve("item.png"), new byte[]{1});

        assertEquals(java.util.Set.of("Whole"), BlockTextureStore.loadAll(folder).keySet());
    }

    @Test
    void savingAgainReplacesTheFaces() throws IOException {
        BlockTextureStore.save(folder, "Again", faces(4, 1));
        BlockFaces newer = faces(4, 7);
        BlockTextureStore.save(folder, "Again", newer);

        assertArrayEquals(newer.top(), BlockTextureStore.loadAll(folder).get("Again").top());
    }

    @Test
    void oneIdComesBackWithItsOwnFaces() throws IOException {
        BlockFaces saved = faces(6, 3);
        BlockTextureStore.save(folder, "Mr. Stone+ 2-A", saved);
        BlockTextureStore.save(folder, "Other", faces(6, 9));

        BlockFaces loaded = BlockTextureStore.load(folder, "Mr. Stone+ 2-A");

        assertArrayEquals(saved.top(), loaded.top());
        assertArrayEquals(saved.front(), loaded.front());
        assertArrayEquals(saved.side(), loaded.side());
    }

    @Test
    void loadingAnIdMissingAFaceThrows() throws IOException {
        Files.write(folder.resolve("Partial#top.png"), new byte[]{1});
        Files.write(folder.resolve("Partial#front.png"), new byte[]{1});

        assertThrows(NoSuchFileException.class, () -> BlockTextureStore.load(folder, "Partial"));
        assertThrows(NoSuchFileException.class, () -> BlockTextureStore.load(folder, "Nothing"));
    }

    @Test
    void theFolderIsReadOnceAndSavesAreAddedToWhatWasRead() throws IOException {
        BlockTextureStore.clearCache();
        try {
            BlockTextureStore.save(folder, "First", faces(4, 1));
            assertEquals(java.util.Set.of("First"), BlockTextureStore.loadAllAsync(folder).join().keySet());

            // Gone from disk, so only memory can still answer with it.
            for (String face : List.of("top", "front", "side")) {
                Files.delete(folder.resolve(BlockTextureStore.fileName("First", face)));
            }
            BlockFaces second = faces(4, 7);
            BlockTextureStore.save(folder, "Second", second);

            Map<String, BlockFaces> again = BlockTextureStore.loadAllAsync(folder).join();
            assertEquals(java.util.Set.of("First", "Second"), again.keySet());
            assertArrayEquals(second.side(), again.get("Second").side());

            BlockTextureStore.clearCache();
            assertEquals(java.util.Set.of("Second"), BlockTextureStore.loadAllAsync(folder).join().keySet());
        } finally {
            BlockTextureStore.clearCache();
        }
    }

    @Test
    void anotherWorldsFolderGetsItsOwnFaces(@TempDir Path other) throws IOException {
        BlockTextureStore.clearCache();
        try {
            BlockTextureStore.save(folder, "Here", faces(4, 1));
            BlockTextureStore.save(other, "There", faces(4, 5));
            assertEquals(java.util.Set.of("Here"), BlockTextureStore.loadAllAsync(folder).join().keySet());

            // No clearCache between them: the folder alone must be enough.
            assertEquals(java.util.Set.of("There"), BlockTextureStore.loadAllAsync(other).join().keySet());
            BlockTextureStore.save(folder, "Late", faces(4, 9));
            assertEquals(java.util.Set.of("There"), BlockTextureStore.loadAllAsync(other).join().keySet());
            assertEquals(java.util.Set.of("Here", "Late"), BlockTextureStore.loadAllAsync(folder).join().keySet());
        } finally {
            BlockTextureStore.clearCache();
        }
    }

    @Test
    void aRegeneratedBlockReplacesWhatWasRead() throws IOException {
        BlockTextureStore.clearCache();
        try {
            BlockTextureStore.save(folder, "Again", faces(4, 1));
            BlockTextureStore.loadAllAsync(folder).join();
            BlockFaces newer = faces(4, 7), newest = faces(4, 9);
            BlockTextureStore.save(folder, "Again", newer);
            BlockTextureStore.save(folder, "Again", newest);

            assertArrayEquals(newest.top(), BlockTextureStore.loadAllAsync(folder).join().get("Again").top());
        } finally {
            BlockTextureStore.clearCache();
        }
    }

    @Test
    void batchesStayUnderTheLimitAndLoseNothing() {
        Map<String, BlockFaces> all = new LinkedHashMap<>();
        for (int i = 0; i < 1000; i++) {
            all.put("block " + i, faces(800, i));
        }

        List<Map<String, BlockFaces>> batches = BlockTextureStore.batches(all, BlockTextureStore.MAX_BATCH_BYTES);

        assertTrue(batches.size() > 1, "2.4 MB of faces should not fit in one packet");
        Map<String, BlockFaces> joined = new LinkedHashMap<>();
        for (Map<String, BlockFaces> batch : batches) {
            int bytes = batch.values().stream().mapToInt(BlockFaces::byteSize).sum();
            assertTrue(bytes <= BlockTextureStore.MAX_BATCH_BYTES, bytes + " bytes in one batch");
            joined.putAll(batch);
        }
        assertEquals(all.keySet(), joined.keySet());
    }

    @Test
    void nothingToSendMakesNoBatchAndALargeBlockStillGetsOne() {
        assertEquals(0, BlockTextureStore.batches(Map.of(), 100).size());
        assertEquals(1, BlockTextureStore.batches(Map.of("Huge", faces(1000, 1)), 100).size());
    }
}
