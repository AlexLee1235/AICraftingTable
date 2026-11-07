package com.watermelon0117.aicraft.common;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class CreationOrderTest {
    @TempDir
    Path folder;

    @Test
    void theSavedOrderIsKeptAndGoneIdsAreDropped() {
        List<String> order = CreationOrder.oldestFirst(List.of("b", "c", "a"), List.of("c", "gone", "a", "b"), id -> 0);

        assertEquals(List.of("c", "a", "b"), order);
    }

    @Test
    void idsTheSavedOrderLacksGoAfterItByFileTime() {
        Map<String, Long> times = Map.of("old", 1L, "new", 9L, "saved", 50L);

        List<String> order = CreationOrder.oldestFirst(List.of("new", "saved", "old"), List.of("saved"), times::get);

        assertEquals(List.of("saved", "old", "new"), order);
    }

    @Test
    void aWorldWithNoSavedOrderGoesByFileTimeThenById() {
        Map<String, Long> times = Map.of("zeta", 5L, "alpha", 5L, "first", 2L, "nofile", 0L);

        List<String> order = CreationOrder.oldestFirst(List.of("zeta", "alpha", "first", "nofile"), List.of(), times::get);

        assertEquals(List.of("nofile", "first", "alpha", "zeta"), order);
    }

    @Test
    void anIdWithNoFilesCountsAsOldest() {
        assertEquals(0, CreationOrder.firstFileTime(folder, "Nothing Here"));
    }

    @Test
    void theEarliestOfTheItemPictureAndTheBlockFacesIsTaken() throws IOException {
        Path textures = Files.createDirectories(folder.resolve("textures"));
        Path blockTextures = Files.createDirectories(folder.resolve("block_textures"));
        Path picture = Files.write(textures.resolve("Inferno Block.png"), new byte[]{1});
        Path top = Files.write(blockTextures.resolve("Inferno Block#top.png"), new byte[]{1});
        Path front = Files.write(blockTextures.resolve("Inferno Block#front.png"), new byte[]{1});
        Path side = Files.write(blockTextures.resolve("Inferno Block#side.png"), new byte[]{1});
        // Another id's files must not count.
        Path other = Files.write(textures.resolve("Inferno Block 2.png"), new byte[]{1});
        created(picture, 3_000_000L);
        created(top, 5_000_000L);
        created(front, 2_000_000L);
        created(side, 4_000_000L);
        created(other, 1_000_000L);
        assumeTrue(Files.readAttributes(front, BasicFileAttributes.class).creationTime().toMillis() == 2_000_000L,
                "this file system keeps no creation time");

        assertEquals(2_000_000L, CreationOrder.firstFileTime(folder, "Inferno Block"));
    }

    @Test
    void theItemPictureAloneIsEnough() throws IOException {
        Path picture = Files.write(Files.createDirectories(folder.resolve("textures")).resolve("Cookie Bar.png"), new byte[]{1});
        created(picture, 7_000_000L);
        assumeTrue(Files.readAttributes(picture, BasicFileAttributes.class).creationTime().toMillis() == 7_000_000L,
                "this file system keeps no creation time");

        assertEquals(7_000_000L, CreationOrder.firstFileTime(folder, "Cookie Bar"));
    }

    private static void created(Path file, long millis) throws IOException {
        FileTime time = FileTime.fromMillis(millis);
        Files.getFileAttributeView(file, BasicFileAttributeView.class).setTimes(time, time, time);
    }
}
