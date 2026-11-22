package com.watermelon0117.aicraft.common;

import java.awt.image.BufferedImage;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Reads the armor an image model painted on the {@link WornLayout} picture into an atlas (one block
 * per face) and a UV table: {@code worn.read_w} of {@code test/armor_texture2} (round 10), ported
 * line for line. Each view is read on its own lattice, fitted to the edges the model painted, as
 * the item pixelizer reads a sprite; each face is framed where the model drew it; each cell takes
 * the representative color of its middle; faces no view shows are filled from the others.
 *
 * <p>The Python's measures are left out: nothing in the atlas or the table depends on them.
 */
public final class WornReader {
    static final int W = WornLayout.W, H = WornLayout.H;
    static final double INSET = 0.18;
    static final double CLEAR_DUMMY = 0.04;   // OKLab distance under which a cell counts as the mannequin
    static final double MIN_DENSITY = 0.5;
    static final int MAX_DENSITY = 2;
    static final double KEEP_FINE = 0.5;
    static final double REACH = 2.0;          // model units
    static final double EDGE_IN = 1.0;        // cells
    static final double MANNEQUIN_OWN = 0.02;
    static final double PALETTE_MATCH = 0.012;
    // grid_processor.Settings as read_w uses them
    static final double MIN_STEP = 8.0, STEP_INCREMENT = 0.25, CONTRAST_FLOOR = 12.0, HARMONIC_SCORE_SLACK = 0.012;
    static final int N = W * H;

    /** The atlas (ARGB) and the UV table as JSON: slot, step, size, faces {"BOX.face": [x, y, w, h, source]}, view_steps. */
    public record Result(BufferedImage atlas, String uv) {
    }

    private final WornLayout L;
    private final String slot;
    private final int[] argb;
    private final int[] labels = new int[N];
    private final double[][] dummyLabs = new double[2][];
    private BitSet likeDummy, mannequinPx, paintedArmor;
    // Drawing
    private byte[] index, owner, nearest;
    private BitSet within;
    private final List<WornLayout.Item> entries = new ArrayList<>();
    private final List<WornLayout.View> entryViews = new ArrayList<>();
    private final Map<WornLayout.Item, Integer> number = new LinkedHashMap<>();
    // Round 11: arm cells left to the body side, in the order read
    private final List<ToBody> toBody = new ArrayList<>();

    /** An arm cell's color left to the body side: the view, the cell centre in the drawing, the body side's face key. */
    private record ToBody(String view, double px, double py, int[] color, String key) {
    }

    private WornReader(WornLayout layout, int[] argb) {
        this.L = layout;
        this.slot = layout.slot;
        this.argb = argb;
    }

    /** Empty when no view has a lattice (worn's 'no lattice'). */
    public static Optional<Result> read(BufferedImage image, String slot) {
        if (image.getWidth() != W || image.getHeight() != H) {
            throw new IllegalArgumentException("armor picture must be " + W + "x" + H + ", got "
                    + image.getWidth() + "x" + image.getHeight());
        }
        int[] argb = image.getRGB(0, 0, W, H, null, 0, W);
        return new WornReader(WornLayout.of(slot), argb).run();
    }

    private boolean alpha(int p) {
        return (argb[p] >>> 24) > 127;
    }

    // ---------------------------------------------------------------- read_w

