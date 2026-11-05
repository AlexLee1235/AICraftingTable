package com.watermelon0117.aicraft.blocks;

import java.util.Locale;

/**
 * The tool that mines a block. Each one is a block of its own because tools only look at block tags,
 * and a tag cannot differ between two block entities of one block.
 */
public enum AIBlockTool {
    PICKAXE, AXE, SHOVEL, HOE, NONE;

    public String blockName() {
        return "ai_block_" + name().toLowerCase(Locale.ROOT);
    }
}
