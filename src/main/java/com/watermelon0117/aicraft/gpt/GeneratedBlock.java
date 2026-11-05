package com.watermelon0117.aicraft.gpt;

import com.watermelon0117.aicraft.blocks.AIBlockHardness;
import com.watermelon0117.aicraft.blocks.AIBlockLight;
import com.watermelon0117.aicraft.blocks.AIBlockTool;
import com.watermelon0117.aicraft.common.BlockFaces;

import java.util.Base64;
import java.util.Locale;

public record GeneratedBlock(AIBlockTool tool, AIBlockHardness hardness, AIBlockLight light, BlockFaces faces) {
    /**
     * A value the texture server does not send gets the default, because it does not send these three yet.
     *
     * @throws IllegalArgumentException if the texture server's answer is not a usable block
     */
    public static GeneratedBlock fromResponse(String kind, String tool, String hardness, String light, String top, String front, String side) {
        AIBlockTool parsedTool = parse("tool", tool, AIBlockTool.class, AIBlockTool.PICKAXE);
        AIBlockHardness parsedHardness = parse("hardness", hardness, AIBlockHardness.class, AIBlockHardness.NORMAL);
        AIBlockLight parsedLight = parse("light", light, AIBlockLight.class, AIBlockLight.NONE);
        if (top == null || front == null || side == null) {
            throw new IllegalArgumentException("The texture server's answer has no block faces");
        }
        Base64.Decoder decoder = Base64.getDecoder();
        BlockFaces faces = BlockFaces.of(kind, decoder.decode(top), decoder.decode(front), decoder.decode(side));
        return new GeneratedBlock(parsedTool, parsedHardness, parsedLight, faces.validated());
    }

    private static <E extends Enum<E>> E parse(String field, String value, Class<E> type, E whenMissing) {
        if (value == null) {
            return whenMissing;
        }
        try {
            return Enum.valueOf(type, value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("The texture server's answer has an unknown " + field + ": " + value);
        }
    }
}
