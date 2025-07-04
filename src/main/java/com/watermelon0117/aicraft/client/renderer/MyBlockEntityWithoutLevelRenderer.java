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

import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.*;

public final class MyBlockEntityWithoutLevelRenderer extends BlockEntityWithoutLevelRenderer {
    private static final String FALLBACK_ID = "default";
    private final Map<String, DynamicItemInstance> sprites = new HashMap<>();

    public MyBlockEntityWithoutLevelRenderer() {
        super(null, null);
        sprites.put(FALLBACK_ID, new DynamicItemInstance(createFallbackImage()));
    }

    /* -------------------------------------------------------------------------
     *  Packets → sprites
     * ---------------------------------------------------------------------- */
    public void loadFromPacket(Map<String, byte[]> payload) {
        payload.forEach(this::addFromPacket);
    }

    public void addFromPacket(String id, byte[] pngBytes) {
        sprites.compute(id, (k, old) -> {
            try {
                if (old != null) old.close();
            } catch (Exception ignored) {
            }
            return new DynamicItemInstance(readPng(pngBytes));
        });
    }

    @Override
    public void renderByItem(ItemStack stack, ItemTransforms.TransformType ctx,
                             PoseStack poseStack, MultiBufferSource buffers,
                             int light, int overlay) {
        String id = Optional.ofNullable(MainItem.getID(stack)).orElse(FALLBACK_ID);
        DynamicItemInstance sprite = sprites.getOrDefault(id, sprites.get(FALLBACK_ID));
        sprite.draw(poseStack, buffers, light);
    }

    public ResourceLocation getTexture(ItemStack stack){
        String id = Optional.ofNullable(MainItem.getID(stack)).orElse(FALLBACK_ID);
        DynamicItemInstance sprite = sprites.getOrDefault(id, sprites.get(FALLBACK_ID));
        return sprite.rl;
    }

    /* -------------------------------------------------------------------------
     *  Utilities
     * ---------------------------------------------------------------------- */

    private static BufferedImage readPng(byte[] data) {
        try (var in = new ByteArrayInputStream(data)) {
            return Optional.ofNullable(ImageIO.read(in)).orElseGet(MyBlockEntityWithoutLevelRenderer::createFallbackImage);
        } catch (IOException ex) {
            ex.printStackTrace();
            return createFallbackImage();
        }
    }

    private static BufferedImage createFallbackImage() {
        int DEFAULT_IMG_SIZE = 16;
        BufferedImage img = new BufferedImage(DEFAULT_IMG_SIZE, DEFAULT_IMG_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, DEFAULT_IMG_SIZE, DEFAULT_IMG_SIZE);
        g.dispose();
        return img;
    }

    private static void vertex(VertexConsumer vc,
                               Matrix4f poseMat,
                               Matrix3f normalMat,
                               float x, float y, float z,
                               float u, float v,
                               int uv2,
                               float nx, float ny, float nz) {
        vc.vertex(poseMat, x, y, z)
                .color(255, 255, 255, 255)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(uv2)
                .normal(normalMat, nx, ny, nz)
                .endVertex();
    }

    @OnlyIn(Dist.CLIENT)
    public static final class DynamicItemInstance implements AutoCloseable {
        public final DynamicTexture texture;
        public ResourceLocation rl;
        public RenderType renderType;
        private boolean dirty = true;
        public final int size;

        DynamicItemInstance(BufferedImage image) {
            this.texture = new DynamicTexture(image.getWidth(), image.getHeight(), true);
            size = image.getWidth();
            for (int x = 0; x < size; ++x) {
                for (int y = 0; y < size; ++y) {
                    int argb = image.getRGB(x, y);
                    int swapped = ((argb & 0x00ff0000) >> 16)
                            | (argb & 0xff00ff00)
                            | ((argb & 0x000000ff) << 16);
                    texture.getPixels().setPixelRGBA(x, size - 1 - y, swapped);
                }
            }
        }

        private void updateTexture() {
            rl = Minecraft.getInstance().getTextureManager()
                    .register("dynamic_map/" + UUID.randomUUID(), texture);
            this.renderType = RenderType.entityCutout(rl);
            texture.upload();
        }

