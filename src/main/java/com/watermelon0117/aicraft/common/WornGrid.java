package com.watermelon0117.aicraft.common;

import java.util.Arrays;
import java.util.BitSet;

/**
 * The lattice fit, labelling and nearest-face transform {@link WornReader} needs, as
 * {@code grid_processor.fit_grid}, {@code scipy.ndimage.label} (8-connected) and
 * {@code scipy.ndimage.distance_transform_edt(return_indices=True)} compute them.
 */
final class WornGrid {
    static final int W = WornLayout.W, H = WornLayout.H;
    private WornGrid() {
    }

    // ---------------------------------------------------------------- lattice

    /**
     * worn.fit_view: the view's lattice, and its coarser refit when a face would get more than MAX_DENSITY times vanilla's
     * cells, kept only when it scores at least KEEP_FINE of the fine fit. Returns {step, x origin, y origin} or null.
     */
    static double[] fitView(int[] argb, BitSet mask, double floorStep, double ceilingStep) {
        double[] grid = fitGrid(argb, mask, WornReader.MIN_STEP, ceilingStep);
        if (grid == null) {
            return null;
        }
        if (grid[0] < floorStep) {
            double[] fine = grid;
            grid = fitGrid(argb, mask, Math.min(Math.ceil(floorStep * 4) / 4, ceilingStep), ceilingStep);
            if (grid == null) {
                return null;
            }
            if (grid[3] < WornReader.KEEP_FINE * fine[3]) {  // no coarser lattice in the drawing: the fine one is its own pixels
                grid = fine;
            }
        }
        return new double[]{grid[0], grid[1], grid[2]};
    }

    /** grid_processor.fit_grid: {step, x origin, y origin, score}, or null with too few edges. */
    static double[] fitGrid(int[] argb, BitSet mask, double minStep, double maxStep) {
        int[] box = bounds(mask);
        double[][] x = edgePeaks(argb, mask, box, true);
        double[][] y = edgePeaks(argb, mask, box, false);
        if (x[0].length < 2 || y[0].length < 2) {
            return null;
        }
        double upper = Math.min(maxStep, Math.min(H, W) / 2.0);
        // np.arange(min_step, upper + increment / 2, increment): start, start + increment, then start + i * (that difference)
        double stop = upper + WornReader.STEP_INCREMENT / 2;
        long count = (long) Math.ceil((stop - minStep) / WornReader.STEP_INCREMENT);
        if (count <= 0) {
            throw new IllegalStateException("no candidate periods within configured range");
        }
        double delta = (minStep + WornReader.STEP_INCREMENT) - minStep;
        double best = Double.NEGATIVE_INFINITY;
        double[] steps = new double[(int) count], scores = new double[(int) count], xo = new double[(int) count], yo = new double[(int) count];
        for (int k = 0; k < count; k++) {
            double step = k == 0 ? minStep : k == 1 ? minStep + WornReader.STEP_INCREMENT : minStep + k * delta;
            double[] fx = phaseFit(x[0], x[1], step), fy = phaseFit(y[0], y[1], step);
            steps[k] = step;
            scores[k] = 0.3 * (fx[1] + fy[1]) + 0.4 * Math.min(fx[1], fy[1]);
            xo[k] = fx[0];
            yo[k] = fy[0];
            best = Math.max(best, scores[k]);
        }
        // Prefer the coarsest near-equivalent fundamental period over its subpixel harmonics.
        int chosen = -1;
        for (int k = 0; k < count; k++) {
            if (scores[k] >= best - WornReader.HARMONIC_SCORE_SLACK && (chosen < 0 || steps[k] > steps[chosen])) {
                chosen = k;
            }
        }
        return new double[]{steps[chosen], xo[chosen], yo[chosen], scores[chosen]};
    }

    private static int[] bounds(BitSet mask) {
        int minX = W, minY = H, maxX = -1, maxY = -1;
        for (int p = mask.nextSetBit(0); p >= 0; p = mask.nextSetBit(p + 1)) {
            int x = p % W, y = p / W;
            minX = Math.min(minX, x);
            maxX = Math.max(maxX, x);
            minY = Math.min(minY, y);
            maxY = Math.max(maxY, y);
        }
        return new int[]{minX, minY, maxX, maxY};
    }

    /** {origin, resultant} of the edge positions' phase at this step. */
    private static double[] phaseFit(double[] positions, double[] weights, double step) {
        double re = 0, im = 0, total = 0;
        for (int i = 0; i < positions.length; i++) {
            double theta = 2 * Math.PI * positions[i] / step;
            re += weights[i] * Math.cos(theta);
            im += weights[i] * Math.sin(theta);
            total += weights[i];
        }
        re /= total;
        im /= total;
        double origin = pyMod(Math.atan2(im, re) * step / (2 * Math.PI), step);
        return new double[]{origin, Math.hypot(re, im)};
    }

