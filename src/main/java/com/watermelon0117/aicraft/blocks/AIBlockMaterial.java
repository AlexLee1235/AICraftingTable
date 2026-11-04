package com.watermelon0117.aicraft.blocks;

import java.util.Locale;

/** The material classes the text model chooses from. Each one is a real block, so vanilla sounds, tools and tags apply. */
public enum AIBlockMaterial {
    STONE, METAL, WOOD, SOIL, WOOL;

    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String blockName() {
        return "ai_block_" + serializedName();
    }

    public static AIBlockMaterial byName(String name) {
        for (AIBlockMaterial material : values()) {
            if (material.serializedName().equals(name)) {
                return material;
            }
        }
        throw new IllegalArgumentException("Unknown block material: " + name);
    }
}
