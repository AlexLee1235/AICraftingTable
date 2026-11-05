package com.watermelon0117.aicraft.gpt;

import com.watermelon0117.aicraft.blocks.AIBlockHardness;
import com.watermelon0117.aicraft.blocks.AIBlockLight;
import com.watermelon0117.aicraft.blocks.AIBlockShape;
import com.watermelon0117.aicraft.blocks.AIBlockTool;
import com.watermelon0117.aicraft.common.BlockFaces;

import java.util.Base64;
import java.util.Locale;

public record GeneratedBlock(AIBlockShape shape, AIBlockTool tool, AIBlockHardness hardness, AIBlockLight light,
                             int count, boolean shapeless, BlockFaces faces) implements GeneratedResult {
    /** A stack holds 64, and the server never asks for more than 8. */
    private static final int MAX_COUNT = 64;

    /** @throws IllegalArgumentException if the texture server's answer is not a usable block */
    public static GeneratedBlock fromResponse(GeneratedResult.Response response) {
        AIBlockShape shape = AIBlockShape.fromModel(response.model());
        AIBlockTool tool = parse("tool", response.tool(), AIBlockTool.class);
        AIBlockHardness hardness = parse("hardness", response.hardness(), AIBlockHardness.class);
        AIBlockLight light = parse("light", response.light(), AIBlockLight.class);
        Integer count = response.count();
        if (count == null) {
            throw new IllegalArgumentException("The texture server's answer has no count");
        }
        if (count < 1 || count > MAX_COUNT) {
            throw new IllegalArgumentException("The texture server's answer has a count out of range: " + count);
        }
        return new GeneratedBlock(shape, tool, hardness, light, count, Boolean.TRUE.equals(response.shapeless()),
                shape == AIBlockShape.CROSS ? crossFaces(response) : faces(response));
    }

    private static BlockFaces crossFaces(GeneratedResult.Response response) {
        if (response.texture() == null) {
            throw new IllegalArgumentException("The texture server's answer has no plant picture");
        }
        return BlockFaces.ofCross(Base64.getDecoder().decode(response.texture())).validatedCross();
    }

    private static BlockFaces faces(GeneratedResult.Response response) {
        if (response.top() == null || response.front() == null || response.side() == null) {
            throw new IllegalArgumentException("The texture server's answer has no block faces");
        }
        Base64.Decoder decoder = Base64.getDecoder();
        return BlockFaces.of(response.kind(), decoder.decode(response.top()), decoder.decode(response.front()), decoder.decode(response.side())).validated();
    }

    private static <E extends Enum<E>> E parse(String field, String value, Class<E> type) {
        if (value == null) {
            throw new IllegalArgumentException("The texture server's answer has no " + field);
        }
        try {
            return Enum.valueOf(type, value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("The texture server's answer has an unknown " + field + ": " + value);
        }
    }
}
