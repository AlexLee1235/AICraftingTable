package com.watermelon0117.aicraft;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix4f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class MyBlockEntityWithoutLevelRenderer extends BlockEntityWithoutLevelRenderer {
    public final Map<String, DynamicItemInstance> maps = new HashMap<>();
    public MyBlockEntityWithoutLevelRenderer() {
        super(null,null);
    }
    public void loadFromFile(){
        File folder = new File("C:\\achieve\\AICraftingTable\\temp");
        File[] files = folder.listFiles();
        if (files == null) {
            System.out.println("Folder not found or empty.");
            return;
        }

        for (File file : files) {
            if (file.isFile()) {
                try {
                    BufferedImage image = ImageIO.read(file);
                    if (image != null) {
                        String name=file.getName().replace(".png","");
                        System.out.println("Loaded image: " + name +
                                " (" + image.getWidth() + "x" + image.getHeight() + ")");
                        this.maps.put(name,new DynamicItemInstance(image));
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
    public void loadNewFile(String name){
        File file=new File("C:\\achieve\\AICraftingTable\\temp\\"+name+".png");
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
    @Override
    public void renderByItem(ItemStack itemStack,ItemTransforms.TransformType ctx,PoseStack poseStack,MultiBufferSource buffers,
                             int light, int overlay) {
        poseStack.pushPose();
        if(!this.maps.containsKey("default")){
            BufferedImage img = new BufferedImage(128,128, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = img.createGraphics();
            g2d.setColor(new Color(255, 255, 255, 255));  // white
            g2d.fillRect(0, 0, 128, 128);
            g2d.dispose();
            this.maps.put("default", new DynamicItemInstance(img));
        }
        CompoundTag tag=itemStack.getTag();
        String id="default";
        if(tag!=null){
            id=tag.getString("texture");
        }
        if(!this.maps.containsKey(id)) {
            System.out.println(id + " not exist");
            id = "default";
        }
        DynamicItemInstance instance=this.maps.get(id);
        instance.draw(poseStack, buffers, false, light);
        poseStack.popPose();
    }
    private static void vertex(VertexConsumer consumer, Matrix4f matrix4f, float x, float y,float z, float u, float v, int uv2){
        consumer.vertex(matrix4f, x,y,z).color(255, 255, 255, 255).uv(u,v).uv2(uv2).endVertex();
    }
    @OnlyIn(Dist.CLIENT)
    class DynamicItemInstance implements AutoCloseable {
        private BufferedImage image;
        private DynamicTexture texture;
        private RenderType renderType;
        private boolean requiresUpload = true;
        DynamicItemInstance(BufferedImage image) {
            this.image = image;
        }
        private void updateTexture() {
            if(this.texture==null) {
                this.texture = new DynamicTexture(image.getWidth(), image.getWidth(), true);
                ResourceLocation resourcelocation = Minecraft.getInstance().getTextureManager().register("map/" + 0, this.texture);
                this.renderType = RenderType.text(resourcelocation);
            }
            for(int i = 0; i < image.getWidth(); ++i) {
                for(int j = 0; j < image.getWidth(); ++j) {
                    int color = this.image.getRGB(i, j);
                    int red = (color & 0x00ff0000) >> 16;
                    int blue = (color & 0x000000ff) << 16;
                    color = (color & 0xff00ff00) | red | blue;
                    this.texture.getPixels().setPixelRGBA(i, image.getWidth()-j-1, color);
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
            Matrix4f mat = p_93292_.last().pose();
            VertexConsumer vc = p_93293_.getBuffer(this.renderType);

            int grid = this.image.getHeight();
            float h = 1.0F / grid;
            float th2 = Math.max(1.0F / grid / 2.0F, 1.0F / 32.0F / 2.0F);
            //RenderSystem.disableCull();
            vertex(vc, mat, 0, 0, 0.5f + th2, 0, 0, uv2);
            vertex(vc, mat, 1, 0, 0.5f + th2, 1, 0, uv2);
            vertex(vc, mat, 1, 1, 0.5f + th2, 1, 1, uv2);
            vertex(vc, mat, 0, 1, 0.5f + th2, 0, 1, uv2);

            vertex(vc, mat, 0, 1, 0.5f - th2, 0, 1, uv2);
            vertex(vc, mat, 1, 1, 0.5f - th2, 1, 1, uv2);
            vertex(vc, mat, 1, 0, 0.5f - th2, 1, 0, uv2);
            vertex(vc, mat, 0, 0, 0.5f - th2, 0, 0, uv2);

            for (int i = 0; i < grid; i++) {
                float y = (float) i / grid;
                vertex(vc, mat, 0, y, 0.5F - th2, 0, y, uv2);
                vertex(vc, mat, 1, y, 0.5F - th2, 1, y, uv2);
                vertex(vc, mat, 1, y, 0.5F + th2, 1, y + h, uv2);
                vertex(vc, mat, 0, y, 0.5F + th2, 0, y + h, uv2);
            }
            for (int i = 0; i < grid; i++) {
                float y = (float) (i + 1) / grid;
                vertex(vc, mat, 0, y, 0.5F + th2, 0, y, uv2);
                vertex(vc, mat, 1, y, 0.5F + th2, 1, y, uv2);
                vertex(vc, mat, 1, y, 0.5F - th2, 1, y - h, uv2);
                vertex(vc, mat, 0, y, 0.5F - th2, 0, y - h, uv2);
            }
            for (int i = 0; i < grid; i++) {
                float x = (float) i / grid;
                vertex(vc, mat, x, 0, 0.5F + th2, x, 0, uv2);
                vertex(vc, mat, x, 1, 0.5F + th2, x, 1, uv2);
                vertex(vc, mat, x, 1, 0.5F - th2, x + h, 1, uv2);
                vertex(vc, mat, x, 0, 0.5F - th2, x + h, 0, uv2);
            }
            for (int i = 0; i < grid; i++) {
                float x = (float) (i + 1) / grid;
                vertex(vc, mat, x, 0, 0.5F - th2, x, 0, uv2);
                vertex(vc, mat, x, 1, 0.5F - th2, x, 1, uv2);
                vertex(vc, mat, x, 1, 0.5F + th2, x - h, 1, uv2);
                vertex(vc, mat, x, 0, 0.5F + th2, x - h, 0, uv2);
            }

            p_93292_.popPose();
        }
        public void close() {
            this.texture.close();
        }
    }
}
