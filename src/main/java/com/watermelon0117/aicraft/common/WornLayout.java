package com.watermelon0117.aicraft.common;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Where every face of every view lies on the 1536x1024 picture the image model paints armor on
 * (the "W" method of {@code test/armor_texture2/worn.py}, round 10): orthographic views of a flat
 * dark mannequin wearing the slot's armor as flat light gray, at vanilla's coverage, with the mod's
 * part sizes.
 *
 * <p>A line-for-line port: the reader must give the Python's output pixel for pixel, so every
 * rounding (half to even), order and comparison follows it. The same file is in the mod
 * ({@code com.watermelon0117.aicraft.common}); the two differ only in the package line.
 */
public final class WornLayout {
    public static final int W = 1536, H = 1024;
    static final int MARGIN = 40, VIEW_GAP = 64;
    static final double MAX_S = 32;
    static final int LIGHT = 0xC8C8C8, DUMMY = 0x545C6A;
    static final double GAP = 0.5;
    static final double B3 = 1.0 / 3;

    public static final List<String> SLOTS = List.of("helmet", "chestplate", "leggings", "boots");

    /** armorlib: (texOffs u, v), box origin, size, part offset, mirror. Model units, y down. */
    record Box(String name, int u0, int v0, double x0, double y0, double z0, int w, int h, int d,
               double px, double py, double pz, boolean mirror) {
    }

    static final Box HEAD = new Box("HEAD", 0, 0, -4, -8, -4, 8, 8, 8, 0, 0, 0, false);
    static final Box BODY = new Box("BODY", 16, 16, -4, 0, -2, 8, 12, 4, 0, 0, 0, false);
    static final Box R_ARM = new Box("R_ARM", 40, 16, -3, -2, -2, 4, 12, 4, -5, 2, 0, false);
    static final Box L_ARM = new Box("L_ARM", 40, 16, -1, -2, -2, 4, 12, 4, 5, 2, 0, true);
    static final Box R_LEG = new Box("R_LEG", 0, 16, -2, 0, -2, 4, 12, 4, -1.9, 12, 0, false);
    static final Box L_LEG = new Box("L_LEG", 0, 16, -2, 0, -2, 4, 12, 4, 1.9, 12, 0, true);
    static final List<Box> MANNEQUIN = List.of(HEAD, BODY, R_ARM, L_ARM, R_LEG, L_LEG);

    /** (x, y, z growth, x shift, y shift); a plain number grows every side alike. */
    record Inflate(double gx, double gy, double gz, double dx, double dy) {
        static Inflate of(double g) {
            return new Inflate(g, g, g, 0.0, 0.0);
        }
    }

    record SlotPart(Box box, Inflate inflate) {
    }

    /** armorlib.SLOT_PARTS_NO_FIGHT without the helmet's hat layer, which worn.parts skips. */
    static final Map<String, List<SlotPart>> SLOT_PARTS = Map.of(
            "helmet", List.of(new SlotPart(HEAD, Inflate.of(1.0))),
            "chestplate", List.of(new SlotPart(BODY, Inflate.of(1.0)), new SlotPart(R_ARM, Inflate.of(1.1)),
                    new SlotPart(L_ARM, Inflate.of(1.1))),
            "leggings", List.of(new SlotPart(BODY, new Inflate(0.45, 0.6, 0.25, 0.0, -1.15)),
                    new SlotPart(R_LEG, new Inflate(0.2, 0.6, 0.2, -0.3, 0.0)),
                    new SlotPart(L_LEG, new Inflate(0.2, 0.6, 0.2, 0.3, 0.0))),
            "boots", List.of(new SlotPart(R_LEG, new Inflate(B3, 1.0, B3, -(0.1 + B3), 0.0)),
                    new SlotPart(L_LEG, new Inflate(B3, 1.0, B3, 0.1 + B3, 0.0))));

    static final Map<String, List<String>> READ_BOXES = Map.of(
            "helmet", List.of("HEAD"), "chestplate", List.of("BODY", "R_ARM"),
            "leggings", List.of("BODY", "R_LEG"), "boots", List.of("R_LEG"));

    static final Map<String, List<String>> SLOT_VIEWS = Map.of(
            "helmet", List.of("front", "back", "right", "left", "top"),
            "chestplate", List.of("front", "back", "right", "left", "top"),
            "leggings", List.of("front", "back", "right", "left"),
            "boots", List.of("front", "back", "right", "left"));

