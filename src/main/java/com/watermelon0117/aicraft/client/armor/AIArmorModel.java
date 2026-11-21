package com.watermelon0117.aicraft.client.armor;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.watermelon0117.aicraft.common.ArmorUV;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The humanoid armor model with one picture block per face ({@link ArmorUV}), so each face shows the pixels the image model drew for it.
 * Vanilla's cubes read box UV, which gives every face of a box one scale.
 * <p>
 * Part sizes and face mapping are those of the armor test's renderer and 3D page (test/armor_texture/armorlib.py SLOT_PARTS_NO_FIGHT,
 * test/armor_texture2/viewer FACES and boxGeometry), which keep overlapping faces out of one plane: vanilla's chestplate arms sit
 * in the body's front plane, the two legs overlap in the middle, and the leggings' hips hide the legs' first row.
 */
@OnlyIn(Dist.CLIENT)
public final class AIArmorModel {
    private AIArmorModel() {
    }

    /** A box in its part's own space (model units, y down, the front toward -z), as vanilla's armor model places it. */
    private record Box(String part, String atlasBox, float[] origin, float[] size, float[] grow, float dx, float dy, boolean mirror) {
    }

    private static final float THIRD = 1.0F / 3.0F;
    private static final float[] HEAD_O = {-4, -8, -4}, HEAD_S = {8, 8, 8};
    private static final float[] BODY_O = {-4, 0, -2}, BODY_S = {8, 12, 4};
    private static final float[] R_ARM_O = {-3, -2, -2}, L_ARM_O = {-1, -2, -2}, LIMB_S = {4, 12, 4};
    private static final float[] LEG_O = {-2, 0, -2};

    private static float[] even(float g) {
        return new float[]{g, g, g};
    }

    private static List<Box> boxes(ArmorUV.Slot slot) {
        return switch (slot) {
            case HELMET -> List.of(new Box("head", ArmorUV.HEAD, HEAD_O, HEAD_S, even(1.0F), 0, 0, false));
            case CHESTPLATE -> List.of(
                    new Box("body", ArmorUV.BODY, BODY_O, BODY_S, even(1.0F), 0, 0, false),
                    new Box("right_arm", ArmorUV.R_ARM, R_ARM_O, LIMB_S, even(1.1F), 0, 0, false),
                    new Box("left_arm", ArmorUV.R_ARM, L_ARM_O, LIMB_S, even(1.1F), 0, 0, true));
            // Legs and hips 1.1 units a texel each way; the hips end where the legs begin (0.05 over), 0.05 in front and 0.05 wider.
            // Each leg grows less across and moves out, so the two meet at x = 0 instead of one covering the other's inner edge.
            case LEGGINGS -> List.of(
                    new Box("body", ArmorUV.BODY, BODY_O, BODY_S, new float[]{0.45F, 0.6F, 0.25F}, 0, -1.15F, false),
                    new Box("right_leg", ArmorUV.R_LEG, LEG_O, LIMB_S, new float[]{0.2F, 0.6F, 0.2F}, -0.3F, 0, false),
                    new Box("left_leg", ArmorUV.R_LEG, LEG_O, LIMB_S, new float[]{0.2F, 0.6F, 0.2F}, 0.3F, 0, true));
            // 7/6 units a texel each way, 0.13 outside the leggings.
            case BOOTS -> List.of(
                    new Box("right_leg", ArmorUV.R_LEG, LEG_O, LIMB_S, new float[]{THIRD, 1.0F, THIRD}, -(0.1F + THIRD), 0, false),
                    new Box("left_leg", ArmorUV.R_LEG, LEG_O, LIMB_S, new float[]{THIRD, 1.0F, THIRD}, 0.1F + THIRD, 0, true));
        };
    }

    private static final List<String> PARTS = List.of("head", "hat", "body", "right_arm", "left_arm", "right_leg", "left_leg");

    /** @param uv the piece's table, whose slot says which parts get boxes */
    public static HumanoidModel<LivingEntity> build(ArmorUV uv) {
        Map<String, List<ModelPart.Cube>> cubes = cubes(uv);
        Map<String, ModelPart> children = new HashMap<>();
        for (String part : PARTS) {
            children.put(part, new ModelPart(cubes.getOrDefault(part, List.of()), Map.of()));
        }
        return new HumanoidModel<>(new ModelPart(List.of(), children));
    }

    /** @return each part's boxes, in the part's own space: the vanilla model's pose puts them on the body */
    static Map<String, List<ModelPart.Cube>> cubes(ArmorUV uv) {
        Map<String, List<ModelPart.Cube>> cubes = new HashMap<>();
        for (Box box : boxes(uv.slot())) {
            cubes.computeIfAbsent(box.part, key -> new ArrayList<>()).add(new FaceCube(box, uv));
        }
        return cubes;
    }

