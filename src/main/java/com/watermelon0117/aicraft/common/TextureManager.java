package com.watermelon0117.aicraft.common;

import com.watermelon0117.aicraft.client.renderer.MyBlockEntityWithoutLevelRenderer;
import com.watermelon0117.aicraft.network.CAddTexturePacket;
import com.watermelon0117.aicraft.network.PacketHandler;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class TextureManager {
    public static CompletableFuture<Map<String, byte[]>> loadFromFileAsync() {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, byte[]> map = new HashMap<>();
            File folder = FileUtil.getTextureFolder();
            File[] files = folder.listFiles();

            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        try {
                            byte[] data = Files.readAllBytes(file.toPath());
                            String name = file.getName().replace(".png", "");
                            map.put(name, data);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

            return map;
        });
    }

    private static String getCurrentDateTime() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
        return now.format(formatter);
    }

    private static byte[] toBytes(BufferedImage txt) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(txt, "png", baos);
        baos.flush(); // ensure all bytes are written
        byte[] bytes2 = baos.toByteArray();
        baos.close();
        return bytes2;
    }

    public static byte[] applyTexture(byte[] bytes, String id) {
        try {
            Files.write(FileUtil.getArchiveFolder(id + "_" + getCurrentDateTime() + ".jpeg").toPath(), bytes);
            BufferedImage txt = ImageGridProcessor.process(ImageGridProcessor.readImageFromBytes(bytes), false);
            ImageGridProcessor.saveImage(txt, new File(FileUtil.getTextureFolder(), id + ".png"));
            return toBytes(txt);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