    private Optional<Result> run() {
        BitSet armor = new BitSet(N), dummyMask = new BitSet(N);
        for (WornLayout.View view : L.byView.values()) {
            for (int k = 0; k < view.items.size(); k++) {
                WornLayout.Item i = view.items.get(k);
                int[] pb = WornLayout.pixelBox(i.px);
                for (int y = pb[1]; y < pb[3]; y++) {
                    for (int x = pb[0]; x < pb[2]; x++) {
                        if (view.owner(x, y) == k) {
                            (i.armor ? armor : dummyMask).set(y * W + x);
                        }
                    }
                }
            }
        }
        // The mannequin as the model returned it: the representative color of mannequin pixels at least 8px from any armor face.
        boolean[] near = new boolean[N], next = new boolean[N];
        for (int p = armor.nextSetBit(0); p >= 0; p = armor.nextSetBit(p + 1)) {
            near[p] = true;
        }
        for (int pass = 0; pass < 8; pass++) {
            System.arraycopy(near, 0, next, 0, N);
            for (int y = 1; y < H - 1; y++) {
                for (int x = 1; x < W - 1; x++) {
                    int p = y * W + x;
                    next[p] = near[p] || near[p - W] || near[p + W] || near[p - 1] || near[p + 1];
                }
            }
            boolean[] t = near;
            near = next;
            next = t;
        }
        next = null;
        BitSet away = new BitSet(N);
        int awayCount = 0;
        for (int p = dummyMask.nextSetBit(0); p >= 0; p = dummyMask.nextSetBit(p + 1)) {
            if (alpha(p) && !near[p]) {
                away.set(p);
                awayCount++;
            }
        }
        near = null;
        int[] seen = awayCount > 100 ? representativeColor(away) : rgbOf(WornLayout.DUMMY);
        dummyLabs[0] = WornColor.oklab(rgbOf(WornLayout.DUMMY));
        dummyLabs[1] = WornColor.oklab(seen);
        likeDummy = new BitSet(N);
        paintedArmor = new BitSet(N);
        for (int p = 0; p < N; p++) {
            if (alpha(p)) {
                if (dummyDist(p) < CLEAR_DUMMY) {
                    likeDummy.set(p);
                } else {
                    paintedArmor.set(p);
                }
            }
        }
        mannequinOnGray();

        List<Integer> readFaces = new ArrayList<>();  // indices into the drawing's entries, in read_faces order
        drawing();
        for (int n = 0; n < entries.size(); n++) {
            WornLayout.Item i = entries.get(n);
            if (i.armor && WornLayout.READ_BOXES.get(slot).contains(i.box)) {
                readFaces.add(n);
            }
        }
        // Every view on its own lattice, as an item sprite.
        Map<String, Double> steps = new LinkedHashMap<>();
        Map<String, double[]> phases = new LinkedHashMap<>();
        for (String v : L.views) {
            List<Integer> mine = new ArrayList<>();
            for (int n : readFaces) {
                if (entryViews.get(n).name.equals(v)) {
                    mine.add(n);
                }
            }
            if (mine.isEmpty()) {
                continue;
            }
            boolean[] isRead = new boolean[entries.size()], isArmor = new boolean[entries.size()];
            for (int n : mine) {
                isRead[n] = true;
            }
            for (int n = 0; n < entries.size(); n++) {
                isArmor[n] = entryViews.get(n).name.equals(v) && entries.get(n).armor;
            }
            BitSet mask = new BitSet(N), whole = new BitSet(N);
            for (int p = paintedArmor.nextSetBit(0); p >= 0; p = paintedArmor.nextSetBit(p + 1)) {
                int n = index[p];
                if (n >= 0 && isRead[n]) {
                    mask.set(p);
                }
                if (n >= 0 && isArmor[n]) {
                    whole.set(p);
                }
            }
            int maskCount = mask.cardinality();
            if (0 < maskCount && maskCount < 0.25 * whole.cardinality()) {
                mask = whole;
                maskCount = whole.cardinality();
            }
            double floorStep = Double.NEGATIVE_INFINITY, ceilingStep = Double.POSITIVE_INFINITY;
            for (int n : mine) {
                WornLayout.Item i = entries.get(n);
                floorStep = Math.max(floorStep, Math.max((i.px[2] - i.px[0]) / (MAX_DENSITY * i.fw()),
                        (i.px[3] - i.px[1]) / (MAX_DENSITY * i.fh())));
                ceilingStep = Math.min(ceilingStep, Math.min((i.px[2] - i.px[0]) / (MIN_DENSITY * i.fw()),
                        (i.px[3] - i.px[1]) / (MIN_DENSITY * i.fh())));
            }
            double[] fit = maskCount >= 16 ? WornGrid.fitView(argb, mask, floorStep, ceilingStep) : null;
            if (fit == null) {
                continue;
            }
            steps.put(v, fit[0]);
            phases.put(v, new double[]{fit[1], fit[2]});
        }
        if (steps.isEmpty()) {
            return Optional.empty();
        }
        double step = median(steps.values().stream().mapToDouble(Double::doubleValue).toArray());

        Map<String, FaceCells> faces = new LinkedHashMap<>();
        for (int n : readFaces) {
            WornLayout.Item i = entries.get(n);
            WornLayout.View view = entryViews.get(n);
            String key = i.box + "." + i.face.face();
            if (faces.containsKey(key) || !steps.containsKey(view.name)) {
                continue;
            }
            int k = view.items.indexOf(i);
            FaceCells read = readFace(n, view, k, i, steps.get(view.name), phases.get(view.name));
            if (read != null) {
                faces.put(key, read);
            }
        }
        // Round 11: the body side's cell at the same spot of the drawing takes the color, unless it read that spot itself.
        for (ToBody t : toBody) {
            FaceCells g = faces.get(t.key());
            if (g == null || !t.view().equals(g.view)) {
                continue;
            }
            int q = countBelow(g.xs, t.px()) - 1, j = countBelow(g.ys, t.py()) - 1;
            if (q < 0 || q >= g.xs.length - 1 || j < 0 || j >= g.ys.length - 1 || g.state[j * g.nx + q] == 1) {
                continue;
            }
            g.cells[j * g.nx + q] = 0xFF000000 | t.color()[0] << 16 | t.color()[1] << 8 | t.color()[2];
            g.state[j * g.nx + q] = 1;
        }
        // Faces no view shows.
        for (WornLayout.Part part : WornLayout.parts(slot)) {
            if (!part.armor() || !WornLayout.READ_BOXES.get(slot).contains(part.name())) {
                continue;
            }
            for (WornLayout.Face f : WornLayout.faceList(part.box(), part.lo(), part.size())) {
                String key = part.name() + "." + f.face();
                if (faces.containsKey(key) || !L.anyCovered(f)) {
                    continue;
                }
                FaceCells outer = faces.get(part.name() + ".right");
                if ((part.name().equals("R_ARM") || part.name().equals("R_LEG")) && f.face().equals("left") && outer != null) {
                    faces.put(key, outer.mirrored(part.name(), f));
                    continue;
                }
                double w = norm(f.p10()[0] - f.p00()[0], f.p10()[1] - f.p00()[1], f.p10()[2] - f.p00()[2]) * L.S;
                double h = norm(f.p01()[0] - f.p00()[0], f.p01()[1] - f.p00()[1], f.p01()[2] - f.p00()[2]) * L.S;
                Double facing = null;
                for (Map.Entry<String, Double> e : steps.entrySet()) {
                    if (f.sameNormal(WornLayout.viewNormal(e.getKey()))) {
                        facing = e.getValue();
                        break;
                    }
                }
                double sf = facing != null ? facing : step;
                int nx = (int) Math.max(1, Math.rint(w / sf)), ny = (int) Math.max(1, Math.rint(h / sf));
                FaceCells hidden = new FaceCells(part.name(), f.face(), f, nx, ny, "nearest");
                Arrays.fill(hidden.state, (byte) -1);
                faces.put(key, hidden);
            }
        }
        fillHidden(faces);
        return Optional.of(pack(faces, step, steps));
    }

    /** How many of the sorted cuts lie below x (np.searchsorted, side left). */
    private static int countBelow(double[] cuts, double x) {
        int n = 0;
        while (n < cuts.length && cuts[n] < x) {
            n++;
        }
        return n;
    }

