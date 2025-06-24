package com.watermelon0117.aicraft;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.io.File;

public class FileUtil {

    public static File getTextureFolder(){
        File folder = new File(Minecraft.getInstance().getSingleplayerServer().getWorldPath(LevelResource.ROOT) + "\\aicraft\\textures");
        return ensureExist(folder);
    }
    public static File getItemTagFolder(){
        File folder = new File(ServerLifecycleHooks.getCurrentServer().getWorldPath(LevelResource.ROOT) + "\\aicraft\\tags");
        return ensureExist(folder);
    }
    public static File getRecipeFile(){
        File folder = new File(ServerLifecycleHooks.getCurrentServer().getWorldPath(LevelResource.ROOT) + "\\aicraft");
        ensureExist(folder);
        return new File(folder, "recipes.txt");
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
