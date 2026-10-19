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
import java.util.Optional;
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
            BufferedImage image = ImageGridProcessor.readImageFromBytes(bytes);
            // Transparent images come from the own-key Sunburst request; the texture server returns opaque ones.
            BufferedImage txt = hasTransparency(image) ? pixelize(image) : null;
            if (txt == null)
                txt = ImageGridProcessor.process(image, false);
            ImageGridProcessor.saveImage(txt, new File(FileUtil.getTextureFolder(), id + ".png"));
            return toBytes(txt);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean hasTransparency(BufferedImage image) {
        if (image == null || !image.getColorModel().hasAlpha())
            return false;
        int[] argb = image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
        for (int p : argb) {
            if ((p >>> 24) < 255)
                return true;
        }
        return false;
    }

    /**
     * Pixelates a transparent model image with SpritePixelizer, then crops and pads the sprite
     * the way ImageGridProcessor does, so own-key textures match the texture server's.
     * Returns null when no pixel grid is found.
     */
    private static BufferedImage pixelize(BufferedImage image) {
        Optional<SpritePixelizer.Result> result = SpritePixelizer.pixelize(image);
        if (result.isEmpty()) {
            System.out.println("No pixel grid found, falling back to ImageGridProcessor");
            return null;
        }
        BufferedImage sprite = result.get().sprite();
        int minX = sprite.getWidth(), minY = sprite.getHeight(), maxX = -1, maxY = -1;
        for (int y = 0; y < sprite.getHeight(); y++) {
            for (int x = 0; x < sprite.getWidth(); x++) {
                if ((sprite.getRGB(x, y) >>> 24) != 0) {
                    minX = Math.min(minX, x);
                    maxX = Math.max(maxX, x);
                    minY = Math.min(minY, y);
                    maxY = Math.max(maxY, y);
                }
            }
        }
        if (maxX < 0)
            return null;
        int w = maxX - minX + 1, h = maxY - minY + 1;
        BufferedImage content = sprite.getSubimage(minX, minY, w, h);
        return ImageGridProcessor.padImageCentered(content, ImageGridProcessor.calcPaddingSize(w, h, 1));
    }
}