    /**
     * Round 11: for the arm's face in a chestplate side view, the pixel box of the body side's face in that view, whose
     * rectangle holds the input's background beside the bare lower arm; null for every other face. By name, not by depth: the
     * left view reads the arm as R_ARM.left, which the layout places behind the body.
     */
    private int[] besideArmBox(WornLayout.View view, WornLayout.Item i) {
        if (!slot.equals("chestplate") || !i.box.equals("R_ARM") || !(view.name.equals("right") || view.name.equals("left"))) {
            return null;
        }
        for (WornLayout.Item it : view.items) {
            if (it.armor && it.box.equals("BODY")) {
                return WornLayout.pixelBox(it.px);
            }
        }
        return null;
    }

    private static int[] rgbOf(int rgb) {
        return new int[]{rgb >> 16 & 0xFF, rgb >> 8 & 0xFF, rgb & 0xFF};
    }

    private double[] lab(int p) {
        return WornColor.oklab(argb[p]);
    }

    private double dummyDist(int p) {
        double[] lab = lab(p);
        double a = norm(lab[0] - dummyLabs[0][0], lab[1] - dummyLabs[0][1], lab[2] - dummyLabs[0][2]);
        double b = norm(lab[0] - dummyLabs[1][0], lab[1] - dummyLabs[1][1], lab[2] - dummyLabs[1][2]);
        return Math.min(a, b);
    }

    static double norm(double a, double b, double c) {
        return Math.sqrt(a * a + b * b + c * c);
    }

    /**
     * Round 10: the mannequin's color where the input showed no mannequin. A patch on the gray joined to the mannequin is the
     * mannequin drawn (clear) unless it is far from the mannequin's own color and matches a dark color the armor uses apart from
     * the mannequin; a patch off the gray not joined to the mannequin is paint.
     */
    private void mannequinOnGray() {
        int[] drawn = L.draw();
        BitSet inGray = new BitSet(N), inDummy = new BitSet(N);
        for (int p = 0; p < N; p++) {
            if ((drawn[p] >>> 24) > 0) {
                int rgb = drawn[p] & 0xFFFFFF;
                if (rgb == WornLayout.LIGHT) {
                    inGray.set(p);
                } else if (rgb == WornLayout.DUMMY) {
                    inDummy.set(p);
                }
            }
        }
        drawn = null;
        int count = WornGrid.label(likeDummy, labels);
        boolean[] touches = new boolean[count + 1];
        for (int p = likeDummy.nextSetBit(0); p >= 0; p = likeDummy.nextSetBit(p + 1)) {
            if (inDummy.get(p)) {
                touches[labels[p]] = true;
            }
        }
        BitSet joins = new BitSet(N);
        for (int p = likeDummy.nextSetBit(0); p >= 0; p = likeDummy.nextSetBit(p + 1)) {
            if (touches[labels[p]]) {
                joins.set(p);
            }
        }
        BitSet onGray = (BitSet) likeDummy.clone();
        onGray.and(inGray);
        int patches = WornGrid.label(onGray, labels);
        boolean[] joined = new boolean[patches + 1];
        double[] sumD = new double[patches + 1];
        int[] sizes = new int[patches + 1];
        for (int p = onGray.nextSetBit(0); p >= 0; p = onGray.nextSetBit(p + 1)) {
            int id = labels[p];
            joined[id] |= joins.get(p);
            sumD[id] += dummyDist(p);
            sizes[id]++;
        }
        joins = null;
        boolean[] inPalette = new boolean[patches + 1];
        boolean anyPalette = false;
        for (int id = 1; id <= patches; id++) {
            inPalette[id] = !joined[id] && sizes[id] >= 30;
            anyPalette |= inPalette[id];
        }
        boolean[] needMedian = new boolean[patches + 1];
        for (int id = 1; id <= patches; id++) {
            needMedian[id] = inPalette[id] || (anyPalette && joined[id] && sumD[id] / sizes[id] >= MANNEQUIN_OWN);
        }
        double[][] medians = patchMedians(onGray, patches, sizes, needMedian);
        List<double[]> palette = new ArrayList<>();
        for (int id = 1; id <= patches; id++) {
            if (inPalette[id]) {
                palette.add(medians[id]);
            }
        }
        boolean[] shown = new boolean[patches + 1];
        for (int id = 1; id <= patches; id++) {
            if (!joined[id]) {
                continue;
            }
            if (sumD[id] / sizes[id] >= MANNEQUIN_OWN && !palette.isEmpty()) {
                double[] c = medians[id];
                double best = Double.POSITIVE_INFINITY;
                for (double[] q : palette) {
                    best = Math.min(best, norm(c[0] - q[0], c[1] - q[1], c[2] - q[2]));
                }
                if (best < PALETTE_MATCH) {
                    continue;
                }
            }
            shown[id] = true;
        }
        BitSet asPaint = new BitSet(N);
        for (int p = onGray.nextSetBit(0); p >= 0; p = onGray.nextSetBit(p + 1)) {
            if (!shown[labels[p]]) {
                asPaint.set(p);
            }
        }
        BitSet offGray = (BitSet) likeDummy.clone();
        offGray.andNot(inGray);
        int offCount = WornGrid.label(offGray, labels);
        boolean[] offJoined = new boolean[offCount + 1];
        for (int p = offGray.nextSetBit(0); p >= 0; p = offGray.nextSetBit(p + 1)) {
            if (inDummy.get(p)) {
                offJoined[labels[p]] = true;
            }
        }
        for (int p = offGray.nextSetBit(0); p >= 0; p = offGray.nextSetBit(p + 1)) {
            if (!offJoined[labels[p]]) {
                asPaint.set(p);
            }
        }
        paintedArmor.or(asPaint);
        mannequinPx = (BitSet) likeDummy.clone();
        mannequinPx.andNot(asPaint);
    }

