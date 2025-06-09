package com.watermelon0117.aicraft;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix4f;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.MaterialColor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.awt.*;
import java.awt.image.BufferedImage;

public class MyBlockEntityWithoutLevelRenderer extends BlockEntityWithoutLevelRenderer {
    private final Int2ObjectMap<DynamicItemInstance> maps = new Int2ObjectOpenHashMap<>();
    public MyBlockEntityWithoutLevelRenderer() {
        super(null,null);
    }
    public void loadFromFile(){

    }
    @Override
    public void renderByItem(ItemStack itemStack,ItemTransforms.TransformType ctx,PoseStack poseStack,MultiBufferSource buffers,
                             int light, int overlay) {
        poseStack.pushPose();
        if(!this.maps.containsKey(0)){
            BufferedImage img = new BufferedImage(128,128, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = img.createGraphics();
            g2d.setColor(new Color(255, 255, 255, 255));  // white
            g2d.fillRect(0, 0, 128, 128);
            g2d.dispose();
            this.maps.put(0, new DynamicItemInstance(img));
        }
        DynamicItemInstance instance=this.maps.get(0);
        instance.draw(poseStack, buffers, false, light);
        poseStack.popPose();
    }

    public void update(int key, BufferedImage img) {
        this.maps.get(key).replaceMapData(img);
    }

    @OnlyIn(Dist.CLIENT)
    class DynamicItemInstance implements AutoCloseable {
        private BufferedImage image;
        private final DynamicTexture texture;
        private final RenderType renderType;
        private boolean requiresUpload = true;
        DynamicItemInstance(BufferedImage image) {
            this.image = image;
            this.texture = new DynamicTexture(image.getHeight(), image.getWidth(), true);
            ResourceLocation resourcelocation = Minecraft.getInstance().getTextureManager().register("map/" + 0, this.texture);
            this.renderType = RenderType.text(resourcelocation);
        }
        void replaceMapData(BufferedImage image) {
            this.image=image;
            this.requiresUpload = true;
        }
        public void forceUpload() {
            this.requiresUpload = true;
        }
        private void updateTexture() {
            for(int i = 0; i < 128; ++i) {
                for(int j = 0; j < 128; ++j) {
                    int color = this.image.getRGB(i * image.getWidth() / 128, image.getHeight() - 1 - j * image.getHeight() / 128);
                    int red = (color & 0x00ff0000) >> 16;
                    int blue = (color & 0x000000ff) << 16;
                    color = (color & 0xff00ff00) | red | blue;
                    this.texture.getPixels().setPixelRGBA(i, j, color);
                }
            }
            this.texture.upload();
        }
        void draw(PoseStack p_93292_, MultiBufferSource p_93293_, boolean p_93294_, int p_93295_) {
            if (this.requiresUpload) {
                this.updateTexture();
                this.requiresUpload = false;
            }
            p_93292_.pushPose();
            Matrix4f matrix4f = p_93292_.last().pose();
            VertexConsumer vertexconsumer = p_93293_.getBuffer(this.renderType);

            float z=0.5f;
            vertexconsumer.vertex(matrix4f, 0.0F, 0.0F, z).color(255, 255, 255, 255).uv(0.0F, 0.0F).uv2(p_93295_).endVertex();
            vertexconsumer.vertex(matrix4f, 1.0F, 0.0F, z).color(255, 255, 255, 255).uv(1.0F, 0.0F).uv2(p_93295_).endVertex();
            vertexconsumer.vertex(matrix4f, 1.0F, 1.0F, z).color(255, 255, 255, 255).uv(1.0F, 1.0F).uv2(p_93295_).endVertex();
            vertexconsumer.vertex(matrix4f, 0.0F, 1.0F, z).color(255, 255, 255, 255).uv(0.0F, 1.0F).uv2(p_93295_).endVertex();
            vertexconsumer.vertex(matrix4f, 0.0F, 1.0F, z).color(255, 255, 255, 255).uv(0.0F, 1.0F).uv2(p_93295_).endVertex();
            vertexconsumer.vertex(matrix4f, 1.0F, 1.0F, z).color(255, 255, 255, 255).uv(1.0F, 1.0F).uv2(p_93295_).endVertex();
            vertexconsumer.vertex(matrix4f, 1.0F, 0.0F, z).color(255, 255, 255, 255).uv(1.0F, 0.0F).uv2(p_93295_).endVertex();
            vertexconsumer.vertex(matrix4f, 0.0F, 0.0F, z).color(255, 255, 255, 255).uv(0.0F, 0.0F).uv2(p_93295_).endVertex();

            p_93292_.popPose();
        }
        public void close() {
            this.texture.close();
        }
    }
}
