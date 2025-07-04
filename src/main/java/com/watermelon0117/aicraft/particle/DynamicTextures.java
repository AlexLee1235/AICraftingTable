package com.watermelon0117.aicraft.particle;

import com.watermelon0117.aicraft.AICraftingTable;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;

public final class DynamicTextures {
    public static final ResourceLocation ID =
            new ResourceLocation(AICraftingTable.MODID, "dynamic/dyn_sheet");

    private static DynamicTexture tex;

    /** call once from FMLClientSetupEvent.enqueueWork */
    public static void bootstrap(TextureManager tm) {
        tex = new DynamicTexture(64, 64, true);
        tm.register(ID, tex);
    }

    public static DynamicTexture tex() { return tex; }

    /** very simple animated gradient */
    public static void tick(long gameTime) {
        var img   = tex.getPixels();
        int w = img.getWidth(), h = img.getHeight();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                float u = (x + gameTime * 2) / (float) w;
                float v = (y + gameTime * 2) / (float) h;
                int r = (int) (Math.sin(u * Math.PI) * 127 + 128);
                int g = (int) (Math.sin(v * Math.PI) * 127 + 128);
                int argb = 0xFF_000000 | r << 16 | g << 8 | 0xFF;
                img.setPixelRGBA(x, y, argb);
            }
        }
        tex.upload();
    }
}
