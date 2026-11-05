package com.watermelon0117.aicraft.blocks;

import net.minecraft.util.StringRepresentable;

import java.util.Locale;

/** What the texture server can say about how hard a block is. The numbers are those of dirt, stone, an iron block and bedrock. */
public enum AIBlockHardness implements StringRepresentable {
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

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
