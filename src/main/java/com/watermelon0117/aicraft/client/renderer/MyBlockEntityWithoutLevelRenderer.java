package com.watermelon0117.aicraft.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix3f;
import com.mojang.math.Matrix4f;
import com.watermelon0117.aicraft.common.FileUtil;
import com.watermelon0117.aicraft.items.MainItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class MyBlockEntityWithoutLevelRenderer extends BlockEntityWithoutLevelRenderer {
    public final Map<String, DynamicItemInstance> maps = new HashMap<>();

    public MyBlockEntityWithoutLevelRenderer() { super(null, null); }

    public void loadFromFile() {
        File folder = FileUtil.getTextureFolder();
        File[] files = folder.listFiles();

        for (File file : files) {
            if (file.isFile()) {
                try {
                    BufferedImage image = ImageIO.read(file);
                    if (image != null) {
                        String name = file.getName().replace(".png", "");
                        System.out.printf("Loaded image: %s(%dx%d)\n", name, image.getWidth(), image.getHeight());
                        this.maps.put(name, new DynamicItemInstance(image));
                    } else {
                        System.out.println("Skipped (not an image): " + file.getName());
                    }
                } catch (IOException e) {
                    System.out.println("Error reading file: " + file.getName());
                    e.printStackTrace();
                }
            }
        }
    }

    public void loadNewFile(String name) {
        File file = new File(FileUtil.getTextureFolder(), name + ".png");
        System.out.println("loadNewFile");
        System.out.println(file);
        if (file.isFile()) {
            try {
                BufferedImage image = ImageIO.read(file);
                if (image != null) {
                    System.out.println("Loaded image: " + name +
                            " (" + image.getWidth() + "x" + image.getHeight() + ")");
                    this.maps.put(name, new DynamicItemInstance(image));
                } else {
                    System.out.println("Skipped (not an image): " + file.getName());
                }
            } catch (IOException e) {
                System.out.println("Error reading file: " + file.getName());
                e.printStackTrace();
            }
        }
    }
    public void loadFromPacket(Map<String, byte[]> payload){
        for (Map.Entry<String, byte[]> entry : payload.entrySet()) {
            String key = entry.getKey();
            byte[] value = entry.getValue();
            maps.put(key, new DynamicItemInstance(fromBytes(value)));
        }
    }
    public void addFromPacket(String id,byte[] data){
        maps.put(id, new DynamicItemInstance(fromBytes(data)));
    }
    private static BufferedImage fromBytes(byte[] imageBytes){
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(imageBytes);
            BufferedImage image = ImageIO.read(bais);
            if (image == null)
                System.out.println("Failed to decode image bytes.");
            return image;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    @Override
    public void renderByItem(ItemStack itemStack, ItemTransforms.TransformType ctx, PoseStack poseStack, MultiBufferSource buffers,
                             int light, int overlay) {
        poseStack.pushPose();
        if (!this.maps.containsKey("default")) {
            BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = img.createGraphics();
            g2d.setColor(new Color(255, 255, 255, 255));  // white
            g2d.fillRect(0, 0, 16, 16);
            g2d.dispose();
            this.maps.put("default", new DynamicItemInstance(img));
        }
        String id = MainItem.getID(itemStack);
        if (id == null || !this.maps.containsKey(id)) {
            id = "default";
        }
        DynamicItemInstance instance = this.maps.get(id);
        instance.draw(poseStack, buffers, false, light);
        poseStack.popPose();
    }

    private static void vertex(VertexConsumer vc,
                               Matrix4f poseMat,
                               Matrix3f normalMat,
                               float x,  float y,  float z,   // position
                               float u,  float v,             // UV
                               int   uv2,                     // lightmap
                               float nx, float ny, float nz)  // normal  ← NEW
    {
        vc.vertex(poseMat, x, y, z)
                .color(255, 255, 255, 255)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(uv2)
                .normal(normalMat, nx, ny, nz)
                .endVertex();
    }

    @OnlyIn(Dist.CLIENT)
    public class DynamicItemInstance implements AutoCloseable {
        private BufferedImage image;
        private DynamicTexture texture;
        private RenderType renderType;
        private boolean requiresUpload = true;

        DynamicItemInstance(BufferedImage image) {
            this.image = image;
        }

        private void updateTexture() {
            if (this.texture == null) {
                this.texture = new DynamicTexture(image.getWidth(), image.getWidth(), true);
                ResourceLocation resourcelocation = Minecraft.getInstance().getTextureManager().register("map/" + 0, this.texture);
                this.renderType = RenderType.entityCutout(resourcelocation);
            }
            for (int i = 0; i < image.getWidth(); ++i) {
                for (int j = 0; j < image.getWidth(); ++j) {
                    int color = this.image.getRGB(i, j);
                    int red = (color & 0x00ff0000) >> 16;
                    int blue = (color & 0x000000ff) << 16;
                    color = (color & 0xff00ff00) | red | blue;
                    this.texture.getPixels().setPixelRGBA(i, image.getWidth() - j - 1, color);
                }
            }
            this.texture.upload();
        }

        void draw(PoseStack p_93292_, MultiBufferSource p_93293_, boolean p_93294_, int uv2) {
            if (this.requiresUpload) {
                this.updateTexture();
                this.requiresUpload = false;
            }
            p_93292_.pushPose();
            PoseStack.Pose pose = p_93292_.last();
            Matrix4f mat   = pose.pose();
            Matrix3f nMat  = pose.normal();        // ← pass this to every call
            VertexConsumer vc = p_93293_.getBuffer(this.renderType);

            int grid = this.image.getHeight();
            float h = 1.0F / grid;
            float th=grid;
            float th2 = Math.max(1.0F / th / 2.0F, 1.0F / 32.0F / 2.0F);
            //RenderSystem.disableCull();
            // front (+Z)
            vertex(vc, mat, nMat, 0, 0, 0.5f + th2, 0, 0, uv2, 0, 0,  1);
            vertex(vc, mat, nMat, 1, 0, 0.5f + th2, 1, 0, uv2, 0, 0,  1);
            vertex(vc, mat, nMat, 1, 1, 0.5f + th2, 1, 1, uv2, 0, 0,  1);
            vertex(vc, mat, nMat, 0, 1, 0.5f + th2, 0, 1, uv2, 0, 0,  1);

            // back (–Z)
            vertex(vc, mat, nMat, 0, 1, 0.5f - th2, 0, 1, uv2, 0, 0, -1);
            vertex(vc, mat, nMat, 1, 1, 0.5f - th2, 1, 1, uv2, 0, 0, -1);
            vertex(vc, mat, nMat, 1, 0, 0.5f - th2, 1, 0, uv2, 0, 0, -1);
            vertex(vc, mat, nMat, 0, 0, 0.5f - th2, 0, 0, uv2, 0, 0, -1);

            for (int i = 0; i < grid; i++) {
                float y = (float) i / grid;                 // bottom face  (-Y)
                vertex(vc, mat, nMat, 0, y, 0.5f - th2, 0, y,       uv2, 0, -1, 0);
                vertex(vc, mat, nMat, 1, y, 0.5f - th2, 1, y,       uv2, 0, -1, 0);
                vertex(vc, mat, nMat, 1, y, 0.5f + th2, 1, y + h,   uv2, 0, -1, 0);
                vertex(vc, mat, nMat, 0, y, 0.5f + th2, 0, y + h,   uv2, 0, -1, 0);
            }
            for (int i = 0; i < grid; i++) {
                float y = (float) (i + 1) / grid;           // top face (+Y)
                vertex(vc, mat, nMat, 0, y, 0.5f + th2, 0, y,       uv2, 0,  1, 0);
                vertex(vc, mat, nMat, 1, y, 0.5f + th2, 1, y,       uv2, 0,  1, 0);
                vertex(vc, mat, nMat, 1, y, 0.5f - th2, 1, y - h,   uv2, 0,  1, 0);
                vertex(vc, mat, nMat, 0, y, 0.5f - th2, 0, y - h,   uv2, 0,  1, 0);
            }
            for (int i = 0; i < grid; i++) {
                float x = (float) i / grid;                 // west face (-X)
                vertex(vc, mat, nMat, x, 0, 0.5f + th2, x, 0,       uv2, -1, 0, 0);
                vertex(vc, mat, nMat, x, 1, 0.5f + th2, x, 1,       uv2, -1, 0, 0);
                vertex(vc, mat, nMat, x, 1, 0.5f - th2, x + h, 1,  uv2, -1, 0, 0);
                vertex(vc, mat, nMat, x, 0, 0.5f - th2, x + h, 0,  uv2, -1, 0, 0);
            }
            for (int i = 0; i < grid; i++) {
                float x = (float) (i + 1) / grid;           // east face (+X)
                vertex(vc, mat, nMat, x, 0, 0.5f - th2, x, 0,       uv2, 1, 0, 0);
                vertex(vc, mat, nMat, x, 1, 0.5f - th2, x, 1,       uv2, 1, 0, 0);
                vertex(vc, mat, nMat, x, 1, 0.5f + th2, x - h, 1,  uv2, 1, 0, 0);
                vertex(vc, mat, nMat, x, 0, 0.5f + th2, x - h, 0,  uv2, 1, 0, 0);
            }

            p_93292_.popPose();
        }

        public void close() {
            this.texture.close();
        }
    }
}