    private static double pyMod(double a, double b) {
        double r = a % b;
        if (r != 0 && (r < 0) != (b < 0)) {
            r += b;
        }
        return r;
    }

    /** grid_processor.edge_peaks on the pairs of pixels with either one in the mask: {positions, weights}. */
    private static double[][] edgePeaks(int[] argb, BitSet mask, int[] box, boolean alongX) {
        int n = alongX ? W - 1 : H - 1;
        double[] energy = new double[n];
        if (box[2] >= 0) {
            int y0 = alongX ? box[1] : Math.max(0, box[1] - 1), y1 = alongX ? box[3] : Math.min(H - 2, box[3]);
            int x0 = alongX ? Math.max(0, box[0] - 1) : box[0], x1 = alongX ? Math.min(W - 2, box[2]) : box[2];
            for (int y = y0; y <= y1; y++) {
                for (int x = x0; x <= x1; x++) {
                    int a = y * W + x, b = alongX ? a + 1 : a + W;
                    if (!(mask.get(a) || mask.get(b))) {
                        continue;
                    }
                    int pa = argb[a], pb = argb[b];
                    int d = Math.max(Math.abs((pb >> 16 & 0xFF) - (pa >> 16 & 0xFF)),
                            Math.max(Math.abs((pb >> 8 & 0xFF) - (pa >> 8 & 0xFF)), Math.abs((pb & 0xFF) - (pa & 0xFF))));
                    if (d > WornReader.CONTRAST_FLOOR) {
                        energy[alongX ? x : y] += d - WornReader.CONTRAST_FLOOR;
                    }
                }
            }
        }
        double max = 0;
        for (double e : energy) {
            max = Math.max(max, e);
        }
        if (max == 0) {
            return new double[][]{new double[0], new double[0]};
        }
        int[] peaks = SpritePixelizer.findPeaks(energy, Math.max(10.0, max * 0.025), 3);
        double[] positions = new double[peaks.length], weights = new double[peaks.length];
        for (int p = 0; p < peaks.length; p++) {
            int lo = Math.max(0, peaks[p] - 1), hi = Math.min(n, peaks[p] + 2);
            double dot = 0, sum = 0;
            for (int i = lo; i < hi; i++) {
                dot += (i + 1) * energy[i];
                sum += energy[i];
            }
            positions[p] = dot / sum;
            weights[p] = sum;
        }
        if (weights.length > 0) {
            // Strong outer contours should not overwhelm all internal color boundaries.
            double cap = percentile(weights, 90);
            for (int i = 0; i < weights.length; i++) {
                weights[i] = Math.sqrt(Math.min(weights[i], cap));
            }
        }
        return new double[][]{positions, weights};
    }

    /** np.percentile, method 'linear', with numpy's interpolation formula. */
    static double percentile(double[] values, double q) {
        double[] sorted = values.clone();
        Arrays.sort(sorted);
        int n = sorted.length;
        double index = (n - 1) * (q / 100);
        if (index >= n - 1) {
            return sorted[n - 1];
        }
        int lo = (int) Math.floor(index);
        double a = sorted[lo], b = sorted[lo + 1], t = index - lo;
        double diff = b - a;
        return t >= 0.5 ? b - diff * (1 - t) : a + diff * t;
    }

    // ---------------------------------------------------------------- labels

    /** 8-connected components of the mask: labels 1..count in {@code labels}, 0 elsewhere. Returns count. */
    static int label(BitSet mask, int[] labels) {
        Arrays.fill(labels, 0);
        int[] parent = new int[1024];
        int next = 1;
        for (int p = mask.nextSetBit(0); p >= 0; p = mask.nextSetBit(p + 1)) {
            int x = p % W, y = p / W;
            int found = 0;
            int[] around = {x > 0 ? p - 1 : -1, y > 0 && x > 0 ? p - W - 1 : -1, y > 0 ? p - W : -1, y > 0 && x < W - 1 ? p - W + 1 : -1};
            for (int q : around) {
                if (q >= 0 && labels[q] != 0) {
                    int root = find(parent, labels[q]);
                    if (found == 0) {
                        found = root;
                    } else if (root != found) {
                        int lo = Math.min(root, found), hi = Math.max(root, found);
                        parent[hi] = lo;
                        found = lo;
                    }
                }
            }
            if (found == 0) {
                if (next >= parent.length) {
                    parent = Arrays.copyOf(parent, parent.length * 2);
                }
                parent[next] = next;
                found = next++;
            }
            labels[p] = found;
        }
        int[] compact = new int[next];
        int count = 0;
        for (int l = 1; l < next; l++) {
            int root = find(parent, l);
            if (root == l) {
                compact[l] = ++count;
            }
        }
        for (int p = mask.nextSetBit(0); p >= 0; p = mask.nextSetBit(p + 1)) {
            labels[p] = compact[find(parent, labels[p])];
        }
        return count;
    }

