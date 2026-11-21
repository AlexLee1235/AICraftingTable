package com.watermelon0117.aicraft.common;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArmorTextureStoreTest {
    @TempDir
    Path folder;

    private static ArmorTexture texture(int size, int fill) {
        byte[] png = new byte[size];
        java.util.Arrays.fill(png, (byte) fill);
        return new ArmorTexture(png, "{\"fill\": " + fill + "}");
    }

    @Test
    void idsWithSpacesAndDotsComeBackWithTheirOwnPictures() throws IOException {
        ArmorTexture first = texture(10, 1), second = texture(20, 5);
        ArmorTextureStore.save(folder, "Mr. Steel+ 2-A Helmet", first);
        ArmorTextureStore.save(folder, "Plain", second);

        Map<String, ArmorTexture> loaded = ArmorTextureStore.loadAll(folder);

        assertEquals(Set.of("Mr. Steel+ 2-A Helmet", "Plain"), loaded.keySet());
        assertArrayEquals(first.png(), loaded.get("Mr. Steel+ 2-A Helmet").png());
        assertEquals(first.uv(), loaded.get("Mr. Steel+ 2-A Helmet").uv());
        assertArrayEquals(second.png(), loaded.get("Plain").png());
    }

    @Test
    void aPictureWithoutItsTableAndOtherFilesAreLeftOut() throws IOException {
        ArmorTextureStore.save(folder, "Whole", texture(4, 1));
        Files.write(folder.resolve("Half.png"), new byte[]{1});
        Files.write(folder.resolve("Lonely.json"), new byte[]{1});
        Files.write(folder.resolve("notes.txt"), new byte[]{1});

        assertEquals(Set.of("Whole"), ArmorTextureStore.loadAll(folder).keySet());
    }

    @Test
    void savingAgainReplacesThePictureAlsoInMemory() throws IOException {
        ArmorTextureStore.clearCache();
        try {
            ArmorTextureStore.save(folder, "Again", texture(4, 1));
            ArmorTextureStore.loadAllAsync(folder).join();
            ArmorTexture newer = texture(4, 7);
            ArmorTextureStore.save(folder, "Again", newer);

            assertArrayEquals(newer.png(), ArmorTextureStore.loadAll(folder).get("Again").png());
            assertArrayEquals(newer.png(), ArmorTextureStore.loadAllAsync(folder).join().get("Again").png());
        } finally {
            ArmorTextureStore.clearCache();
        }
    }

    @Test
    void anotherWorldsFolderGetsItsOwnPictures(@TempDir Path other) throws IOException {
        ArmorTextureStore.clearCache();
        try {
            ArmorTextureStore.save(folder, "Here", texture(4, 1));
            ArmorTextureStore.save(other, "There", texture(4, 5));
            assertEquals(Set.of("Here"), ArmorTextureStore.loadAllAsync(folder).join().keySet());
            assertEquals(Set.of("There"), ArmorTextureStore.loadAllAsync(other).join().keySet());
        } finally {
            ArmorTextureStore.clearCache();
        }
    }

    @Test
    void batchesStayUnderTheLimitAndLoseNothing() {
        Map<String, ArmorTexture> all = new LinkedHashMap<>();
        for (int i = 0; i < 400; i++) {
            all.put("piece " + i, texture(6000, i));
        }

        List<Map<String, ArmorTexture>> batches = ArmorTextureStore.batches(all, ArmorTextureStore.MAX_BATCH_BYTES);

        assertTrue(batches.size() > 1, "2.4 MB of pictures should not fit in one packet");
        Map<String, ArmorTexture> joined = new LinkedHashMap<>();
        for (Map<String, ArmorTexture> batch : batches) {
            int bytes = batch.values().stream().mapToInt(ArmorTexture::byteSize).sum();
            assertTrue(bytes <= ArmorTextureStore.MAX_BATCH_BYTES, bytes + " bytes in one batch");
            joined.putAll(batch);
        }
        assertEquals(all.keySet(), joined.keySet());
        assertEquals(0, ArmorTextureStore.batches(Map.of(), 100).size());
        assertEquals(1, ArmorTextureStore.batches(Map.of("Huge", texture(1000, 1)), 100).size());
    }
}
