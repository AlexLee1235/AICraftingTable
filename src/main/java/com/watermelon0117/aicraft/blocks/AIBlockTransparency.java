package com.watermelon0117.aicraft.blocks;

import com.watermelon0117.aicraft.blockentities.AIBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;

/**
 * How much of a block can be seen through, read from its faces' pixels: the texture server asks for transparency only
 * when its judge says a block is see-through, but whether that came out clear or tinted is the image model's doing.
 */
public enum AIBlockTransparency implements StringRepresentable {
    /** Every pixel solid, like stone. */
    OPAQUE,
    /** Pixels clear or solid, like glass. */
    CUTOUT,
    /** Many pixels partly clear, like stained glass or ice. */
    TRANSLUCENT;

    public boolean seeThrough() {
        return this != OPAQUE;
    }

    /**
     * Glass drawn clear in a frame still has a few soft cells where they meet: 0.4 to 0.8% of the pixels in the paid
     * round (test/block_glass/run_paid), against 57 to 100% for glass drawn tinted. Below this share it is cut out,
     * which draws those few solid and keeps rain and particles visible behind it.
     */
    static final double TRANSLUCENT_SHARE = 0.05;

    /** @param alphas every alpha of a block's faces */
    public static AIBlockTransparency ofAlphas(int[] alphas) {
        int partlyClear = 0;
        boolean clear = false;
        for (int alpha : alphas) {
            partlyClear += alpha > 0 && alpha < 255 ? 1 : 0;
            clear |= alpha == 0;
        }
        if (partlyClear > 0 && partlyClear >= TRANSLUCENT_SHARE * alphas.length) {
            return TRANSLUCENT;
        }
        return clear ? CUTOUT : OPAQUE;
    }

    /** A missing or unknown name is opaque, which is what every block made before transparency was. */
    public static AIBlockTransparency byName(String name) {
        for (AIBlockTransparency value : values()) {
            if (value.getSerializedName().equals(name)) {
                return value;
            }
        }
        return OPAQUE;
    }

    public static AIBlockTransparency ofStack(ItemStack stack) {
        CompoundTag data = stack.getTagElement(AIBlockEntity.DATA_KEY);
        return data == null ? OPAQUE : byName(data.getString(AIPlacedBlock.TRANSPARENCY.getName()));
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
