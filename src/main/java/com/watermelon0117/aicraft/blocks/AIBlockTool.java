package com.watermelon0117.aicraft.blocks;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Locale;

/**
 * The tool that mines a block, kept in its block entity. Tools only read the tags of the state they are given,
 * so a vanilla block with those tags stands in for the AI block when asking one how it does.
 */
public enum AIBlockTool {
    PICKAXE, AXE, SHOVEL, HOE, NONE;

    /** A method rather than a field, so that reading a server's answer does not start up the game's blocks. */
    public BlockState standIn() {
        return switch (this) {
            case PICKAXE -> Blocks.STONE.defaultBlockState();
            case AXE -> Blocks.OAK_PLANKS.defaultBlockState();
            case SHOVEL -> Blocks.DIRT.defaultBlockState();
            case HOE -> Blocks.HAY_BLOCK.defaultBlockState();
            case NONE -> Blocks.WHITE_WOOL.defaultBlockState();
        };
    }

    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** @return the tool with this name, or the pickaxe when a tag has none or an unknown one */
    public static AIBlockTool byName(String name) {
        for (AIBlockTool tool : values()) {
            if (tool.getSerializedName().equals(name)) {
                return tool;
            }
        }
        return PICKAXE;
    }
}