    /** Per patch, the median OKLab color of its pixels (np.median per channel), where asked. */
    private double[][] patchMedians(BitSet pixels, int patches, int[] sizes, boolean[] need) {
        double[][][] values = new double[patches + 1][][];
        int[] fill = new int[patches + 1];
        for (int id = 1; id <= patches; id++) {
            if (need[id]) {
                values[id] = new double[3][sizes[id]];
            }
        }
        for (int p = pixels.nextSetBit(0); p >= 0; p = pixels.nextSetBit(p + 1)) {
            int id = labels[p];
            if (values[id] != null) {
                double[] lab = lab(p);
                for (int c = 0; c < 3; c++) {
                    values[id][c][fill[id]] = lab[c];
                }
                fill[id]++;
            }
        }
        double[][] out = new double[patches + 1][];
        for (int id = 1; id <= patches; id++) {
            if (values[id] != null) {
                out[id] = new double[]{median(values[id][0]), median(values[id][1]), median(values[id][2])};
            }
        }
        return out;
    }

    /** np.median: the middle value, or the mean of the two middle values. */
    static double median(double[] values) {
        double[] sorted = values.clone();
        Arrays.sort(sorted);
        int n = sorted.length;
        return n % 2 == 1 ? sorted[n / 2] : (sorted[n / 2 - 1] + sorted[n / 2]) / 2;
    }

    /** grid_processor.representative_color over pixels: the most common 32-level neighborhood, then its median, rounded half to even. */
    private int[] representativeColor(BitSet pixels) {
        int[] counts = new int[512];
        for (int p = pixels.nextSetBit(0); p >= 0; p = pixels.nextSetBit(p + 1)) {
            counts[code(argb[p])]++;
        }
        int mode = 0;
        for (int i = 1; i < 512; i++) {
            if (counts[i] > counts[mode]) {
                mode = i;
            }
        }
        int[][] histogram = new int[3][256];
        for (int p = pixels.nextSetBit(0); p >= 0; p = pixels.nextSetBit(p + 1)) {
            if (code(argb[p]) == mode) {
                histogram[0][argb[p] >> 16 & 0xFF]++;
                histogram[1][argb[p] >> 8 & 0xFF]++;
                histogram[2][argb[p] & 0xFF]++;
            }
        }
        return medianColor(histogram, counts[mode]);
    }

    private static int[] representativeColor(int[] colors, int n) {
        int[] counts = new int[512];
        for (int i = 0; i < n; i++) {
            counts[code(colors[i])]++;
        }
        int mode = 0;
        for (int i = 1; i < 512; i++) {
            if (counts[i] > counts[mode]) {
                mode = i;
            }
        }
        int[][] histogram = new int[3][256];
        for (int i = 0; i < n; i++) {
            if (code(colors[i]) == mode) {
                histogram[0][colors[i] >> 16 & 0xFF]++;
                histogram[1][colors[i] >> 8 & 0xFF]++;
                histogram[2][colors[i] & 0xFF]++;
            }
        }
        return medianColor(histogram, counts[mode]);
    }

    private static int code(int rgb) {
        return ((rgb >> 16 & 0xFF) / 32) * 64 + ((rgb >> 8 & 0xFF) / 32) * 8 + ((rgb & 0xFF) / 32);
    }

    private static int[] medianColor(int[][] histogram, int n) {
        int[] out = new int[3];
        for (int c = 0; c < 3; c++) {
            double m = n % 2 == 1 ? nth(histogram[c], n / 2) : (nth(histogram[c], n / 2 - 1) + nth(histogram[c], n / 2)) / 2.0;
            out[c] = (int) Math.rint(m);
        }
        return out;
    }

    private static int nth(int[] histogram, int k) {
        int seen = 0;
        for (int v = 0; v < 256; v++) {
            seen += histogram[v];
            if (seen > k) {
                return v;
            }
        }
        throw new IllegalStateException();
    }

    // ---------------------------------------------------------------- Drawing

    /**
     * The model's drawing placed on the input's faces (worn.Drawing): every painted pixel that is not the mannequin's color
     * belongs to the face whose gray it lies on, or to the face whose rectangle holds it over the mannequin, or, over the
     * background within REACH and connected to the piece, to the nearest face.
     */
    private void drawing() {
        index = new byte[N];
        byte[] rectOf = new byte[N];
        Arrays.fill(index, (byte) -1);
        Arrays.fill(rectOf, (byte) -1);
        BitSet dummy = new BitSet(N);
        for (WornLayout.View view : L.byView.values()) {
            for (int k = 0; k < view.items.size(); k++) {
                WornLayout.Item i = view.items.get(k);
                int[] pb = WornLayout.pixelBox(i.px);
                boolean rectangle = i.armor && !L.anyCovered(i.face);  // drawn as nothing (the chestplate's body top): its rectangle
                int n = entries.size();
                for (int y = pb[1]; y < pb[3]; y++) {
                    for (int x = pb[0]; x < pb[2]; x++) {
                        int p = y * W + x;
                        boolean shown = rectangle ? view.owner(x, y) == k : view.ownerCov(x, y) == k;
                        if (i.armor) {
                            if (shown) {
                                index[p] = (byte) n;
                            }
                            if (view.owner(x, y) == k) {
                                rectOf[p] = (byte) n;
                            }
                        } else if (shown) {
                            dummy.set(p);
                        }
                    }
                }
                if (i.armor) {
                    number.put(i, n);
                    entries.add(i);
                    entryViews.add(view);
                }
            }
        }
        for (int p = 0; p < N; p++) {
            if (index[p] < 0 && dummy.get(p) && rectOf[p] >= 0) {
                index[p] = rectOf[p];
            }
        }
        rectOf = null;
        int[] ft = WornGrid.featureTransform(index);
        nearest = new byte[N];
        within = new BitSet(N);
        double reach = REACH * L.S;
        for (int p = 0; p < N; p++) {
            int f = ft[p];
            nearest[p] = index[f];
            double dy = f / W - p / W, dx = f % W - p % W;
            if (Math.sqrt(dy * dy + dx * dx) <= reach) {
                within.set(p);
            }
        }
        ft = null;
        int count = WornGrid.label(paintedArmor, labels);
        boolean[] touching = new boolean[count + 1];
        for (int p = paintedArmor.nextSetBit(0); p >= 0; p = paintedArmor.nextSetBit(p + 1)) {
            if (index[p] >= 0) {
                touching[labels[p]] = true;
            }
        }
        owner = new byte[N];
        Arrays.fill(owner, (byte) -1);
        for (int p = paintedArmor.nextSetBit(0); p >= 0; p = paintedArmor.nextSetBit(p + 1)) {
            if (index[p] >= 0) {
                owner[p] = index[p];
            } else if (!dummy.get(p) && within.get(p) && touching[labels[p]]) {
                owner[p] = nearest[p];  // loose: over the background, near a face, connected to the piece
            }
        }
    }

