package com.watermelon0117.aicraft.blocks;

import net.minecraft.world.level.block.SoundType;

import java.util.Locale;

/** What the texture server can say a block sounds like: the vanilla sounds that suit these shapes and differ from one another. */
public enum AIBlockSound {
    STONE, DEEPSLATE, NETHERRACK, NETHER_BRICKS, BONE, METAL, COPPER, NETHERITE, AMETHYST, GLASS, WOOD,
    WOOL, GRASS, GRAVEL, SAND, MUD, SNOW, SLIME, HONEY, CORAL, WART, SCULK;

    /** A method rather than a field, so that reading a server's answer does not start up the game's sounds. */
    public SoundType soundType() {
        return switch (this) {
            case STONE -> SoundType.STONE;
            case DEEPSLATE -> SoundType.DEEPSLATE;
            case NETHERRACK -> SoundType.NETHERRACK;
            case NETHER_BRICKS -> SoundType.NETHER_BRICKS;
            case BONE -> SoundType.BONE_BLOCK;
            case METAL -> SoundType.METAL;
            case COPPER -> SoundType.COPPER;
            case NETHERITE -> SoundType.NETHERITE_BLOCK;
            case AMETHYST -> SoundType.AMETHYST;
            case GLASS -> SoundType.GLASS;
            case WOOD -> SoundType.WOOD;
            case WOOL -> SoundType.WOOL;
            case GRASS -> SoundType.GRASS;
            case GRAVEL -> SoundType.GRAVEL;
            case SAND -> SoundType.SAND;
            case MUD -> SoundType.MUD;
            case SNOW -> SoundType.SNOW;
            case SLIME -> SoundType.SLIME_BLOCK;
            case HONEY -> SoundType.HONEY_BLOCK;
            case CORAL -> SoundType.CORAL_BLOCK;
            case WART -> SoundType.WART_BLOCK;
            case SCULK -> SoundType.SCULK;
        };
    }

    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** @return the one of these that is this vanilla sound, or stone when none is */
    public static AIBlockSound of(SoundType type) {
        for (AIBlockSound sound : values()) {
            if (sound.soundType() == type) {
                return sound;
            }
        }
        return STONE;
    }

    /** @return the sound with this name, or stone when a tag has none or an unknown one */
    public static AIBlockSound byName(String name) {
        for (AIBlockSound sound : values()) {
            if (sound.getSerializedName().equals(name)) {
                return sound;
            }
        }
        return STONE;
    }
}
