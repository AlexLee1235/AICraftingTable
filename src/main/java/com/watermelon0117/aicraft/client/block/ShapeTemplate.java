package com.watermelon0117.aicraft.client.block;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.model.IQuadTransformer;
import net.minecraftforge.client.model.data.ModelData;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * The quads of one baked vanilla model, ready to be pointed at another sprite. Copying them keeps vanilla's shape,
 * UV lock, shading and culling without restating any of it, and needs only the sprite the quads will show.
 */
@OnlyIn(Dist.CLIENT)
public final class ShapeTemplate {
    private static final Direction[] SIDES = Direction.values();
    /** Where the quads with no side go: those that are never culled. */
    private static final int NO_SIDE = SIDES.length;
    private static final int VERTICES_PER_QUAD = 4;
    private static final float SPRITE_PIXELS = 16.0F;
    /** Vanilla's UVs are whole pixels, and the float arithmetic that finds them again must not move an edge onto the next one. */
    private static final float PIXEL_STEPS = 1000.0F;

    private record Quad(int[] vertices, float[] pixels, int tint, Direction direction, boolean shade, boolean ambientOcclusion) {
        static Quad of(BakedQuad quad) {
            TextureAtlasSprite sprite = quad.getSprite();
            int[] vertices = quad.getVertices();
            float[] pixels = new float[VERTICES_PER_QUAD * 2];
            for (int vertex = 0; vertex < VERTICES_PER_QUAD; vertex++) {
                int uv = vertex * IQuadTransformer.STRIDE + IQuadTransformer.UV0;
                pixels[vertex * 2] = pixel(Float.intBitsToFloat(vertices[uv]), sprite.getU0(), sprite.getU1());
                pixels[vertex * 2 + 1] = pixel(Float.intBitsToFloat(vertices[uv + 1]), sprite.getV0(), sprite.getV1());
            }
            return new Quad(vertices.clone(), pixels, quad.getTintIndex(), quad.getDirection(), quad.isShade(), quad.hasAmbientOcclusion());
        }

        private static float pixel(float value, float start, float end) {
            return Math.round((value - start) / (end - start) * SPRITE_PIXELS * PIXEL_STEPS) / PIXEL_STEPS;
        }

        BakedQuad on(TextureAtlasSprite sprite) {
            int[] copy = vertices.clone();
            for (int vertex = 0; vertex < VERTICES_PER_QUAD; vertex++) {
                int uv = vertex * IQuadTransformer.STRIDE + IQuadTransformer.UV0;
                copy[uv] = Float.floatToRawIntBits(sprite.getU(pixels[vertex * 2]));
                copy[uv + 1] = Float.floatToRawIntBits(sprite.getV(pixels[vertex * 2 + 1]));
            }
            return new BakedQuad(copy, tint, direction, sprite, shade, ambientOcclusion);
        }
    }

    private final List<List<Quad>> quadsBySide;
    private final boolean ambientOcclusion;
    private final boolean gui3d;
    private final boolean blockLight;

    private ShapeTemplate(List<List<Quad>> quadsBySide, BakedModel model) {
        this.quadsBySide = quadsBySide;
        this.ambientOcclusion = model.useAmbientOcclusion();
        this.gui3d = model.isGui3d();
        this.blockLight = model.usesBlockLight();
    }

    /** @param state the vanilla state to copy, or null for an item's model */
    public static ShapeTemplate of(BakedModel model, @Nullable BlockState state) {
        RandomSource random = RandomSource.create(42);
        List<List<Quad>> quadsBySide = new ArrayList<>(NO_SIDE + 1);
        for (int index = 0; index <= NO_SIDE; index++) {
            List<Quad> quads = new ArrayList<>();
            for (BakedQuad quad : model.getQuads(state, index == NO_SIDE ? null : SIDES[index], random, ModelData.EMPTY, null)) {
                quads.add(Quad.of(quad));
            }
            quadsBySide.add(List.copyOf(quads));
        }
        return new ShapeTemplate(List.copyOf(quadsBySide), model);
    }

    /** @return the quads of each side, for {@link #quadsOf} */
    List<List<BakedQuad>> on(TextureAtlasSprite sprite) {
        List<List<BakedQuad>> baked = new ArrayList<>(quadsBySide.size());
        for (List<Quad> quads : quadsBySide) {
            List<BakedQuad> side = new ArrayList<>(quads.size());
            for (Quad quad : quads) {
                side.add(quad.on(sprite));
            }
            baked.add(List.copyOf(side));
        }
        return List.copyOf(baked);
    }

    static List<BakedQuad> quadsOf(List<List<BakedQuad>> baked, @Nullable Direction side) {
        return baked.get(side == null ? NO_SIDE : side.ordinal());
    }

    public boolean ambientOcclusion() {
        return ambientOcclusion;
    }

    public boolean gui3d() {
        return gui3d;
    }

    public boolean blockLight() {
        return blockLight;
    }
}