    /** ([x0, x1, y0, y1] in image px, each edge's move in cells, outward positive, null where it stays). */
    private Object[] frame(int n, WornLayout.View view, int k, WornLayout.Item i, Double step) {
        double x0 = i.px[0], y0 = i.px[1], x1 = i.px[2], y1 = i.px[3];
        int r = (int) Math.ceil(REACH * L.S) + 2;
        int bx0 = Math.max(0, (int) x0 - r), by0 = Math.max(0, (int) y0 - r);
        int bx1 = Math.min(W, (int) x1 + r), by1 = Math.min(H, (int) y1 + r);
        double[] rect = {x0, x1, y0, y1};
        int minX = Integer.MAX_VALUE, maxX = -1, minY = Integer.MAX_VALUE, maxY = -1;
        for (int y = by0; y < by1; y++) {
            for (int x = bx0; x < bx1; x++) {
                if (owner[y * W + x] == n) {
                    minX = Math.min(minX, x);
                    maxX = Math.max(maxX, x);
                    minY = Math.min(minY, y);
                    maxY = Math.max(maxY, y);
                }
            }
        }
        if (maxX < 0) {
            return new Object[]{rect, new Double[4]};
        }
        double[] drawn = {minX, maxX + 1, minY, maxY + 1};
        int[] pb = WornLayout.pixelBox(i.px);
        int px0 = pb[0], py0 = pb[1], px1 = pb[2], py1 = pb[3];
        int[][] edges = {{1, -1}, {1, 1}, {0, -1}, {0, 1}};
        for (int e = 0; e < 4; e++) {
            int axis = edges[e][0], outward = edges[e][1];
            double edge = rect[e];
            if (drawn[e] * outward > edge * outward) {  // drawn past the edge
                rect[e] = drawn[e];
                continue;
            }
            if (step == null) {
                continue;
            }
            // Drawn short of it: in, when the input's gray reaches this edge with background beyond, for a cell or more of it.
            int ok = 0;
            if (axis == 1) {
                int inside = outward < 0 ? px0 : px1 - 1, beyond = inside + outward;
                for (int y = py0; y < py1; y++) {
                    if (view.ownerCov(inside, y) == k && (beyond < 0 || beyond >= W || view.owner(beyond, y) < 0)) {
                        ok++;
                    }
                }
            } else {
                int inside = outward < 0 ? py0 : py1 - 1, beyond = inside + outward;
                for (int x = px0; x < px1; x++) {
                    if (view.ownerCov(x, inside) == k && (beyond < 0 || beyond >= H || view.owner(x, beyond) < 0)) {
                        ok++;
                    }
                }
            }
            if (ok >= step) {
                rect[e] = edge - outward * Math.min(Math.abs(drawn[e] - edge), EDGE_IN * step);
            }
        }
        double[] original = {x0, x1, y0, y1};
        int[] out = {-1, 1, -1, 1};
        Double[] moved = new Double[4];
        for (int e = 0; e < 4; e++) {
            if (Math.abs(rect[e] - original[e]) >= 0.5) {
                double value = (rect[e] - original[e]) * out[e] / (step != null ? step : L.S);
                moved[e] = new BigDecimal(value).setScale(2, RoundingMode.HALF_EVEN).doubleValue();  // Python round(x, 2)
            }
        }
        return new Object[]{rect, moved};
    }

    // ---------------------------------------------------------------- one face

    /** A face's cells: ARGB per cell, state 1 read, 0 clear, -1 hidden; cut positions for read faces. */
    static final class FaceCells {
        final String box, face;
        final WornLayout.Face geometry;
        final int nx, ny;
        final int[] cells;
        final byte[] state;
        final String source;
        double[] xs, ys;
        String view;  // the view a read face was read in

        FaceCells(String box, String face, WornLayout.Face geometry, int nx, int ny, String source) {
            this.box = box;
            this.face = face;
            this.geometry = geometry;
            this.nx = nx;
            this.ny = ny;
            this.cells = new int[nx * ny];
            this.state = new byte[nx * ny];
            this.source = source;
        }

        FaceCells mirrored(String box, WornLayout.Face geometry) {
            FaceCells out = new FaceCells(box, geometry.face(), geometry, nx, ny, "outer side, mirrored");
            for (int j = 0; j < ny; j++) {
                for (int q = 0; q < nx; q++) {
                    out.cells[j * nx + q] = cells[j * nx + nx - 1 - q];
                    out.state[j * nx + q] = state[j * nx + nx - 1 - q];
                }
            }
            return out;
        }
    }

