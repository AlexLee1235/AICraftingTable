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
    private static final float BLOCK_SIXTEENTHS = 16.0F;
    /** A turned model's corners land a rounding error away from the grid, far less than this. */
    private static final float ON_GRID = 0.001F;

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

        private float position(int vertex, int axis) {
            return Float.intBitsToFloat(vertices[vertex * IQuadTransformer.STRIDE + IQuadTransformer.POSITION + axis]) * BLOCK_SIXTEENTHS;
        }

        /** @return this quad as a face of a box, or null if it is not one on the grid of sixteenths */
        @Nullable
        InnerFaces.Face face() {
            int axis = direction.getAxis().ordinal();
            int uAxis = InnerFaces.uAxis(axis);
            int vAxis = InnerFaces.vAxis(axis);
            int uMin = Integer.MAX_VALUE, uMax = Integer.MIN_VALUE, vMin = Integer.MAX_VALUE, vMax = Integer.MIN_VALUE;
            int plane = Math.round(position(0, axis));
            for (int vertex = 0; vertex < VERTICES_PER_QUAD; vertex++) {
                for (int a = 0; a < 3; a++) {
                    if (Math.abs(position(vertex, a) - Math.round(position(vertex, a))) > ON_GRID) {
                        return null;
                    }
                }
                if (Math.round(position(vertex, axis)) != plane) {
                    return null;
                }
                int u = Math.round(position(vertex, uAxis));
                int v = Math.round(position(vertex, vAxis));
                uMin = Math.min(uMin, u);
                uMax = Math.max(uMax, u);
                vMin = Math.min(vMin, v);
                vMax = Math.max(vMax, v);
            }
            boolean[] corners = new boolean[VERTICES_PER_QUAD];
            for (int vertex = 0; vertex < VERTICES_PER_QUAD; vertex++) {
                int u = Math.round(position(vertex, uAxis));
                int v = Math.round(position(vertex, vAxis));
                if (u != uMin && u != uMax || v != vMin && v != vMax) {
                    return null;
                }
                corners[(u == uMax ? 1 : 0) + (v == vMax ? 2 : 0)] = true;
            }
            for (boolean corner : corners) {
                if (!corner || uMin == uMax || vMin == vMax) {
                    return null;
                }
            }
            return new InnerFaces.Face(axis, direction.getAxisDirection() == Direction.AxisDirection.POSITIVE, plane, uMin, uMax, vMin, vMax);
        }

        /** Each vertex keeps its corner of the rectangle, so the quad faces the same way and shows the pixels it showed there. */
        Quad part(InnerFaces.Face face, InnerFaces.Rect rect) {
            int uAxis = InnerFaces.uAxis(face.axis());
            int vAxis = InnerFaces.vAxis(face.axis());
            float[][] cornerU = new float[2][2];
            float[][] cornerV = new float[2][2];
            for (int vertex = 0; vertex < VERTICES_PER_QUAD; vertex++) {
                int highU = Math.round(position(vertex, uAxis)) == face.uMax() ? 1 : 0;
                int highV = Math.round(position(vertex, vAxis)) == face.vMax() ? 1 : 0;
                cornerU[highU][highV] = pixels[vertex * 2];
                cornerV[highU][highV] = pixels[vertex * 2 + 1];
            }
            int[] partVertices = vertices.clone();
            float[] partPixels = new float[VERTICES_PER_QUAD * 2];
            for (int vertex = 0; vertex < VERTICES_PER_QUAD; vertex++) {
                int u = Math.round(position(vertex, uAxis)) == face.uMax() ? rect.uMax() : rect.uMin();
                int v = Math.round(position(vertex, vAxis)) == face.vMax() ? rect.vMax() : rect.vMin();
                int start = vertex * IQuadTransformer.STRIDE + IQuadTransformer.POSITION;
                partVertices[start + uAxis] = Float.floatToRawIntBits(u / BLOCK_SIXTEENTHS);
                partVertices[start + vAxis] = Float.floatToRawIntBits(v / BLOCK_SIXTEENTHS);
                float s = (u - face.uMin()) / (float) (face.uMax() - face.uMin());
                float t = (v - face.vMin()) / (float) (face.vMax() - face.vMin());
                partPixels[vertex * 2] = between(cornerU, s, t);
                partPixels[vertex * 2 + 1] = between(cornerV, s, t);
            }
            return new Quad(partVertices, partPixels, tint, direction, shade, ambientOcclusion);
        }

        private static float between(float[][] corners, float s, float t) {
            float value = (1 - s) * (1 - t) * corners[0][0] + s * (1 - t) * corners[1][0] + (1 - s) * t * corners[0][1] + s * t * corners[1][1];
            return Math.round(value * PIXEL_STEPS) / PIXEL_STEPS;
        }
    }

    private final List<List<Quad>> quadsBySide;
    private final boolean ambientOcclusion;
    private final boolean gui3d;
    private final boolean blockLight;

    private ShapeTemplate(List<List<Quad>> quadsBySide, boolean ambientOcclusion, boolean gui3d, boolean blockLight) {
        this.quadsBySide = quadsBySide;
        this.ambientOcclusion = ambientOcclusion;
        this.gui3d = gui3d;
        this.blockLight = blockLight;
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
        return new ShapeTemplate(List.copyOf(quadsBySide), model.useAmbientOcclusion(), model.isGui3d(), model.usesBlockLight());
    }

    /**
     * For a block you can see through, whose model must not show the faces its boxes hide from each other.
     *
     * @return this template without those faces, or itself if it has none or a quad is not the face of a box
     */
    public ShapeTemplate withoutInnerFaces() {
        List<InnerFaces.Face> faces = new ArrayList<>();
        for (List<Quad> quads : quadsBySide) {
            for (Quad quad : quads) {
                InnerFaces.Face face = quad.face();
                if (face == null) {
                    return this;
                }
                faces.add(face);
            }
        }
        List<List<InnerFaces.Rect>> kept = InnerFaces.outsideParts(faces);
        List<List<Quad>> outside = new ArrayList<>(quadsBySide.size());
        boolean changed = false;
        int index = 0;
        for (List<Quad> quads : quadsBySide) {
            List<Quad> side = new ArrayList<>(quads.size());
            for (Quad quad : quads) {
                InnerFaces.Face face = faces.get(index);
                List<InnerFaces.Rect> parts = kept.get(index++);
                if (parts.equals(List.of(face.whole()))) {
                    side.add(quad);
                    continue;
                }
                changed = true;
                for (InnerFaces.Rect part : parts) {
                    side.add(quad.part(face, part));
                }
            }
            outside.add(List.copyOf(side));
        }
        return changed ? new ShapeTemplate(List.copyOf(outside), ambientOcclusion, gui3d, blockLight) : this;
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
