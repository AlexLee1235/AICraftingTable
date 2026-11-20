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

    /** @return the level for a vanilla block's light: any light at all is at least dim, and from 10 up it is nearer bright's 15 */
    public static AIBlockLight nearest(int emission) {
        if (emission <= 0) {
            return NONE;
        }
        return emission < 10 ? DIM : BRIGHT;
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