    static final Map<String, double[]> WINDOW = Map.of(
            "helmet", new double[]{-10.5, 5.0}, "chestplate", new double[]{-9.0, 15.5},
            "leggings", new double[]{-3.0, 26.0}, "boots", new double[]{7.0, 26.0});

    static final List<String> FACES = List.of("front", "back", "right", "left", "top", "bottom");

    /** Outward normal (axis, sign) of each face of an unmirrored box. */
    static int[] normalOf(String face) {
        return switch (face) {
            case "front" -> new int[]{2, -1};
            case "back" -> new int[]{2, 1};
            case "right" -> new int[]{0, -1};
            case "left" -> new int[]{0, 1};
            case "top" -> new int[]{1, -1};
            default -> new int[]{1, 1};
        };
    }

    /** The normal a view sees: each view sees the faces whose outward normal points at it. */
    static int[] viewNormal(String view) {
        return switch (view) {
            case "front" -> new int[]{2, -1};
            case "back" -> new int[]{2, 1};
            case "right" -> new int[]{0, -1};
            case "left" -> new int[]{0, 1};
            default -> new int[]{1, -1};
        };
    }

    /** (u, v): the image position of a world point, image right and down. */
    static double[] project(String view, double[] p) {
        return switch (view) {
            case "front" -> new double[]{p[0], p[1]};
            case "back" -> new double[]{-p[0], p[1]};
            case "right" -> new double[]{-p[2], p[1]};
            case "left" -> new double[]{p[2], p[1]};
            default -> new double[]{p[0], -p[2]};
        };
    }

    /** Vanilla's coverage, the texels of the 64x32 layer vanilla paints for each slot (prompts.coverage). */
    private static final Map<String, String[]> COVERAGE = Map.of(
            "helmet", new String[]{
                    "0 ........########",
                    "1 ........########",
                    "2 ........########",
                    "3 ........########",
                    "4 ........########",
                    "5 ........########",
                    "6 ........########",
                    "7 ........########",
                    "8 ################################",
                    "9 ################################",
                    "10 ################################",
                    "11 #########..##..#################",
                    "12 ####.......##.......############",
                    "13 ........................########",
                    "14 ..........................####"},
            "chestplate", new String[]{
                    "16 ............................................####",
                    "17 ............................................####",
                    "18 ............................................####",
                    "19 ............................................####",
                    "20 ................######....########....##################",
                    "21 ................#######..###############################",
                    "22 ................########################################",
                    "23 ................########################################",
                    "24 ................########################################",
                    "25 ................########################################",
                    "26 ................########################",
                    "27 ................########################",
                    "28 ................########################",
                    "29 .....................######......######",
                    "30 ......................####"},
            "leggings", new String[]{
                    "16 ....####",
                    "17 ....####",
                    "18 ....####",
                    "19 ....####",
                    "20 ################",
                    "21 ################",
                    "22 ################",
                    "23 ################",
                    "24 ################",
                    "25 ################",
                    "26 ################",
                    "27 ########################################",
                    "28 ########################################",
                    "29 ................########################",
                    "30 ................########################",
                    "31 ................########################"},
            "boots", new String[]{
                    "16 ........####",
                    "17 ........####",
                    "18 ........####",
                    "19 ........####",
                    "26 ################",
                    "27 ################",
                    "28 ################",
                    "29 ################",
                    "30 ################",
                    "31 ################"});

    static boolean[][] coverage(String slot) {
        boolean[][] out = new boolean[32][64];
        for (String line : COVERAGE.get(slot)) {
            int space = line.indexOf(' ');
            int y = Integer.parseInt(line.substring(0, space));
            String row = line.substring(space + 1);
            for (int x = 0; x < row.length(); x++) {
                out[y][x] = row.charAt(x) == '#';
            }
        }
        return out;
    }

    // ---------------------------------------------------------------- geometry

    /** One face: corners p00, p10 (s = 1), p01 (t = 1) in world units, texel size and texel origin. */
    record Face(String face, int axis, int sign, double[] p00, double[] p10, double[] p01, int fw, int fh, int tx, int ty) {
        boolean sameNormal(int[] n) {
            return axis == n[0] && sign == n[1];
        }
    }

    record Part(boolean armor, String name, Box box, double[] lo, double[] size) {
    }

    static double spread(String name) {
        return switch (name) {
            case "R_ARM" -> -(2.1 + GAP);
            case "L_ARM" -> 2.1 + GAP;
            case "R_LEG" -> -GAP / 2;
            case "L_LEG" -> GAP / 2;
            default -> 0.0;
        };
    }

