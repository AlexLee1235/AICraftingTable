package com.watermelon0117.aicraft.gpt;

import com.watermelon0117.aicraft.blocks.AIBlockAttach;
import com.watermelon0117.aicraft.blocks.AIBlockHardness;
import com.watermelon0117.aicraft.blocks.AIBlockLight;
import com.watermelon0117.aicraft.blocks.AIBlockShape;
import com.watermelon0117.aicraft.blocks.AIBlockSound;
import com.watermelon0117.aicraft.blocks.AIBlockTool;
import com.watermelon0117.aicraft.blocks.AIBlockTransparency;
import com.watermelon0117.aicraft.common.BlockFaces;
import org.jetbrains.annotations.Nullable;

import java.util.Base64;
import java.util.Locale;

/**
 * @param soundOf      the id of a vanilla block whose own sound this block keeps, or null; the texture server never names one
 * @param transparency read from the faces; a plant is opaque here, as it has no such state and is cut out anyway
 * @param attach       what holds a cross in place; every other shape says {@code BELOW}, and nothing reads it
 */
public record GeneratedBlock(AIBlockShape shape, AIBlockTool tool, AIBlockHardness hardness, AIBlockLight light,
                             AIBlockSound sound, @Nullable String soundOf, boolean flammable, AIBlockTransparency transparency,
                             int count, boolean shapeless, AIBlockAttach attach, BlockFaces faces) implements GeneratedResult {
    /** A stack holds 64, and the server never asks for more than 8. */
    private static final int MAX_COUNT = 64;

    /** @throws IllegalArgumentException if the texture server's answer is not a usable block */
    public static GeneratedBlock fromResponse(GeneratedResult.Response response) {
        AIBlockShape shape = AIBlockShape.fromModel(response.model());
        AIBlockTool tool = parse("tool", response.tool(), AIBlockTool.class);
        AIBlockHardness hardness = parse("hardness", response.hardness(), AIBlockHardness.class);
        AIBlockLight light = parse("light", response.light(), AIBlockLight.class);
        AIBlockSound sound = parse("sound", response.sound(), AIBlockSound.class);
        if (response.flammable() == null) {
            throw new IllegalArgumentException("The texture server's answer has no flammable");
        }
        Integer count = response.count();
        if (count == null) {
            throw new IllegalArgumentException("The texture server's answer has no count");
        }
        if (count < 1 || count > MAX_COUNT) {
            throw new IllegalArgumentException("The texture server's answer has a count out of range: " + count);
        }
        BlockFaces faces = shape == AIBlockShape.CROSS ? crossFaces(response) : faces(response);
        AIBlockTransparency transparency = shape == AIBlockShape.CROSS ? AIBlockTransparency.OPAQUE : faces.transparency();
        AIBlockAttach attach = shape == AIBlockShape.CROSS ? parse("attach", response.attach(), AIBlockAttach.class) : AIBlockAttach.BELOW;
        return new GeneratedBlock(shape, tool, hardness, light, sound, null, response.flammable(), transparency, count,
                Boolean.TRUE.equals(response.shapeless()), attach, faces);
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
