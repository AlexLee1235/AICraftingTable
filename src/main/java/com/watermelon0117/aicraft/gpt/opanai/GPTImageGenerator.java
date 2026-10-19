package com.watermelon0117.aicraft.gpt.opanai;

import java.util.concurrent.CompletableFuture;

public class GPTImageGenerator {
    OpenAIImageClient imgClient = new OpenAIImageClient();
    public CompletableFuture<String> generateItem(String name, String desc) {
        // Transparent PNG, same request as the texture server; TextureManager pixelates it with SpritePixelizer.
        return imgClient.generateAsync(buildPrompt(name, desc), "1024x1024", "transparent", "low", "medium", "png");
    }

    /**
     * Sunburst draws much finer pixels than gpt-image-1 did. Asking for a small item on a coarse
     * 16x16 canvas, flat and on a transparent background, keeps its textures near the old sizes.
     */
    static String buildPrompt(String name, String desc) {
        return "Create a Minecraft inventory sprite of " + name + ".\n"
                + "Description: " + desc + "\n"
                + "The whole image is a native 16x16 bitmap enlarged exactly 64 times by nearest neighbor. "
                + "Make the item SMALL: its entire silhouette fits in the central 8 columns and 8 rows, "
                + "occupying no more than half the image width or height. "
                + "Leave the outer four pixel rows and columns fully transparent (alpha 0). "
                + "Every native pixel is a solid 64x64 square on the same grid. "
                + "Use only these few central pixels for the recognizable silhouette and essential details; "
                + "do not draw tiny additional pixels inside them.\n"
                + "Draw it as a flat 2D front-facing inventory icon, not an isometric 3D block.\n"
                + "Background: fully transparent (alpha 0), with only the item visible. "
                + "Do not paint a colored backdrop or a checkerboard pattern. "
                + "Keep the item opaque; transparency is for the empty background. "
                + "No ground plane, cast shadow, surrounding scene, grid lines, text, frame or editor interface. "
                + "No gradients, antialiasing, subpixel detail, glow halos, noise or small decorative textures. "
                + "For emissive materials use a few solid bright pixels instead of blurry light. "
                + "Keep the item fully visible and recognizable. "
                + "Simplify details that cannot fit into whole pixels; do not replace the item with an unrelated generic shape.";
    }
}
