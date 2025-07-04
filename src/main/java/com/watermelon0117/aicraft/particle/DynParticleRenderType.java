package com.watermelon0117.aicraft.particle;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.watermelon0117.aicraft.items.MainItem;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;

public final class DynParticleRenderType {

    public static final ParticleRenderType INSTANCE = new ParticleRenderType() {
        @Override
        public void begin(BufferBuilder buf, TextureManager tm) {
            RenderSystem.setShader(GameRenderer::getParticleShader);
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.defaultBlendFunc();
            tm.bindForSetup(new ResourceLocation("minecraft", "textures/item/apple.png"));           // our live texture
            buf.begin(VertexFormat.Mode.QUADS,
                    DefaultVertexFormat.PARTICLE);
        }
        @Override public void end(Tesselator tess) {
            tess.end();
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
        }
        @Override public String toString() { return "DYNAMIC_PARTICLE"; }
    };

    private DynParticleRenderType() {}
}