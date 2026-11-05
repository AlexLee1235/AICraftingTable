package com.watermelon0117.aicraft.blocks;

import net.minecraft.util.StringRepresentable;

import java.util.Locale;

/** What the texture server can say about how much light a block gives. */
public enum AIBlockLight implements StringRepresentable {
    NONE(0),
    DIM(5),
    BRIGHT(15);

    private final int level;

    AIBlockLight(int level) {
        this.level = level;
    }

    public int level() {
        return level;
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