    static Part placed(boolean armor, Box box, Inflate g, String name) {
        double[] lo = {box.x0 - g.gx + box.px + g.dx + spread(name), box.y0 - g.gy + box.py + g.dy, box.z0 - g.gz + box.pz};
        double[] size = {box.w + 2 * g.gx, box.h + 2 * g.gy, box.d + 2 * g.gz};
        return new Part(armor, name, box, lo, size);
    }

    /** The armor and the mannequin in the input pose (worn.parts). */
    static List<Part> parts(String slot) {
        List<Part> out = new ArrayList<>();
        for (SlotPart p : SLOT_PARTS.get(slot)) {
            out.add(placed(true, p.box, p.inflate, p.box.name));
        }
        for (Box b : MANNEQUIN) {
            if (!slot.equals("chestplate") && b.name.contains("ARM")) {
                continue;
            }
            out.add(placed(false, b, Inflate.of(0.0), b.name));
        }
        return out;
    }

    /** (a, b, c) in the unit box of a face at (s, t) (armorlib.faces_of). */
    private static double[] unit(String face, double s, double t) {
        return switch (face) {
            case "front" -> new double[]{s, t, 0 * s};
            case "back" -> new double[]{1 - s, t, 1 + 0 * s};
            case "right" -> new double[]{0 * s, t, 1 - s};
            case "left" -> new double[]{1 + 0 * s, t, s};
            case "top" -> new double[]{s, 0 * s, 1 - t};
            default -> new double[]{s, 1 + 0 * s, 1 - t};
        };
    }

    static List<Face> faceList(Box box, double[] lo, double[] size) {
        List<Face> out = new ArrayList<>();
        int w = box.w, h = box.h, d = box.d, u0 = box.u0, v0 = box.v0;
        for (String face : FACES) {
            int[] n = normalOf(face);
            int axis = n[0], sign = n[1];
            if (box.mirror && axis == 0) {
                sign = -sign;
            }
            int fw, fh, tx, ty;
            switch (face) {
                case "front" -> { fw = w; fh = h; tx = u0 + d; ty = v0 + d; }
                case "back" -> { fw = w; fh = h; tx = u0 + 2 * d + w; ty = v0 + d; }
                case "right" -> { fw = d; fh = h; tx = u0; ty = v0 + d; }
                case "left" -> { fw = d; fh = h; tx = u0 + d + w; ty = v0 + d; }
                case "top" -> { fw = w; fh = d; tx = u0 + d; ty = v0; }
                default -> { fw = w; fh = d; tx = u0 + d + w; ty = v0; }
            }
            out.add(new Face(face, axis, sign, world(box, face, lo, size, 0, 0), world(box, face, lo, size, 1, 0),
                    world(box, face, lo, size, 0, 1), fw, fh, tx, ty));
        }
        return out;
    }

    static Face faceOf(Part part, String face) {
        for (Face f : faceList(part.box, part.lo, part.size)) {
            if (f.face.equals(face)) {
                return f;
            }
        }
        throw new IllegalArgumentException(face);
    }

    private static double[] world(Box box, String face, double[] lo, double[] size, double s, double t) {
        double[] p = unit(face, s, t);
        if (box.mirror) {
            p[0] = 1 - p[0];
        }
        return new double[]{lo[0] + p[0] * size[0], lo[1] + p[1] * size[1], lo[2] + p[2] * size[2]};
    }

    // ---------------------------------------------------------------- the layout

    /** One face as one view shows it. */
    static final class Item {
        final boolean armor;
        final String box;
        final Face face;
        final double[] a, b, c;
        final double depth;
        double[] px;  // x0, y0, x1, y1 on the canvas
        String view;

        Item(boolean armor, String box, Face face, double[] a, double[] b, double[] c, double depth) {
            this.armor = armor;
            this.box = box;
            this.face = face;
            this.a = a;
            this.b = b;
            this.c = c;
            this.depth = depth;
        }

        int fw() {
            return face.fw;
        }

        int fh() {
            return face.fh;
        }
    }

    /** One view's items, nearest last, and who owns each pixel of its part of the canvas. */
    static final class View {
        final String name;
        final List<Item> items = new ArrayList<>();
        double[] rect;  // u0, v0, u1, v1 in model units
        double ox, oy;
        int bx0, by0, bx1, by1;  // the pixels this view's items cover
        byte[] owner, ownerCov;   // full rectangles; vanilla's coverage only (as the input shows it)

