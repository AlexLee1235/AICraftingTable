package com.watermelon0117.aicraft.common;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

/**
 * Faces live in their own folder as {@code <id>#top.png}: TextureManager treats every file in the item folder as an item,
 * and ids allow neither '#' nor any path separator.
 */
public final class BlockTextureStore {
    /** A custom payload may not exceed 1,048,576 bytes, and the constructor that checks it throws. */
    public static final int MAX_BATCH_BYTES = 1_000_000;
    private static final int ENTRY_OVERHEAD_BYTES = 32;
    private static final String TOP = "top";
    private static final String FRONT = "front";
    private static final String SIDE = "side";

    private BlockTextureStore() {
    }

    public static void save(Path folder, String id, BlockFaces faces) throws IOException {
        Files.write(folder.resolve(fileName(id, TOP)), faces.top());
        Files.write(folder.resolve(fileName(id, FRONT)), faces.front());
        Files.write(folder.resolve(fileName(id, SIDE)), faces.side());
    }

    static String fileName(String id, String face) {
        return id + "#" + face + ".png";
    }

    /** Only ids with all three faces are returned. */
    public static Map<String, BlockFaces> loadAll(Path folder) throws IOException {
        Map<String, Map<String, byte[]>> found = new HashMap<>();
        try (Stream<Path> files = Files.list(folder)) {
            for (Path file : (Iterable<Path>) files::iterator) {
                String name = file.getFileName().toString();
                int separator = name.lastIndexOf('#');
                if (separator <= 0 || !name.endsWith(".png")) {
                    continue;
                }
                String id = name.substring(0, separator);
                String face = name.substring(separator + 1, name.length() - ".png".length());
                found.computeIfAbsent(id, key -> new HashMap<>()).put(face, Files.readAllBytes(file));
            }
        }
        Map<String, BlockFaces> faces = new HashMap<>();
        found.forEach((id, byFace) -> {
            if (byFace.containsKey(TOP) && byFace.containsKey(FRONT) && byFace.containsKey(SIDE)) {
                faces.put(id, new BlockFaces(byFace.get(TOP), byFace.get(FRONT), byFace.get(SIDE)));
            }
        });
        return faces;
    }

    public static CompletableFuture<Map<String, BlockFaces>> loadAllAsync(Path folder) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return loadAll(folder);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    /** Splits faces into groups that each fit in one packet. A group holds at least one block, whatever its size. */
    public static List<Map<String, BlockFaces>> batches(Map<String, BlockFaces> all, int maxBytes) {
        List<Map<String, BlockFaces>> batches = new ArrayList<>();
        Map<String, BlockFaces> current = new LinkedHashMap<>();
        int currentBytes = 0;
        for (Map.Entry<String, BlockFaces> entry : all.entrySet()) {
            int bytes = encodedSize(entry.getKey(), entry.getValue());
            if (!current.isEmpty() && currentBytes + bytes > maxBytes) {
                batches.add(current);
                current = new LinkedHashMap<>();
                currentBytes = 0;
            }
            current.put(entry.getKey(), entry.getValue());
            currentBytes += bytes;
        }
        if (!current.isEmpty()) {
            batches.add(current);
        }
        return batches;
    }

    private static int encodedSize(String id, BlockFaces faces) {
        return id.getBytes(StandardCharsets.UTF_8).length + faces.byteSize() + ENTRY_OVERHEAD_BYTES;
    }
}
