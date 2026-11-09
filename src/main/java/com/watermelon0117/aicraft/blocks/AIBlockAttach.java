package com.watermelon0117.aicraft.blocks;

import net.minecraft.util.StringRepresentable;

import java.util.Locale;

/** What the texture server says holds a cross in place. */
public enum AIBlockAttach implements StringRepresentable {
    /** Stands on the block below. */
    BELOW,
    /** Hangs from the block above. */
    ABOVE,
    /** Needs no support, as a vanilla chain does not, so it can stack on itself. */
    ANY;

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