        View(String name) {
            this.name = name;
        }

        int owner(int x, int y) {
            return inside(x, y) ? owner[(y - by0) * (bx1 - bx0) + x - bx0] : -1;
        }

        int ownerCov(int x, int y) {
            return inside(x, y) ? ownerCov[(y - by0) * (bx1 - bx0) + x - bx0] : -1;
        }

        double depthCov(int x, int y) {
            int k = ownerCov(x, y);
            return k < 0 ? Double.NEGATIVE_INFINITY : items.get(k).depth;
        }

        boolean isArmor(int k) {
            return k >= 0 && items.get(k).armor;
        }

        private boolean inside(int x, int y) {
            return x >= bx0 && x < bx1 && y >= by0 && y < by1;
        }
    }

    final String slot;
    final boolean[][] cov;
    final List<String> views;
    final Map<String, View> byView = new LinkedHashMap<>();
    final double S;

    private WornLayout(String slot) {
        this.slot = slot;
        this.cov = coverage(slot);
        this.views = SLOT_VIEWS.get(slot);
        double loV = WINDOW.get(slot)[0], hiV = WINDOW.get(slot)[1];
        for (String v : views) {
            byView.put(v, new View(v));
        }
        for (Part part : parts(slot)) {
            for (Face f : faceList(part.box, part.lo, part.size)) {
                for (String v : views) {
                    if (!f.sameNormal(viewNormal(v))) {
                        continue;
                    }
                    double[] a = project(v, f.p00);
                    double[] pb = project(v, f.p10), pc = project(v, f.p01);
                    double[] b = {pb[0] - a[0], pb[1] - a[1]};
                    double[] c = {pc[0] - a[0], pc[1] - a[1]};
                    double[][] corners = {a, {a[0] + b[0], a[1] + b[1]}, {a[0] + c[0], a[1] + c[1]},
                            {a[0] + b[0] + c[0], a[1] + b[1] + c[1]}};
                    double u0 = corners[0][0], v0 = corners[0][1], u1 = corners[0][0], v1 = corners[0][1];
                    for (double[] p : corners) {
                        u0 = Math.min(u0, p[0]);
                        v0 = Math.min(v0, p[1]);
                        u1 = Math.max(u1, p[0]);
                        v1 = Math.max(v1, p[1]);
                    }
                    if (!v.equals("top")) {
                        v0 = Math.max(v0, loV);
                        v1 = Math.min(v1, hiV);
                    }
                    if (v1 <= v0) {
                        continue;
                    }
                    Item item = new Item(part.armor, part.name, f, a, b, c, f.sign * (f.axis == 0 ? f.p00[0] : f.axis == 1 ? f.p00[1] : f.p00[2]));
                    item.view = v;
                    item.px = new double[]{u0, v0, u1, v1};  // model units until scaled below
                    byView.get(v).items.add(item);
                }
            }
        }
        // Scale: row 1 holds every view but the top one, row 2 the top view.
        for (View view : byView.values()) {
            double u0 = Double.POSITIVE_INFINITY, v0 = Double.POSITIVE_INFINITY, u1 = Double.NEGATIVE_INFINITY, v1 = Double.NEGATIVE_INFINITY;
            for (Item i : view.items) {
                u0 = Math.min(u0, i.px[0]);
                v0 = Math.min(v0, i.px[1]);
                u1 = Math.max(u1, i.px[2]);
                v1 = Math.max(v1, i.px[3]);
            }
            view.rect = new double[]{u0, v0, u1, v1};
        }
        List<View> row1 = new ArrayList<>();
        for (View view : byView.values()) {
            if (!view.name.equals("top")) {
                row1.add(view);
            }
        }
        View top = byView.get("top");
        double width1 = 0.0;
        for (View view : row1) {
            width1 += view.rect[2] - view.rect[0];
        }
        double maxH = Double.NEGATIVE_INFINITY;
        for (View view : row1) {
            maxH = Math.max(maxH, view.rect[3] - view.rect[1]);
        }
        double height = maxH + (top != null ? top.rect[3] - top.rect[1] : 0);
        int gapsY = top != null ? VIEW_GAP : 0;
        double s = Math.min(MAX_S, Math.min((W - 2 * MARGIN - VIEW_GAP * (row1.size() - 1)) / width1,
                (H - 2 * MARGIN - gapsY) / height));
        this.S = Math.floor(s * 4) / 4;
        double h1 = maxH * S;
        double totalH = h1 + gapsY + (top != null ? (top.rect[3] - top.rect[1]) * S : 0);
        double topY = Math.rint((H - totalH) / 2);
        double x = Math.rint((W - (width1 * S + VIEW_GAP * (row1.size() - 1))) / 2);
        for (View view : row1) {
            double u0 = view.rect[0], v0 = view.rect[1], u1 = view.rect[2], v1 = view.rect[3];
            // Views of one row share their v so the body lines up; each is centered in its own column.
            view.ox = x - u0 * S;
            view.oy = topY + (h1 - (v1 - v0) * S) / 2 - v0 * S;
            x += (u1 - u0) * S + VIEW_GAP;
        }
        if (top != null) {
            top.ox = byView.get("front").ox;
            top.oy = topY + h1 + gapsY - top.rect[1] * S;  // under the front view, same x
        }
        for (View view : byView.values()) {
            for (Item i : view.items) {
                double u0 = i.px[0], v0 = i.px[1], u1 = i.px[2], v1 = i.px[3];
                i.px = new double[]{view.ox + u0 * S, view.oy + v0 * S, view.ox + u1 * S, view.oy + v1 * S};
            }
            view.items.sort(Comparator.comparingDouble(i -> i.depth));  // stable, as Python's sort
        }
        owners();
    }

