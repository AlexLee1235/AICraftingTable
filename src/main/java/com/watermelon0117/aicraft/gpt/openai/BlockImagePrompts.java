package com.watermelon0117.aicraft.gpt.openai;

import java.util.Map;

/** The texture server's image prompts for blocks ({@code blocks.js}), frozen by its experiments: change both copies, and test them again. */
final class BlockImagePrompts {
    private static final String FLAT_SCAN = "Every square is seen perfectly straight on like a flat scan and fills its third of the image edge to edge.";

    private static final String NATURAL_LOOK = "Each square will be shrunk to 16x16 pixels, so use only bold chunky shapes: flat-colored patches at least one sixteenth of a square wide, "
            + "with strong contrast between neighboring patches, and no fine lines, tiny speckles or smooth gradients. Match the look of vanilla Minecraft block textures.\n"
            + "Not a cube, not isometric, no perspective, no cast shadow, no scene, no text, no grid lines.";

    private static final String MACHINE_LOOK = "Every part must read at that size: draw a bold simple layout with a plain outer frame if the block has one, one large central "
            + "feature that identifies the block, and flat-colored shapes at least one sixteenth of a square wide. Use strong contrast between "
            + "neighboring shapes, and no fine lines, tiny speckles, small text or smooth gradients. Match the look of vanilla Minecraft block textures.\n"
            + "Not a cube, not isometric, no perspective, no cast shadow, no scene, no grid lines.";

    private static final String SEE_THROUGH_LINES = "Use the image's transparency as the glass's own: where the glass is clear, the pixels are fully transparent; "
            + "where it is tinted, frosted or holds a liquid, the pixels are partly transparent in that color, so what is behind the block shows through; "
            + "frames, bars and other solid parts are fully opaque. There is no background behind the glass.";

    private static final Map<String, String> CROSS_POSES = Map.of(
            "below", "It stands upright, seen from the side, with its stem or base at the bottom of its silhouette. No soil, ground or shadow under it.",
            "above", "It hangs straight down, seen from the side, with the part it hangs by at the top of its silhouette. No ceiling or shadow above it.",
            "any", "It runs straight up and down, seen from the side, tall and narrow. No ground, ceiling or shadow around it.");

    private BlockImagePrompts() {
    }

    private static String openingFaces(String name) {
        return "Create a Minecraft block texture sheet for " + name + ": three flat square faces of the same block side by side in one image.\n"
                + "The image is exactly three equal squares in a row, with no gaps, margins, labels or dividers between them.\n";
    }

    private static String faceLines(BlockJudge.Plan plan) {
        return "Left square, the top face: " + plan.top() + ".\nMiddle square, the front face: " + plan.front() + ".\nRight square, the side face: " + plan.side() + ".\n";
    }

    /** A see-through material has the prompt the glass test used. One with faces is described face by face, so it is its own prompt plus the same lines. */
    static String sheet(String name, BlockJudge.Plan plan) {
        if (!plan.seeThrough()) {
            return opaqueSheet(name, plan);
        }
        if (!"material".equals(plan.kind())) {
            return opaqueSheet(name, plan) + "\n" + SEE_THROUGH_LINES;
        }
        return "Create a Minecraft block texture sheet for " + name + ": three flat square tiles of the same see-through glass block side by side in one image.\n"
                + "Description: " + plan.description() + "\n"
                + "The image is exactly three equal squares in a row, with no gaps, margins, labels or dividers between them. "
                + "Each square is its own variation of the block, not a continuation of its neighbor.\n"
                + FLAT_SCAN + " Each square repeats seamlessly by itself: "
                + "its left edge continues into its own right edge and its top edge into its own bottom edge.\n"
                + SEE_THROUGH_LINES + "\n"
                + "Each square will be shrunk to 16x16 pixels, so use only bold chunky shapes: flat patches at least one sixteenth of a square wide, "
                + "with strong contrast between neighboring patches, and no fine lines or tiny speckles; change color or transparency in clear steps, not smooth gradients. "
                + "Match the look of vanilla Minecraft glass and stained glass textures.\n"
                + "Not a cube, not isometric, no perspective, no cast shadow, no scene, no text, no grid lines.";
    }

    private static String opaqueSheet(String name, BlockJudge.Plan plan) {
        if ("material".equals(plan.kind())) {
            return "Create a Minecraft block texture sheet for " + name + ": three flat square tiles of the same material side by side in one image.\n"
                    + "Description: " + plan.description() + "\n"
                    + "The image is exactly three equal squares in a row, with no gaps, margins, labels or dividers between them. "
                    + "Each square is its own variation of the material, not a continuation of its neighbor.\n"
                    + FLAT_SCAN + " Each square repeats seamlessly by itself: "
                    + "its left edge continues into its own right edge and its top edge into its own bottom edge. No border, frame, bevel or vignette unless the material itself has one.\n"
                    + NATURAL_LOOK;
        }
        if ("natural".equals(plan.kind())) {
            return openingFaces(name) + faceLines(plan)
                    + FLAT_SCAN + " All three use the same materials, colors and lighting, "
                    + "so they clearly belong to one block. Each square's left edge continues into its own right edge, so blocks can sit side by side without a seam. "
                    + "No border, frame, bevel or vignette unless the material itself has one.\n"
                    + NATURAL_LOOK;
        }
        return openingFaces(name) + faceLines(plan)
                + FLAT_SCAN + " All three use the same materials, colors and lighting, "
                + "so they clearly belong to one block. Each square will be shrunk to 16x16 pixels. "
                + MACHINE_LOOK;
    }

    /** Drawn like an item, so it has a transparent background. */
    static String cross(String name, String description, String attach) {
        return GPTImageGenerator.buildPrompt(name, description) + "\n" + CROSS_POSES.get(attach);
    }
}
