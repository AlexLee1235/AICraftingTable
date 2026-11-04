package com.watermelon0117.aicraft.common;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.io.File;
import java.nio.file.Path;

public class FileUtil {
    public static File getTextureFolder() {
        File folder = new File(ServerLifecycleHooks.getCurrentServer().getWorldPath(LevelResource.ROOT) + "\\aicraft\\textures");
        return ensureExist(folder);
    }

    public static Path getBlockTextureFolder() {
        Path folder = ServerLifecycleHooks.getCurrentServer().getWorldPath(LevelResource.ROOT).resolve("aicraft").resolve("block_textures");
        return ensureExist(folder.toFile()).toPath();
    }

    public static File getTempFolder(String name) {
        File folder = new File("./aicraft/process/");
        ensureExist(folder);
        return new File(folder, name);
    }

    public static File getArchiveFolder(String name) {
        File folder = new File("./aicraft/images/");
        ensureExist(folder);
        return new File(folder, name);
    }

    private static File ensureExist(File folder) {
        if (!folder.exists()) {
            if (!folder.mkdirs()) {
                throw new RuntimeException("Failed to create directory: " + folder);
            }
        } else if (!folder.isDirectory()) {
            throw new RuntimeException("Path exists but is not a directory: " + folder);
        }
        return folder;
    }
}
