package com.watermelon0117.aicraft.gpt;

import com.watermelon0117.aicraft.blocks.AIBlockTool;
import com.watermelon0117.aicraft.common.BlockFaces;

import java.util.Base64;

public record GeneratedBlock(AIBlockTool tool, BlockFaces faces) {
    /** The texture server does not say which tool a block takes yet, so every generated block gets this one. */
    private static final AIBlockTool TOOL = AIBlockTool.PICKAXE;

    /** @throws IllegalArgumentException if the texture server's answer is not a usable block */
    public static GeneratedBlock fromResponse(String kind, String top, String front, String side) {
        if (top == null || front == null || side == null) {
            throw new IllegalArgumentException("The texture server's answer has no block faces");
        }
        Base64.Decoder decoder = Base64.getDecoder();
        BlockFaces faces = BlockFaces.of(kind, decoder.decode(top), decoder.decode(front), decoder.decode(side));
        return new GeneratedBlock(TOOL, faces.validated());
    }
}
