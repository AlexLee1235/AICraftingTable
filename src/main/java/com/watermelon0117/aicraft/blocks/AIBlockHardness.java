package com.watermelon0117.aicraft.blocks;

import java.util.Locale;

/** What the texture server can say about how hard a block is. The numbers are those of a sapling, dirt, stone, an iron block and bedrock. */
public enum AIBlockHardness {
    INSTANT(0.0F, 0.0F),
    SOFT(0.5F, 0.5F),
    NORMAL(1.5F, 6.0F),
    HARD(5.0F, 6.0F),
    UNBREAKABLE(-1.0F, 3600000.0F);

    private final float hardness;
    private final float blastResistance;

    AIBlockHardness(float hardness, float blastResistance) {
        this.hardness = hardness;
        this.blastResistance = blastResistance;
    }

    public float hardness() {
        return hardness;
    }

    public float blastResistance() {
        return blastResistance;
    }

    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** @return the level with this name, or normal when a tag has none or an unknown one */
    public static AIBlockHardness byName(String name) {
        for (AIBlockHardness hardness : values()) {
            if (hardness.getSerializedName().equals(name)) {
                return hardness;
            }
        }
        return NORMAL;
    }
}