        void draw(PoseStack poseStack, MultiBufferSource source, int uv2) {
            if (dirty) {
                updateTexture();
                dirty = false;
            }
            poseStack.pushPose();
            Matrix4f mat = poseStack.last().pose();
            Matrix3f nMat = poseStack.last().normal();
            VertexConsumer vc = source.getBuffer(this.renderType);

            float h = 1.0F / size;
            float th2 = Math.max(1.0F / size / 2.0F, 1.0F / 32.0F / 2.0F);  //control thickness to be max(1/size, 1/32) voxel

            // front (+Z)
            vertex(vc, mat, nMat, 0, 0, 0.5f + th2, 0, 0, uv2, 0, 0, 1);
            vertex(vc, mat, nMat, 1, 0, 0.5f + th2, 1, 0, uv2, 0, 0, 1);
            vertex(vc, mat, nMat, 1, 1, 0.5f + th2, 1, 1, uv2, 0, 0, 1);
            vertex(vc, mat, nMat, 0, 1, 0.5f + th2, 0, 1, uv2, 0, 0, 1);
            // back (–Z)
            vertex(vc, mat, nMat, 0, 1, 0.5f - th2, 0, 1, uv2, 0, 0, -1);
            vertex(vc, mat, nMat, 1, 1, 0.5f - th2, 1, 1, uv2, 0, 0, -1);
            vertex(vc, mat, nMat, 1, 0, 0.5f - th2, 1, 0, uv2, 0, 0, -1);
            vertex(vc, mat, nMat, 0, 0, 0.5f - th2, 0, 0, uv2, 0, 0, -1);
            for (int i = 0; i < size; i++) {
                float y = (float) i / size;                 // bottom face  (-Y)
                vertex(vc, mat, nMat, 0, y, 0.5f - th2, 0, y, uv2, 0, -1, 0);
                vertex(vc, mat, nMat, 1, y, 0.5f - th2, 1, y, uv2, 0, -1, 0);
                vertex(vc, mat, nMat, 1, y, 0.5f + th2, 1, y + h, uv2, 0, -1, 0);
                vertex(vc, mat, nMat, 0, y, 0.5f + th2, 0, y + h, uv2, 0, -1, 0);
            }
            for (int i = 0; i < size; i++) {
                float y = (float) (i + 1) / size;           // top face (+Y)
                vertex(vc, mat, nMat, 0, y, 0.5f + th2, 0, y, uv2, 0, 1, 0);
                vertex(vc, mat, nMat, 1, y, 0.5f + th2, 1, y, uv2, 0, 1, 0);
                vertex(vc, mat, nMat, 1, y, 0.5f - th2, 1, y - h, uv2, 0, 1, 0);
                vertex(vc, mat, nMat, 0, y, 0.5f - th2, 0, y - h, uv2, 0, 1, 0);
            }
            for (int i = 0; i < size; i++) {
                float x = (float) i / size;                 // west face (-X)
                vertex(vc, mat, nMat, x, 0, 0.5f + th2, x, 0, uv2, -1, 0, 0);
                vertex(vc, mat, nMat, x, 1, 0.5f + th2, x, 1, uv2, -1, 0, 0);
                vertex(vc, mat, nMat, x, 1, 0.5f - th2, x + h, 1, uv2, -1, 0, 0);
                vertex(vc, mat, nMat, x, 0, 0.5f - th2, x + h, 0, uv2, -1, 0, 0);
            }
            for (int i = 0; i < size; i++) {
                float x = (float) (i + 1) / size;           // east face (+X)
                vertex(vc, mat, nMat, x, 0, 0.5f - th2, x, 0, uv2, 1, 0, 0);
                vertex(vc, mat, nMat, x, 1, 0.5f - th2, x, 1, uv2, 1, 0, 0);
                vertex(vc, mat, nMat, x, 1, 0.5f + th2, x - h, 1, uv2, 1, 0, 0);
                vertex(vc, mat, nMat, x, 0, 0.5f + th2, x - h, 0, uv2, 1, 0, 0);
            }
            poseStack.popPose();
        }

        @Override
        public void close() {
            texture.close();
        }
    }
}