    private FaceCells readFace(int n, WornLayout.View view, int k, WornLayout.Item i, double stepV, double[] phase) {
        double ox = phase[0], oy = phase[1];
        double x0 = i.px[0], y0 = i.px[1], x1 = i.px[2], y1 = i.px[3];
        if (!(i.b[0] > 0 && i.c[1] > 0)) {
            throw new IllegalStateException("read faces run along +u and +v");
        }
        Object[] framed = frame(n, view, k, i, stepV);
        double[] rect = (double[]) framed[0];
        Double[] moved = (Double[]) framed[1];
        double X0 = rect[0], X1 = rect[1], Y0 = rect[2], Y1 = rect[3];
        double sx = (X1 - X0) / (x1 - x0), sy = (Y1 - Y0) / (y1 - y0);
        double[] xs = cuts(X0, X1, ox, stepV), ys = cuts(Y0, Y1, oy, stepV);
        int nx = xs.length - 1, ny = ys.length - 1;
        FaceCells out = new FaceCells(i.box, i.face.face(), i.face, nx, ny, "read");
        out.xs = xs;
        out.ys = ys;
        out.view = view.name;
        byte[] why = new byte[nx * ny];  // 1 left transparent, 2 painted in the mannequin's color
        boolean[] onGray = cellsOnGray(view, k, i, xs, ys);
        int[] bodyBox = besideArmBox(view, i);
        String bodyKey = null;
        if (bodyBox != null) {
            for (WornLayout.Item it : view.items) {
                if (it.armor && it.box.equals("BODY")) {
                    bodyKey = "BODY." + it.face.face();
                    break;
                }
            }
        }
        for (int j = 0; j < ny; j++) {
            for (int q = 0; q < nx; q++) {
                int cell = j * nx + q;
                int cx0 = (int) Math.max(0, Math.ceil(xs[q] - 0.5)), cx1 = (int) Math.min(W, Math.ceil(xs[q + 1] - 0.5));
                int cy0 = (int) Math.max(0, Math.ceil(ys[j] - 0.5)), cy1 = (int) Math.min(H, Math.ceil(ys[j + 1] - 0.5));
                int w = Math.max(0, cx1 - cx0), h = Math.max(0, cy1 - cy0);
                // As the input showed it, looked up where these pixels lie on the input's face.
                int[] xi = new int[w], yi = new int[h];
                for (int a = 0; a < w; a++) {
                    xi[a] = (int) Math.min(Math.max(Math.rint(x0 + ((cx0 + a) + 0.5 - X0) / sx - 0.5), 0), W - 1);
                }
                for (int b = 0; b < h; b++) {
                    yi[b] = (int) Math.min(Math.max(Math.rint(y0 + ((cy0 + b) + 0.5 - Y0) / sy - 0.5), 0), H - 1);
                }
                boolean[] blocked = new boolean[w * h];
                boolean[] beside = bodyBox != null ? new boolean[w * h] : null;
                int ahead = 0, behind = 0, behindOrBeside = 0;
                for (int b = 0; b < h; b++) {
                    for (int a = 0; a < w; a++) {
                        int oc = view.ownerCov(xi[a], yi[b]);
                        double dc = view.depthCov(xi[a], yi[b]);
                        boolean isAhead = oc != k && dc > i.depth + 1e-6;
                        boolean isBehind = oc != k && view.isArmor(oc) && dc < i.depth - 1e-6;
                        // Round 11: the input's background on the body side's rectangle, beside the bare lower arm
                        boolean isBeside = bodyBox != null && oc == -1 && xi[a] >= bodyBox[0] && xi[a] < bodyBox[2]
                                && yi[b] >= bodyBox[1] && yi[b] < bodyBox[3];
                        ahead += isAhead ? 1 : 0;
                        behind += isBehind ? 1 : 0;
                        behindOrBeside += isBehind || isBeside ? 1 : 0;
                        if (isBeside) {
                            beside[b * w + a] = !isBehind;
                        }
                        blocked[b * w + a] = isAhead || isBehind || isBeside;
                    }
                }
                int total = w * h;
                if (total > 0 && 2L * ahead > total) {
                    out.state[cell] = -1;
                    continue;
                }
                int ix = (int) (w * INSET), iy = (int) (h * INSET);
                if (beside != null && total > 0 && 2L * behind <= total && 2L * behindOrBeside > total) {
                    // These pixels clear the cell: their color goes to the body side once every face is read.
                    int[] colors = new int[Math.max(0, (w - 2 * ix) * (h - 2 * iy))];
                    int found = 0;
                    for (int b = iy; b < h - iy; b++) {
                        for (int a = ix; a < w - ix; a++) {
                            int p = (cy0 + b) * W + cx0 + a;
                            if (beside[b * w + a] && alpha(p)) {
                                colors[found++] = argb[p] & 0xFFFFFF;
                            }
                        }
                    }
                    if (found > 0) {
                        toBody.add(new ToBody(view.name, (xs[q] + xs[q + 1]) / 2, (ys[j] + ys[j + 1]) / 2,
                                representativeColor(colors, found), bodyKey));
                    }
                }
                if (total > 0 && 2L * behindOrBeside > total) {
                    continue;
                }
                int selected = 0, opaque = 0;
                int[] colors = new int[Math.max(0, (w - 2 * ix) * (h - 2 * iy))];
                int[] pixelAt = new int[colors.length];
                for (int b = iy; b < h - iy; b++) {
                    for (int a = ix; a < w - ix; a++) {
                        if (blocked[b * w + a]) {
                            continue;
                        }
                        selected++;
                        int p = (cy0 + b) * W + cx0 + a;
                        if (alpha(p)) {
                            pixelAt[opaque] = p;
                            colors[opaque++] = argb[p] & 0xFFFFFF;
                        }
                    }
                }
                if (selected == 0 || opaque < 0.5 * selected) {
                    why[cell] = 1;
                    continue;
                }
                int[] color = representativeColor(colors, opaque);
                double[] lab = WornColor.oklab(color);
                double toDummy = Math.min(norm(lab[0] - dummyLabs[0][0], lab[1] - dummyLabs[0][1], lab[2] - dummyLabs[0][2]),
                        norm(lab[0] - dummyLabs[1][0], lab[1] - dummyLabs[1][1], lab[2] - dummyLabs[1][2]));
                if (toDummy < CLEAR_DUMMY) {
                    // Clear where vanilla leaves the face uncovered, or where the model drew the mannequin itself onto the gray;
                    // read where it is a dark armor color on the gray (round 10). Judged on the cell's mannequin-colored pixels.
                    int like = 0, mannequin = 0;
                    for (int c = 0; c < opaque; c++) {
                        if (likeDummy.get(pixelAt[c])) {
                            like++;
                            mannequin += mannequinPx.get(pixelAt[c]) ? 1 : 0;
                        }
                    }
                    boolean shown = like > 0 && 2L * mannequin > like;
                    if (!onGray[cell] || shown) {
                        why[cell] = 2;
                        continue;
                    }
                }
                out.cells[cell] = 0xFF000000 | color[0] << 16 | color[1] << 8 | color[2];
                out.state[cell] = 1;
            }
        }
        int hiddenCells = 0;
        boolean anyRead = false;
        for (byte s : out.state) {
            hiddenCells += s < 0 ? 1 : 0;
            anyRead |= s > 0;
        }
        if (2L * hiddenCells > (long) nx * ny && !anyRead) {
            return null;  // behind something nearer with nothing read: made by the hidden-face rules
        }
        fillWidened(out, why, onGray, x0, x1, y0, y1, moved);
        return out;
    }

