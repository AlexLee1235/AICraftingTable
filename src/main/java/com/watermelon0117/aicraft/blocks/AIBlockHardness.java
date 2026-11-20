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

    /** @return this level, or the hardest one a player can still break in place of unbreakable */
    public AIBlockHardness breakable() {
        return this == UNBREAKABLE ? HARD : this;
    }

    /**
     * @return the level nearest a vanilla block's destroy time. Times multiply rather than add, so the line between two
     * levels is their geometric mean: below 0.87 is soft and below 2.74 normal.
     */
    public static AIBlockHardness nearest(float destroyTime) {
        if (destroyTime < 0) {
            return UNBREAKABLE;
        }
        if (destroyTime == 0) {
            return INSTANT;
        }
        AIBlockHardness[] breakable = {SOFT, NORMAL, HARD};
        for (int i = 0; i < breakable.length - 1; i++) {
            if (destroyTime < Math.sqrt(breakable[i].hardness * breakable[i + 1].hardness)) {
                return breakable[i];
            }
        }
        return HARD;
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