    public static WornLayout of(String slot) {
        if (!SLOTS.contains(slot)) {
            throw new IllegalArgumentException("unknown armor slot: " + slot);
        }
        return new WornLayout(slot);
    }

    boolean faceCovered(Face f, int tj, int ti) {
        return cov[f.ty + tj][f.tx + ti];
    }

    boolean anyCovered(Face f) {
        for (int j = 0; j < f.fh; j++) {
            for (int i = 0; i < f.fw; i++) {
                if (cov[f.ty + j][f.tx + i]) {
                    return true;
                }
            }
        }
        return false;
    }

    static int[] pixelBox(double[] px) {
        return new int[]{(int) Math.max(0, Math.ceil(px[0] - 0.5)), (int) Math.max(0, Math.ceil(px[1] - 0.5)),
                (int) Math.min(W, Math.ceil(px[2] - 0.5)), (int) Math.min(H, Math.ceil(px[3] - 0.5))};
    }

    /** (s, t) of the pixel centre (x, y) on an item: np.linalg.solve on its matrix, which is always diagonal. */
    double sOf(Item i, View v, int x) {
        return ((x + 0.5 - v.ox) / S - i.a[0]) / i.b[0];
    }

    double tOf(Item i, View v, int y) {
        return ((y + 0.5 - v.oy) / S - i.a[1]) / i.c[1];
    }

    /** Whether vanilla covers the texel under the pixel centre (x, y) of an armor item. */
    boolean coveredAt(Item i, View v, int x, int y) {
        int ti = (int) Math.min(Math.max(Math.floor(sOf(i, v, x) * i.fw()), 0), i.fw() - 1);
        int tj = (int) Math.min(Math.max(Math.floor(tOf(i, v, y) * i.fh()), 0), i.fh() - 1);
        return faceCovered(i.face, tj, ti);
    }

    /** Per view: which item owns each pixel, nearest last; armor as full rectangles, and as the input shows it. */
    private void owners() {
        for (View view : byView.values()) {
            for (Item i : view.items) {
                if (i.b[1] != 0 || i.c[0] != 0) {
                    throw new IllegalStateException("face not axis-aligned");
                }
            }
            int bx0 = W, by0 = H, bx1 = 0, by1 = 0;
            for (Item i : view.items) {
                int[] pb = pixelBox(i.px);
                if (pb[2] <= pb[0] || pb[3] <= pb[1]) {
                    continue;
                }
                bx0 = Math.min(bx0, pb[0]);
                by0 = Math.min(by0, pb[1]);
                bx1 = Math.max(bx1, pb[2]);
                by1 = Math.max(by1, pb[3]);
            }
            if (bx1 <= bx0) {
                bx0 = by0 = bx1 = by1 = 0;
            }
            view.bx0 = bx0;
            view.by0 = by0;
            view.bx1 = bx1;
            view.by1 = by1;
            int bw = bx1 - bx0;
            view.owner = new byte[bw * (by1 - by0)];
            view.ownerCov = new byte[bw * (by1 - by0)];
            Arrays.fill(view.owner, (byte) -1);
            Arrays.fill(view.ownerCov, (byte) -1);
            for (int k = 0; k < view.items.size(); k++) {
                Item i = view.items.get(k);
                int[] pb = pixelBox(i.px);
                for (int y = pb[1]; y < pb[3]; y++) {
                    for (int x = pb[0]; x < pb[2]; x++) {
                        int at = (y - by0) * bw + x - bx0;
                        view.owner[at] = (byte) k;
                        if (!i.armor || coveredAt(i, view, x, y)) {
                            view.ownerCov[at] = (byte) k;
                        }
                    }
                }
            }
        }
    }