    /**
     * The model's own cells over a face from a to b: one cell for every lattice cell whose centre lies on the face, cut on
     * the view's lattice lines; the outermost cuts are the face's own edges.
     */
    static double[] cuts(double a, double b, double origin, double step) {
        int ka = (int) Math.ceil((a - origin) / step - 0.5);
        int kb = (int) Math.floor((b - origin) / step - 0.5);
        if (kb < ka) {
            return new double[]{a, b};
        }
        double[] out = new double[kb - ka + 2];
        out[0] = a;
        for (int k = ka + 1; k <= kb; k++) {
            out[k - ka] = origin + k * step;
        }
        out[out.length - 1] = b;
        return out;
    }

    /** Per cell: whether its centre, mapped back onto the input's face, lies on this face's gray. */
    private boolean[] cellsOnGray(WornLayout.View view, int k, WornLayout.Item i, double[] xs, double[] ys) {
        double x0 = i.px[0], y0 = i.px[1], x1 = i.px[2], y1 = i.px[3];
        double sx = (xs[xs.length - 1] - xs[0]) / (x1 - x0), sy = (ys[ys.length - 1] - ys[0]) / (y1 - y0);
        int nx = xs.length - 1, ny = ys.length - 1;
        int[] xi = new int[nx], yi = new int[ny];
        for (int q = 0; q < nx; q++) {
            xi[q] = (int) Math.min(Math.max(Math.floor(x0 + ((xs[q] + xs[q + 1]) / 2 - xs[0]) / sx), 0), W - 1);
        }
        for (int j = 0; j < ny; j++) {
            yi[j] = (int) Math.min(Math.max(Math.floor(y0 + ((ys[j] + ys[j + 1]) / 2 - ys[0]) / sy), 0), H - 1);
        }
        boolean[] out = new boolean[nx * ny];
        for (int j = 0; j < ny; j++) {
            for (int q = 0; q < nx; q++) {
                out[j * nx + q] = view.ownerCov(xi[q], yi[j]) == k;
            }
        }
        return out;
    }

    /**
     * Where a face's frame grew past the input's edge for something drawn on only some of its rows, the outer cells left
     * transparent on the gray take the color of the nearest read cell inward along the same row or column.
     */
    private static void fillWidened(FaceCells f, byte[] why, boolean[] onGray, double x0, double x1, double y0, double y1, Double[] moved) {
        int nx = f.nx, ny = f.ny;
        double[] cx = new double[nx], cy = new double[ny];
        for (int q = 0; q < nx; q++) {
            cx[q] = (f.xs[q] + f.xs[q + 1]) / 2;
        }
        for (int j = 0; j < ny; j++) {
            cy[j] = (f.ys[j] + f.ys[j + 1]) / 2;
        }
        for (int e = 0; e < 4; e++) {
            Double m = moved[e];
            if (m == null || m <= 0) {
                continue;
            }
            if (e < 2) {  // left, right: along the row
                int step = e == 0 ? 1 : -1;
                for (int q = 0; q < nx; q++) {
                    if (!(e == 0 ? cx[q] < x0 : cx[q] > x1)) {
                        continue;
                    }
                    for (int j = 0; j < ny; j++) {
                        int at = j * nx + q;
                        if (f.state[at] != 0 || why[at] != 1 || !onGray[at]) {
                            continue;
                        }
                        for (int t = q + step; step > 0 ? t < nx : t > -1; t += step) {
                            if (f.state[j * nx + t] == 1) {
                                f.cells[at] = f.cells[j * nx + t];
                                f.state[at] = 1;
                                why[at] = 0;
                                break;
                            }
                        }
                    }
                }
            } else {  // top, bottom: along the column
                int step = e == 2 ? 1 : -1;
                for (int j = 0; j < ny; j++) {
                    if (!(e == 2 ? cy[j] < y0 : cy[j] > y1)) {
                        continue;
                    }
                    for (int q = 0; q < nx; q++) {
                        int at = j * nx + q;
                        if (f.state[at] != 0 || why[at] != 1 || !onGray[at]) {
                            continue;
                        }
                        for (int t = j + step; step > 0 ? t < ny : t > -1; t += step) {
                            if (f.state[t * nx + q] == 1) {
                                f.cells[at] = f.cells[t * nx + q];
                                f.state[at] = 1;
                                why[at] = 0;
                                break;
                            }
                        }
                    }
                }
            }
        }
    }

