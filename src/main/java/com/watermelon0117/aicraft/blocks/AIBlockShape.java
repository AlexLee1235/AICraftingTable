package com.watermelon0117.aicraft.blocks;

/** The shapes the texture server can name. It names no block, so that reading a server's answer does not start up the game's blocks. */
public enum AIBlockShape {
    CUBE, SLAB, STAIRS, FENCE, WALL, CROSS;

    /** @throws IllegalArgumentException if the texture server's answer does not name a shape */
    public static AIBlockShape fromModel(String model) {
        if (model == null) {
            throw new IllegalArgumentException("The texture server's answer has no model");
        }
        return switch (model) {
            case "cube_material", "cube_natural", "cube_machine" -> CUBE;
            case "slab" -> SLAB;
            case "stairs" -> STAIRS;
            case "fence" -> FENCE;
            case "wall" -> WALL;
            case "cross" -> CROSS;
            default -> throw new IllegalArgumentException("The texture server's answer has an unknown model: " + model);
        };
    }
}