    /** A box whose faces each read their own block of the picture. */
    static final class FaceCube extends ModelPart.Cube {
        /** A hair inside each face: on an exact boundary, nearest sampling can take the neighbouring block's texel. */
        private static final float INSET = 0.002F;
        private static final float[][] CORNERS = {{0, 0}, {1, 0}, {1, 1}, {0, 1}};

        /** Per face: 4 corners of x, y, z (blocks), u, v. */
        private final List<float[]> quads = new ArrayList<>();
        private final List<float[]> normals = new ArrayList<>();

        FaceCube(Box box, ArmorUV uv) {
            // The parent's box is kept for anything reading min and max; it makes no polygons of its own.
            super(0, 0, box.origin[0], box.origin[1], box.origin[2], box.size[0], box.size[1], box.size[2],
                    box.grow[0], box.grow[1], box.grow[2], box.mirror, 64, 32, Set.of());
            float[] lo = {box.origin[0] - box.grow[0] + box.dx, box.origin[1] - box.grow[1] + box.dy, box.origin[2] - box.grow[2]};
            float[] size = {box.size[0] + 2 * box.grow[0], box.size[1] + 2 * box.grow[1], box.size[2] + 2 * box.grow[2]};
            for (String face : ArmorUV.FACE_NAMES) {
                ArmorUV.Rect rect = uv.face(box.atlasBox, face);
                if (rect == null) {
                    continue;
                }
                float[] quad = new float[20];
                for (int i = 0; i < 4; i++) {
                    float s = CORNERS[i][0], t = CORNERS[i][1];
                    float[] p = position(face, s, t);
                    if (box.mirror) {
                        p[0] = 1 - p[0];
                    }
                    quad[i * 5] = (lo[0] + p[0] * size[0]) / 16.0F;
                    quad[i * 5 + 1] = (lo[1] + p[1] * size[1]) / 16.0F;
                    quad[i * 5 + 2] = (lo[2] + p[2] * size[2]) / 16.0F;
                    float se = INSET + s * (1 - 2 * INSET), te = INSET + t * (1 - 2 * INSET);
                    quad[i * 5 + 3] = (rect.x() + se * rect.w()) / uv.width();
                    quad[i * 5 + 4] = (rect.y() + te * rect.h()) / uv.height();
                }
                float[] n = normal(face);
                if (box.mirror && (face.equals("right") || face.equals("left"))) {
                    n[0] = -n[0];
                }
                quads.add(quad);
                normals.add(n);
            }
        }

        /** Corner (s, t) of a face in the unit box, s along the face's block left to right and t top to bottom. */
        private static float[] position(String face, float s, float t) {
            return switch (face) {
                case "front" -> new float[]{s, t, 0};
                case "back" -> new float[]{1 - s, t, 1};
                case "right" -> new float[]{0, t, 1 - s};
                case "left" -> new float[]{1, t, s};
                case "top" -> new float[]{s, 0, 1 - t};
                case "bottom" -> new float[]{s, 1, 1 - t};
                default -> throw new IllegalArgumentException(face);
            };
        }

        /** Outward, in model space (y down, the front toward -z). */
        private static float[] normal(String face) {
            return switch (face) {
                case "front" -> new float[]{0, 0, -1};
                case "back" -> new float[]{0, 0, 1};
                case "right" -> new float[]{-1, 0, 0};
                case "left" -> new float[]{1, 0, 0};
                case "top" -> new float[]{0, -1, 0};
                case "bottom" -> new float[]{0, 1, 0};
                default -> throw new IllegalArgumentException(face);
            };
        }

        @Override
        public void compile(PoseStack.Pose pose, VertexConsumer consumer, int light, int overlay, float red, float green, float blue, float alpha) {
            Matrix4f matrix = pose.pose();
            Matrix3f normalMatrix = pose.normal();
            for (int q = 0; q < quads.size(); q++) {
                float[] quad = quads.get(q);
                float[] n = normals.get(q);
                Vector3f normal = normalMatrix.transform(new Vector3f(n[0], n[1], n[2]));
                for (int i = 0; i < 4; i++) {
                    Vector4f p = matrix.transform(new Vector4f(quad[i * 5], quad[i * 5 + 1], quad[i * 5 + 2], 1.0F));
                    consumer.vertex(p.x(), p.y(), p.z(), red, green, blue, alpha, quad[i * 5 + 3], quad[i * 5 + 4], overlay, light,
                            normal.x(), normal.y(), normal.z());
                }
            }
        }
    }
}
