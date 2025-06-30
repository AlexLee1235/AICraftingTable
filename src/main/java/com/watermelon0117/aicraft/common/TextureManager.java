package com.watermelon0117.aicraft.common;

import com.watermelon0117.aicraft.client.renderer.MyBlockEntityWithoutLevelRenderer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

public class TextureManager {
    public static Map<String, byte[]> loadFromFile() {
        Map<String, byte[]> map=new HashMap<>();
        File folder = FileUtil.getTextureFolder();
        File[] files = folder.listFiles();

        for (File file : files) {
            if (file.isFile()) {
                try {
                    byte[] data = Files.readAllBytes(file.toPath());
                    String name = file.getName().replace(".png", "");
                    map.put(name, data);
                } catch (IOException e) {
                    System.out.println("Error reading file: " + file.getName());
                    e.printStackTrace();
                }
            }
        }
        return map;
    }
}
