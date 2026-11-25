package com.watermelon0117.aicraft.gpt.openai;

import com.watermelon0117.aicraft.common.ArmorTexture;
import com.watermelon0117.aicraft.common.ItemSheet;
import com.watermelon0117.aicraft.common.WornLayout;
import com.watermelon0117.aicraft.common.WornReader;
import com.watermelon0117.aicraft.gpt.GenerationException;
import net.minecraft.network.chat.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class GPTImageGenerator {
    OpenAIImageClient imgClient = new OpenAIImageClient();

    /** Three drawings of the item in a row, the texture server's request for an item; {@link ItemSheet} picks one. */
    public CompletableFuture<String> generateItemSheet(String name, String desc) {
        return imgClient.generateAsync(buildItemSheetPrompt(name, desc), "1536x512", "transparent", "low", "medium", "png");
    }

    /** A transparent PNG, the same request as the texture server's for a plant. */
    CompletableFuture<String> generateDrawing(String prompt) {
        return imgClient.generateAsync(prompt, "1024x1024", "transparent", "low", "medium", "png");
    }

    /**
     * The picture of an armor piece as worn, as the texture server makes it: the model paints the light gray armor on the
     * mannequin's views (WornLayout) from the item's icon, and the W reader makes the atlas and its UV table.
     *
     * @param iconPng the icon the player sees in the inventory
     */
    CompletableFuture<ArmorTexture> paintArmor(String slot, String name, String description, byte[] iconPng) {
        byte[] views = png(WornLayout.input(slot));
        byte[] icon = png(WornLayout.icon(readPng(iconPng)));
        return imgClient.editAsync(WornLayout.prompt(slot, name, description), List.of(views, icon), "1536x1024", "transparent", "low", "medium", "png")
                .thenApply(b64 -> {
                    Optional<WornReader.Result> read = WornReader.read(readPng(Base64.getDecoder().decode(b64)), slot);
                    if (read.isEmpty()) {
                        throw new GenerationException(Component.translatable("error.aicraft.armor_failed"));
                    }
                    return new ArmorTexture(png(read.get().atlas()), read.get().uv());
                });
    }

    private static BufferedImage readPng(byte[] bytes) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
            if (image == null) {
                throw new IllegalArgumentException("Unreadable picture");
            }
            return image;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static byte[] png(BufferedImage image) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "png", out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** One image of three squares. Transparent only for a see-through block: in that mode the model leaves every block faintly see-through. */
    CompletableFuture<String> generateSheet(String prompt, boolean seeThrough) {
        return imgClient.generateAsync(prompt, "1536x512", seeThrough ? "transparent" : "opaque", "low", "medium", seeThrough ? "png" : "jpeg");
    }

    /**
     * The item prompt below changed only where three squares need it, as test/item_sheet_3x round B made it: the texture
     * server's buildItemSheetPrompt, word for word. One 1536x512 image costs about 40% of a 1024 one, and in the test the
     * left drawing was as large as a single item and kept the named materials.
     */
    static String buildItemSheetPrompt(String name, String desc) {
        return "Create a Minecraft inventory sprite sheet of " + name + ": three drawings of the same item side by side in one image.\n"
                + "Description: " + desc + "\n"
                + "The image is exactly three equal squares in a row, with no gaps, labels or dividers between them. "
                + "Each square holds one complete item, centered in its own square; no part of an item reaches into a neighboring square. "
                + "All three are the same item made of the same materials, with the same color palette, as the description says; "
                + "they differ only in small details of shape. Never turn a copy into another material, tier or color scheme.\n"
                + "Each square is a native 16x16 bitmap enlarged exactly 32 times by nearest neighbor. "
                + "Make each item SMALL: its entire silhouette fits in the central 8 columns and 8 rows of its square, "
                + "occupying no more than half the square's width or height. "
                + "Leave the outer four pixel rows and columns of every square fully transparent (alpha 0). "
                + "Every native pixel is a solid 32x32 square on the same grid. "
                + "Use only these few central pixels for the recognizable silhouette and essential details; "
                + "do not draw tiny additional pixels inside them.\n"
                + "Draw each one as a flat 2D front-facing inventory icon, not an isometric 3D block.\n"
                + "Background: fully transparent (alpha 0), with only the three items visible. "
                + "Do not paint a colored backdrop or a checkerboard pattern. "
                + "Keep the items opaque; transparency is for the empty background. "
                + "No ground plane, cast shadow, surrounding scene, grid lines, text, frame or editor interface. "
                + "No gradients, antialiasing, subpixel detail, glow halos, noise or small decorative textures. "
                + "For emissive materials use a few solid bright pixels instead of blurry light. "
                + "Keep every item fully visible and recognizable. "
                + "Simplify details that cannot fit into whole pixels; do not replace the item with an unrelated generic shape.";
    }

    /**
     * Sunburst draws much finer pixels than gpt-image-1 did. Asking for a small item on a coarse
     * 16x16 canvas, flat and on a transparent background, keeps its textures near the old sizes.
     * Plants are still drawn from it; items were until the sheet above.
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