    private static int find(int[] parent, int l) {
        while (parent[l] != l) {
            parent[l] = parent[parent[l]];
            l = parent[l];
        }
        return l;
    }

    // ---------------------------------------------------------------- nearest face

    /**
     * For every pixel, the nearest pixel with {@code index >= 0} (y * W + x), as scipy's feature transform finds it: a pass
     * down each column, then along each row, ties to the lower index ({@code _ComputeFT} and {@code _VoronoiFT}).
     */
    static int[] featureTransform(byte[] index) {
        int[] ft = new int[W * H];
        int[] fy = new int[Math.max(W, H)], fx = new int[Math.max(W, H)], g = new int[Math.max(W, H)];
        int[] outY = new int[Math.max(W, H)], outX = new int[Math.max(W, H)];
        // Axis 0: each column on its own.
        for (int x = 0; x < W; x++) {
            for (int y = 0; y < H; y++) {
                boolean feature = index[y * W + x] >= 0;
                fy[y] = feature ? y : -1;
                fx[y] = x;
            }
            voronoi(H, 0, new int[]{0, x}, fy, fx, g, outY, outX);
            for (int y = 0; y < H; y++) {
                ft[y * W + x] = outY[y] < 0 ? -1 : outY[y] * W + outX[y];
            }
        }
        // Axis 1: each row, over the columns' nearest.
        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                int f = ft[y * W + x];
                fy[x] = f < 0 ? -1 : f / W;
                fx[x] = f < 0 ? 0 : f % W;
            }
            voronoi(W, 1, new int[]{y, 0}, fy, fx, g, outY, outX);
            for (int x = 0; x < W; x++) {
                ft[y * W + x] = outY[x] < 0 ? -1 : outY[x] * W + outX[x];
            }
        }
        return ft;
    }

    /** scipy's _VoronoiFT on one line along axis d, rank 2; f = (fy, fx), fy < 0 where the line has no site yet. */
    private static void voronoi(int len, int d, int[] coor, int[] fy, int[] fx, int[] g, int[] outY, int[] outX) {
        int l = -1;
        for (int ii = 0; ii < len; ii++) {
            if (fy[ii] >= 0) {
                long fd = d == 0 ? fy[ii] : fx[ii];
                long tw = d == 0 ? fx[ii] - coor[1] : fy[ii] - coor[0];
                long wR = tw * tw;
                while (l >= 1) {
                    int idx1 = g[l], idx2 = g[l - 1];
                    long f1 = d == 0 ? fy[idx1] : fx[idx1];
                    long a = f1 - (d == 0 ? fy[idx2] : fx[idx2]);
                    long b = fd - f1;
                    long c = a + b;
                    long tu = d == 0 ? fx[idx2] - coor[1] : fy[idx2] - coor[0];
                    long tv = d == 0 ? fx[idx1] - coor[1] : fy[idx1] - coor[0];
                    long uR = tu * tu, vR = tv * tv;
                    if (c * vR - b * uR - a * wR - a * b * c <= 0) {
                        break;
                    }
                    --l;
                }
                ++l;
                g[l] = ii;
            }
        }
        int maxl = l;
        if (maxl < 0) {
            for (int ii = 0; ii < len; ii++) {
                outY[ii] = fy[ii];
                outX[ii] = fx[ii];
            }
            return;
        }
        l = 0;
        for (int ii = 0; ii < len; ii++) {
            long delta1 = distance(d, coor, fy[g[l]], fx[g[l]], ii);
            while (l < maxl) {
                long delta2 = distance(d, coor, fy[g[l + 1]], fx[g[l + 1]], ii);
                if (delta1 <= delta2) {
                    break;
                }
                delta1 = delta2;
                ++l;
            }
            outY[ii] = fy[g[l]];
            outX[ii] = fx[g[l]];
        }
    }

    private static long distance(int d, int[] coor, int sy, int sx, int ii) {
        long ty = d == 0 ? sy - ii : sy - coor[0];
        long tx = d == 1 ? sx - ii : sx - coor[1];
        return ty * ty + tx * tx;
    }
}