    /** Hidden cells take the nearest read (or clear) cell of the same box, by the distance between cell centres in world units. */
    private static void fillHidden(Map<String, FaceCells> faces) {
        Map<String, List<FaceCells>> byBox = new LinkedHashMap<>();
        Map<FaceCells, double[][]> centres = new LinkedHashMap<>();
        for (FaceCells f : faces.values()) {
            double[] s = new double[f.nx], t = new double[f.ny];
            for (int q = 0; q < f.nx; q++) {
                s[q] = (q + 0.5) / f.nx;
            }
            for (int j = 0; j < f.ny; j++) {
                t[j] = (j + 0.5) / f.ny;
            }
            if (f.xs != null) {  // read faces: cell centres where the model's cells lie
                double x0 = f.xs[0], x1 = f.xs[f.xs.length - 1], y0 = f.ys[0], y1 = f.ys[f.ys.length - 1];
                for (int q = 0; q < f.nx; q++) {
                    s[q] = ((f.xs[q] + f.xs[q + 1]) / 2 - x0) / (x1 - x0);
                }
                for (int j = 0; j < f.ny; j++) {
                    t[j] = ((f.ys[j] + f.ys[j + 1]) / 2 - y0) / (y1 - y0);
                }
            }
            double[] p00 = f.geometry.p00(), p10 = f.geometry.p10(), p01 = f.geometry.p01();
            double[][] c = new double[f.nx * f.ny][3];
            for (int j = 0; j < f.ny; j++) {
                for (int q = 0; q < f.nx; q++) {
                    for (int d = 0; d < 3; d++) {
                        c[j * f.nx + q][d] = p00[d] + s[q] * (p10[d] - p00[d]) + t[j] * (p01[d] - p00[d]);
                    }
                }
            }
            centres.put(f, c);
            byBox.computeIfAbsent(f.box, b -> new ArrayList<>()).add(f);
        }
        for (List<FaceCells> entries : byBox.values()) {
            List<Integer> colors = new ArrayList<>();
            List<double[]> points = new ArrayList<>();
            for (FaceCells f : entries) {
                double[][] c = centres.get(f);
                for (int at = 0; at < f.state.length; at++) {
                    if (f.state[at] >= 0) {
                        colors.add(f.cells[at]);
                        points.add(c[at]);
                    }
                }
            }
            if (points.isEmpty()) {
                continue;
            }
            for (FaceCells f : entries) {
                double[][] c = centres.get(f);
                for (int at = 0; at < f.state.length; at++) {
                    if (f.state[at] >= 0) {
                        continue;
                    }
                    int best = 0;
                    double bestD = Double.POSITIVE_INFINITY;
                    for (int m = 0; m < points.size(); m++) {
                        double[] p = points.get(m);
                        double dx = c[at][0] - p[0], dy = c[at][1] - p[1], dz = c[at][2] - p[2];
                        double d = dx * dx + dy * dy + dz * dz;
                        if (d < bestD) {
                            bestD = d;
                            best = m;
                        }
                    }
                    f.cells[at] = colors.get(best);
                }
            }
        }
    }

    /** One row per box, faces left to right with a 1px gap. */
    private Result pack(Map<String, FaceCells> faces, double step, Map<String, Double> steps) {
        Map<String, List<FaceCells>> rows = new LinkedHashMap<>();
        for (FaceCells f : faces.values()) {
            rows.computeIfAbsent(f.box, b -> new ArrayList<>()).add(f);
        }
        int width = 0, height = 0;
        for (List<FaceCells> r : rows.values()) {
            int w = 0, h = 0;
            for (FaceCells f : r) {
                w += f.nx + 1;
                h = Math.max(h, f.ny);
            }
            width = Math.max(width, w);
            height += h + 1;
        }
        BufferedImage atlas = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        StringBuilder json = new StringBuilder();
        json.append("{\"slot\":\"").append(slot).append("\",\"step\":").append(step)
                .append(",\"size\":[").append(width).append(',').append(height).append("],\"faces\":{");
        int y = 0;
        boolean first = true;
        for (List<FaceCells> r : rows.values()) {
            List<FaceCells> sorted = new ArrayList<>(r);
            sorted.sort((a, b) -> Integer.compare(WornLayout.FACES.indexOf(a.face), WornLayout.FACES.indexOf(b.face)));
            int x = 0, h = 0;
            for (FaceCells f : sorted) {
                for (int j = 0; j < f.ny; j++) {
                    for (int q = 0; q < f.nx; q++) {
                        atlas.setRGB(x + q, y + j, f.cells[j * f.nx + q]);
                    }
                }
                json.append(first ? "" : ",").append('"').append(f.box).append('.').append(f.face).append("\":[")
                        .append(x).append(',').append(y).append(',').append(f.nx).append(',').append(f.ny)
                        .append(",\"").append(f.source).append("\"]");
                first = false;
                x += f.nx + 1;
                h = Math.max(h, f.ny);
            }
            y += h + 1;
        }
        json.append("},\"view_steps\":{");
        first = true;
        for (Map.Entry<String, Double> e : steps.entrySet()) {
            json.append(first ? "" : ",").append('"').append(e.getKey()).append("\":").append(e.getValue().doubleValue());
            first = false;
        }
        json.append("}}");
        return new Result(atlas, json.toString());
    }
}
