package com.watermelon0117.aicraft.common;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.ToLongFunction;

/**
 * The order AI items were made in. Worlds from before it was saved have only their texture files to go by, and those keep
 * the time they were first written: regenerating overwrites the same file, which NTFS leaves the creation time of alone.
 */
public final class CreationOrder {
    private static final String[] BLOCK_FACES = {"top", "front", "side"};

    private CreationOrder() {
    }

    /**
     * @param saved the ids as last saved, oldest first; empty for a world from before the order was saved
     * @return the ids oldest first: those in {@code saved} as saved, then the rest by {@code firstFileTime} and then by id.
     * The rest go after because only an older copy of the mod, which drops the saved order, makes items without a place in it.
     */
    public static List<String> oldestFirst(Collection<String> ids, List<String> saved, ToLongFunction<String> firstFileTime) {
        Set<String> remaining = new LinkedHashSet<>(ids);
        List<String> ordered = new ArrayList<>(ids.size());
        for (String id : saved) {
            if (remaining.remove(id)) {
                ordered.add(id);
            }
        }
        // Each time reads files, so it is read once per id rather than once per comparison.
        Map<String, Long> times = new HashMap<>();
        remaining.forEach(id -> times.put(id, firstFileTime.applyAsLong(id)));
        remaining.stream()
                .sorted(Comparator.comparingLong((String id) -> times.get(id)).thenComparing(Comparator.naturalOrder()))
                .forEach(ordered::add);
        return ordered;
    }

    /**
     * @param aicraftFolder the world's {@code aicraft} folder
     * @return the earliest creation time, in milliseconds, of the id's item picture and block faces, or 0 when it has none.
     * Java 17 on Linux gives the last modified time instead, which is when the item was last regenerated.
     */
    public static long firstFileTime(Path aicraftFolder, String id) {
        long earliest = Long.MAX_VALUE;
        List<Path> files = new ArrayList<>();
        files.add(aicraftFolder.resolve("textures").resolve(id + ".png"));
        for (String face : BLOCK_FACES) {
            files.add(aicraftFolder.resolve("block_textures").resolve(BlockTextureStore.fileName(id, face)));
        }
        for (Path file : files) {
            if (!Files.isRegularFile(file)) {
                continue;
            }
            try {
                earliest = Math.min(earliest, Files.readAttributes(file, BasicFileAttributes.class).creationTime().toMillis());
            } catch (IOException | RuntimeException ignored) {
                // Only a guess at an old item's place rests on this, which is not worth failing a world's load over.
            }
        }
        return earliest == Long.MAX_VALUE ? 0 : earliest;
    }
}