    View view(String name) {
        return byView.get(name);
    }

    // ---------------------------------------------------------------- the model's inputs

    /** Input 1: the canvas, ARGB (worn.input_w). */
    public int[] draw() {
        int[] img = new int[W * H];
        for (View view : byView.values()) {
            for (Item i : view.items) {
                int[] pb = pixelBox(i.px);
                for (int y = pb[1]; y < pb[3]; y++) {
                    for (int x = pb[0]; x < pb[2]; x++) {
                        if (!i.armor) {
                            img[y * W + x] = 0xFF000000 | DUMMY;
                        } else if (coveredAt(i, view, x, y)) {
                            img[y * W + x] = 0xFF000000 | LIGHT;
                        }
                    }
                }
            }
        }
        return img;
    }

    public static BufferedImage input(String slot) {
        BufferedImage out = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        out.setRGB(0, 0, W, H, of(slot).draw(), 0, W);
        return out;
    }

    /** Input 2: the item's icon enlarged by a whole factor to about 512 px (worn.icon_input). */
    public static BufferedImage icon(BufferedImage sprite) {
        int k = Math.max(1, 512 / Math.max(sprite.getWidth(), sprite.getHeight()));
        BufferedImage out = new BufferedImage(sprite.getWidth() * k, sprite.getHeight() * k, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < out.getHeight(); y++) {
            for (int x = 0; x < out.getWidth(); x++) {
                out.setRGB(x, y, sprite.getRGB(x / k, y / k));
            }
        }
        return out;
    }

    private static final String SQUARE = "Minecraft armor is made of boxes. Keep it square and blocky: the light gray shapes have only "
            + "straight horizontal and vertical edges and square corners, and the armor keeps exactly that outline. Paint every light "
            + "gray pixel, right up to the edges and into every corner. Do not round, bevel, cut or step any corner, do not curve any "
            + "edge, and leave no see-through or unpainted spots on the light gray shapes.\n"
            + "Paint only on the light gray shapes. ";

    private static String viewWords(String view) {
        return switch (view) {
            case "front" -> "the front";
            case "back" -> "the back";
            case "right" -> "the character's right side";
            case "left" -> "the character's left side";
            default -> "from above, with the front toward the bottom of the image";
        };
    }

    /** The edit prompt (worn.prompt_w with square=True). */
    public static String prompt(String slot, String name, String description) {
        List<String> views = SLOT_VIEWS.get(slot);
        List<String> row1 = new ArrayList<>();
        for (String v : views) {
            if (!v.equals("top")) {
                row1.add(viewWords(v));
            }
        }
        String where = "from left to right: " + String.join(", ", row1.subList(0, row1.size() - 1)) + " and " + row1.get(row1.size() - 1);
        if (views.contains("top")) {
            where += "; below the front view, the same character seen " + viewWords("top");
        }
        String piece = switch (slot) {
            case "helmet" -> "a light gray helmet";
            case "chestplate" -> "a light gray chestplate";
            default -> "light gray " + slot;
        };
        return "Image 1 shows a Minecraft character, a plain dark gray mannequin, wearing " + piece + ", in several views "
                + "(" + where + "). Every view shows the same " + slot + " turned a different way.\n"
                + "Image 2 is the item " + name + ".\n"
                + "Description: " + description + "\n"
                + "Paint the light gray " + slot + " as this item's armor, as vanilla-style Minecraft armor pixel art: armor plates, rims and "
                + "edges, two or three shades of each color and a few bright highlight pixels. Use the item's colors, materials and features, "
                + "but do not paste the item picture onto the armor.\n"
                + "Paint the same design in every view, so the views fit together around the body.\n"
                + SQUARE
                + "Leave the dark gray mannequin and the transparent background exactly as they are. No text, no labels, no shadow.";
    }
}
