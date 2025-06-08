package com.watermelon0117.aicraft;

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

public class MyBlockEntityWithoutLevelRenderer extends BlockEntityWithoutLevelRenderer {
    private final Int2ObjectMap<DynamicItemInstance> maps = new Int2ObjectOpenHashMap<>();
    public MyBlockEntityWithoutLevelRenderer() {
        super(null,null);
    }

    @Override
    public void renderByItem(ItemStack stack,ItemTransforms.TransformType ctx,PoseStack pose,MultiBufferSource buffers,
                             int light, int overlay) {
        pose.pushPose();

        byte[] colours = new byte[128 * 128];
        for (int i = 0; i < 128*128; i++) {
            colours[i]= (byte) (i%128);
        }

        render(pose, buffers, 0, colours, false, light);
        pose.popPose();
    }

    public void update(int p_168766_, byte[] p_168767_) {
        this.getOrCreateMapInstance(p_168766_, p_168767_).forceUpload();
    }
    public void render(PoseStack stack, MultiBufferSource bufferSource, int key, byte[] data, boolean p_168776_, int p_168777_) {
        DynamicItemInstance instance=this.getOrCreateMapInstance(key, data);
        //System.out.println(instance.renderType.toString());
        instance.draw(stack, bufferSource, p_168776_, p_168777_);
    }
    private DynamicItemInstance getOrCreateMapInstance(int key, byte[] new_data) {
        return this.maps.compute(key, (id, val) -> {
            if (val == null) {
                return new DynamicItemInstance(id, new_data);
            } else {
                val.replaceMapData(new_data);
                return val;
            }
        });
    }
    public void resetData() {
        for(DynamicItemInstance maprenderer$mapinstance : this.maps.values()) {
            maprenderer$mapinstance.close();
        }
        this.maps.clear();
    }
    public void close() {
        this.resetData();
    }
    @OnlyIn(Dist.CLIENT)
    class DynamicItemInstance implements AutoCloseable {
        private byte[] colors;
        private final DynamicTexture texture;
        private final RenderType renderType;
        private boolean requiresUpload = true;
        DynamicItemInstance(int p_168783_, byte[] colors) {
            this.colors = colors;
            this.texture = new DynamicTexture(128, 128, true);
            ResourceLocation resourcelocation = Minecraft.getInstance().getTextureManager().register("map/" + p_168783_, this.texture);
            this.renderType = RenderType.text(resourcelocation);
        }
        void replaceMapData(byte[] colors) {
            this.colors=colors;
            this.requiresUpload = true;
        }
        public void forceUpload() {
            this.requiresUpload = true;
        }
        private void updateTexture() {
            for(int i = 0; i < 128; ++i) {
                for(int j = 0; j < 128; ++j) {
                    int k = j + i * 128;
                    this.texture.getPixels().setPixelRGBA(j, i, MaterialColor.getColorFromPackedId(this.colors[k]));
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
