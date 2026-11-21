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
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * Worn armor pictures live in their own folder as {@code <id>.png} and {@code <id>.json}, apart from the item icons: TextureManager
 * treats every file in the item folder as an icon. Kept the way {@link BlockTextureStore} keeps block faces.
 */
public final class ArmorTextureStore {
    /** A custom payload may not exceed 1,048,576 bytes, and the constructor that checks it throws. */
    public static final int MAX_BATCH_BYTES = 1_000_000;
    private static final int ENTRY_OVERHEAD_BYTES = 32;
    private static final String PNG = ".png";
    private static final String JSON = ".json";

    private ArmorTextureStore() {
    }

    /** The pieces of the folder last asked for, so each login does not read every file again. {@link #save} keeps it current. */
    private static Path cachedFolder;
    private static CompletableFuture<Map<String, ArmorTexture>> cached;

    public static void save(Path folder, String id, ArmorTexture texture) throws IOException {
        Files.write(folder.resolve(id + PNG), texture.png());
        Files.writeString(folder.resolve(id + JSON), texture.uv(), StandardCharsets.UTF_8);
        synchronized (ArmorTextureStore.class) {
            if (cached != null && folder.equals(cachedFolder)) {
                // After the files are written, and chained, as in BlockTextureStore.save.
                cached = cached.thenApply(all -> {
                    all.put(id, texture);
                    return all;
                });
            }
        }
    }

    /** Forgets the pieces kept by {@link #loadAllAsync}, for a server that starts: the folder may have changed while none ran. */
    public static synchronized void clearCache() {
        cachedFolder = null;
        cached = null;
    }

    /** Only ids with both files are returned. */
    public static Map<String, ArmorTexture> loadAll(Path folder) throws IOException {
        Map<String, ArmorTexture> found = new HashMap<>();
        try (Stream<Path> files = Files.list(folder)) {
            for (Path file : (Iterable<Path>) files::iterator) {
                String name = file.getFileName().toString();
                if (!name.endsWith(PNG)) {
                    continue;
                }
                String id = name.substring(0, name.length() - PNG.length());
                Path table = folder.resolve(id + JSON);
                if (!id.isEmpty() && Files.isRegularFile(table)) {
                    found.put(id, new ArmorTexture(Files.readAllBytes(file), Files.readString(table, StandardCharsets.UTF_8)));
                }
            }
        }
        return found;
    }

    /** Reads the folder the first time and answers from memory after that, until {@link #clearCache}. A failed read is tried again. */
    public static synchronized CompletableFuture<Map<String, ArmorTexture>> loadAllAsync(Path folder) {
        if (cached != null && folder.equals(cachedFolder) && !cached.isCompletedExceptionally()) {
            return cached;
        }
        cachedFolder = folder;
        cached = CompletableFuture.supplyAsync(() -> {
            try {
                // Concurrent, as save adds to it on the server thread while a login may be reading it.
                return new ConcurrentHashMap<>(loadAll(folder));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        return cached;
    }

    /** Splits pieces into groups that each fit in one packet. A group holds at least one piece, whatever its size. */
    public static List<Map<String, ArmorTexture>> batches(Map<String, ArmorTexture> all, int maxBytes) {
        List<Map<String, ArmorTexture>> batches = new ArrayList<>();
        Map<String, ArmorTexture> current = new LinkedHashMap<>();
        int currentBytes = 0;
        for (Map.Entry<String, ArmorTexture> entry : all.entrySet()) {
            int bytes = entry.getKey().getBytes(StandardCharsets.UTF_8).length + entry.getValue().byteSize() + ENTRY_OVERHEAD_BYTES;
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
}
