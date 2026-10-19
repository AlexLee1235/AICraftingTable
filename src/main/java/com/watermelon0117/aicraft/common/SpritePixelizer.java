package com.watermelon0117.aicraft.common;

import java.awt.image.BufferedImage;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Recovers the pixel grid an image model actually drew and samples one color per cell.
 *
 * <p>Java port of the offline "recovery v2" used to evaluate the Sunburst migration
 * ({@code test/pixel_grid_recovery_v2/processor.py} with
 * {@code test/pixel_grid_reconstruction/grid_processor.py}): a single square lattice fitted
 * to edge positions by phase, refined against the foreground mask, then a representative
 * color per occupied cell. The source is never resized.
 *
 * <p>If the model's own grid gives a sprite larger than {@link #MAX_SIDE}, the grid is refit
 * with a minimum step that keeps the item within {@code MAX_SIDE - 2} cells.
 */
public final class SpritePixelizer {
    public static final int MAX_SIDE = 32;

    // grid_processor.Settings
    private static final double MIN_STEP = 8.0;
    private static final double MAX_STEP = 128.0;
    private static final double STEP_INCREMENT = 0.25;
    private static final double CONTRAST_FLOOR = 12.0;
    private static final double PHASE_TOLERANCE_FRACTION = 0.10;
    private static final double HARMONIC_SCORE_SLACK = 0.012;
    private static final double BACKGROUND_MIN_TOLERANCE = 12.0;
    private static final double BACKGROUND_MAX_TOLERANCE = 40.0;
    // recovery v2 Settings
    private static final double GLOBAL_KEY_MIN_CHROMA = 48.0;
    private static final double STRICT_COLOR_DISTANCE = 18.0;
    private static final double BORDER_COLOR_DISTANCE = 36.0;
    private static final double FOREGROUND_COVERAGE = 0.55;
    private static final double CENTER_INSET_FRACTION = 0.18;
    private static final double PHASE_SEARCH_FRACTION = 0.15;
    private static final int PHASE_SEARCH_POINTS = 5;
    private static final int TRANSPARENT_MARGIN = 1;

    /**
     * @param sprite        square ARGB sprite, item centered with a transparent margin
     * @param contentWidth  width of the item in cells
     * @param contentHeight height of the item in cells
     * @param step          source pixels per cell
     * @param capped        whether the coarser refit was used
     * @param maskMethod    "source_alpha" or "chroma_key"
     */
    public record Result(BufferedImage sprite, int contentWidth, int contentHeight,
                         double step, boolean capped, String maskMethod) {
    }

    private SpritePixelizer() {
    }

    public static Optional<Result> pixelize(BufferedImage image) {
        Source src = Source.of(image);
        Optional<Result> result = pixelize(src, MIN_STEP, false);
        if (result.isPresent() && result.get().sprite().getWidth() > MAX_SIDE) {
            double extent = Math.max(src.maskWidth(), src.maskHeight());
            Optional<Result> capped = pixelize(src, Math.max(MIN_STEP, extent / (MAX_SIDE - 2)), true);
            if (capped.isPresent()) {
                return capped;
            }
        }
        return result;
    }

    private static Optional<Result> pixelize(Source src, double minStep, boolean capped) {
        if (src.maskCount == 0) {
            return Optional.empty();
        }
        Grid grid = fitGrid(src, minStep);
        if (grid == null) {
            return Optional.empty();
        }
        double step = grid.step;
        double[] origin = refinePhase(src, step, grid.xOrigin, grid.yOrigin);
        Geometry geo = cellGeometry(src, step, origin[0], origin[1]);
        int rows = geo.yy.length - 1, cols = geo.xx.length - 1;
        int[] cells = new int[rows * cols];
        int inset = (int) (step * CENTER_INSET_FRACTION);
        for (int j = 0; j < rows; j++) {
            for (int i = 0; i < cols; i++) {
                if (!geo.occupied[j * cols + i]) {
                    continue;
                }
                int left = geo.xx[i], right = geo.xx[i + 1], top = geo.yy[j], bottom = geo.yy[j + 1];
                List<int[]> selected = new ArrayList<>();
                if (inset > 0 && Math.min(bottom - top, right - left) > inset * 2) {
                    collect(src, left + inset, right - inset, top + inset, bottom - inset, selected);
                }
                if (selected.isEmpty()) {
                    collect(src, left, right, top, bottom, selected);
                }
                if (selected.isEmpty()) {
                    throw new IllegalStateException("an occupied cell must contain source foreground");
                }
                int[] c = representativeColor(selected);
                cells[j * cols + i] = 0xFF000000 | (c[0] << 16) | (c[1] << 8) | c[2];
            }
        }
        int minX = cols, maxX = -1, minY = rows, maxY = -1;
        for (int j = 0; j < rows; j++) {
            for (int i = 0; i < cols; i++) {
                if (cells[j * cols + i] != 0) {
                    minX = Math.min(minX, i);
                    maxX = Math.max(maxX, i);
                    minY = Math.min(minY, j);
                    maxY = Math.max(maxY, j);
                }
            }
        }
        if (maxX < 0) {
            return Optional.empty();
        }
        int width = maxX - minX + 1, height = maxY - minY + 1;
        int side = Math.max(16, (int) Math.ceil((Math.max(width, height) + 2.0 * TRANSPARENT_MARGIN) / 8) * 8);
        BufferedImage sprite = new BufferedImage(side, side, BufferedImage.TYPE_INT_ARGB);
        int py = (side - height) / 2, px = (side - width) / 2;
        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {
                sprite.setRGB(px + i, py + j, cells[(minY + j) * cols + (minX + i)]);
            }
        }
        return Optional.of(new Result(sprite, width, height, step, capped, src.maskMethod));
    }

    private static void collect(Source src, int x0, int x1, int y0, int y1, List<int[]> out) {
        for (int y = y0; y < y1; y++) {
            for (int x = x0; x < x1; x++) {
                int k = y * src.w + x;
                if (src.mask[k]) {
                    out.add(new int[]{src.r[k], src.g[k], src.b[k]});
                }
            }
        }
    }

    /** Most common 32-level RGB neighborhood, then the median of its actual colors. */
    static int[] representativeColor(List<int[]> colors) {
        int[] counts = new int[512];
        for (int[] c : colors) {
            counts[code(c)]++;
        }
        int mode = 0;
        for (int i = 1; i < 512; i++) {
            if (counts[i] > counts[mode]) {
                mode = i;
            }
        }
        int n = counts[mode];
        int[][] channels = new int[3][n];
        int k = 0;
        for (int[] c : colors) {
            if (code(c) == mode) {
                channels[0][k] = c[0];
                channels[1][k] = c[1];
                channels[2][k] = c[2];
                k++;
            }
        }
        int[] result = new int[3];
        for (int ch = 0; ch < 3; ch++) {
            result[ch] = (int) Math.rint(median(channels[ch]));
        }
        return result;
    }

    private static int code(int[] c) {
        return (c[0] / 32) * 64 + (c[1] / 32) * 8 + (c[2] / 32);
    }

    private static double median(int[] values) {
        int[] sorted = values.clone();
        Arrays.sort(sorted);
        int n = sorted.length;
        return n % 2 == 1 ? sorted[n / 2] : (sorted[n / 2 - 1] + sorted[n / 2]) / 2.0;
    }

    // ---------------------------------------------------------------- lattice fit

    private record Grid(double step, double xOrigin, double yOrigin) {
    }

    private static Grid fitGrid(Source src, double minStep) {
        double[][] x = edgePeaks(src, true);
        double[][] y = edgePeaks(src, false);
        if (x[0].length < 2 || y[0].length < 2) {
            return null;
        }
        double upper = Math.min(MAX_STEP, Math.min(src.h, src.w) / 2.0);
        int count = (int) Math.ceil((upper + STEP_INCREMENT / 2 - minStep) / STEP_INCREMENT);
        if (count <= 0) {
            return null;
        }
        double[] steps = new double[count], scores = new double[count], xo = new double[count], yo = new double[count];
        double best = Double.NEGATIVE_INFINITY;
        for (int k = 0; k < count; k++) {
            double step = minStep + k * STEP_INCREMENT;
            double[] fx = phaseFit(x[0], x[1], step), fy = phaseFit(y[0], y[1], step);
            steps[k] = step;
            scores[k] = 0.3 * (fx[1] + fy[1]) + 0.4 * Math.min(fx[1], fy[1]);
            xo[k] = fx[0];
            yo[k] = fy[0];
            best = Math.max(best, scores[k]);
        }
        int chosen = -1;
        for (int k = 0; k < count; k++) {
            if (scores[k] >= best - HARMONIC_SCORE_SLACK && (chosen < 0 || steps[k] > steps[chosen])) {
                chosen = k;
            }
        }
        return new Grid(steps[chosen], xo[chosen], yo[chosen]);
    }

    /** Returns {origin, resultant}. */
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

    /** Returns {positions, weights} of edge peaks along x (columns) or y (rows). */
    private static double[][] edgePeaks(Source src, boolean alongX) {
        int w = src.w, h = src.h;
        int n = alongX ? w - 1 : h - 1;
        double[] energy = new double[n];
        for (int y = 0; y < (alongX ? h : h - 1); y++) {
            for (int x = 0; x < (alongX ? w - 1 : w); x++) {
                int a = y * w + x, b = alongX ? a + 1 : a + w;
                if (!(src.mask[a] || src.mask[b])) {
                    continue;
                }
                int d = Math.max(Math.abs(src.r[b] - src.r[a]),
                        Math.max(Math.abs(src.g[b] - src.g[a]), Math.abs(src.b[b] - src.b[a])));
                if (d > CONTRAST_FLOOR) {
                    energy[alongX ? x : y] += d - CONTRAST_FLOOR;
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
        int[] peaks = findPeaks(energy, Math.max(10.0, max * 0.025), 3);
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
            double cap = percentile(weights, 90);
            for (int i = 0; i < weights.length; i++) {
                weights[i] = Math.sqrt(Math.min(weights[i], cap));
            }
        }
        return new double[][]{positions, weights};
    }

    /** scipy.signal.find_peaks(x, prominence=minProminence, distance=distance). */
    static int[] findPeaks(double[] x, double minProminence, int distance) {
        List<Integer> peaks = new ArrayList<>();
        int i = 1, last = x.length - 1;
        while (i < last) {
            if (x[i - 1] < x[i]) {
                int ahead = i + 1;
                while (ahead < last && x[ahead] == x[i]) {
                    ahead++;
                }
                if (x[ahead] < x[i]) {
                    peaks.add((i + ahead - 1) / 2);
                    i = ahead;
                }
            }
            i++;
        }
        // distance: keep higher peaks first; ties keep the later index, like numpy's argsort order
        int m = peaks.size();
        boolean[] keep = new boolean[m];
        Arrays.fill(keep, true);
        Integer[] order = new Integer[m];
        for (int k = 0; k < m; k++) {
            order[k] = k;
        }
        Arrays.sort(order, (a, b) -> Double.compare(x[peaks.get(a)], x[peaks.get(b)]));
        for (int k = m - 1; k >= 0; k--) {
            int j = order[k];
            if (!keep[j]) {
                continue;
            }
            for (int l = j - 1; l >= 0 && peaks.get(j) - peaks.get(l) < distance; l--) {
                keep[l] = false;
            }
            for (int l = j + 1; l < m && peaks.get(l) - peaks.get(j) < distance; l++) {
                keep[l] = false;
            }
        }
        List<Integer> result = new ArrayList<>();
        for (int k = 0; k < m; k++) {
            if (keep[k] && prominence(x, peaks.get(k)) >= minProminence) {
                result.add(peaks.get(k));
            }
        }
        return result.stream().mapToInt(Integer::intValue).toArray();
    }

    private static double prominence(double[] x, int peak) {
        double leftMin = x[peak];
        for (int i = peak; i >= 0 && x[i] <= x[peak]; i--) {
            leftMin = Math.min(leftMin, x[i]);
        }
        double rightMin = x[peak];
        for (int i = peak; i < x.length && x[i] <= x[peak]; i++) {
            rightMin = Math.min(rightMin, x[i]);
        }
        return x[peak] - Math.max(leftMin, rightMin);
    }

    /** numpy.percentile with linear interpolation. */
    static double percentile(double[] values, double q) {
        double[] sorted = values.clone();
        Arrays.sort(sorted);
        double index = (sorted.length - 1) * q / 100.0;
        int lo = (int) Math.floor(index), hi = (int) Math.ceil(index);
        return sorted[lo] + (sorted[hi] - sorted[lo]) * (index - lo);
    }

    /** Python float modulo: result has the sign of the divisor. */
    private static double pyMod(double a, double b) {
        double r = a % b;
        if (r != 0 && (r < 0) != (b < 0)) {
            r += b;
        }
        return r;
    }

    // ---------------------------------------------------------------- phase refinement

    private static double[] refinePhase(Source src, double step, double initialX, double initialY) {
        double[] deltas = new double[PHASE_SEARCH_POINTS];
        double start = -PHASE_SEARCH_FRACTION * step, stop = PHASE_SEARCH_FRACTION * step;
        double inc = (stop - start) / (PHASE_SEARCH_POINTS - 1);
        for (int i = 0; i < PHASE_SEARCH_POINTS; i++) {
            deltas[i] = i * inc + start;
        }
        deltas[PHASE_SEARCH_POINTS - 1] = stop;
        double bestIou = Double.NEGATIVE_INFINITY, bestTie = Double.NEGATIVE_INFINITY, bestX = 0, bestY = 0;
        for (double dx : deltas) {
            for (double dy : deltas) {
                double ox = pyMod(initialX + dx, step), oy = pyMod(initialY + dy, step);
                double iou = cellGeometry(src, step, ox, oy).iou;
                double tie = -Math.abs(dx) - Math.abs(dy);
                if (iou > bestIou || (iou == bestIou && tie > bestTie)) {
                    bestIou = iou;
                    bestTie = tie;
                    bestX = ox;
                    bestY = oy;
                }
            }
        }
        return new double[]{bestX, bestY};
    }

    private record Geometry(int[] xx, int[] yy, boolean[] occupied, double iou) {
    }

    private static Geometry cellGeometry(Source src, double step, double ox, double oy) {
        int[] xx = lines(src.minX, src.maxX, ox, step, src.w);
        int[] yy = lines(src.minY, src.maxY, oy, step, src.h);
        int rows = yy.length - 1, cols = xx.length - 1;
        boolean[] occupied = new boolean[Math.max(0, rows * cols)];
        long intersection = 0, reconstruction = 0;
        double threshold = step * step * FOREGROUND_COVERAGE;
        for (int j = 0; j < rows; j++) {
            for (int i = 0; i < cols; i++) {
                long count = src.integral(xx[i], yy[j], xx[i + 1], yy[j + 1]);
                if (count >= threshold) {
                    occupied[j * cols + i] = true;
                    intersection += count;
                    reconstruction += (long) (yy[j + 1] - yy[j]) * (xx[i + 1] - xx[i]);
                }
            }
        }
        long union = src.maskCount + reconstruction - intersection;
        return new Geometry(xx, yy, occupied, union == 0 ? 0.0 : (double) intersection / union);
    }

    private static int[] lines(int min, int max, double origin, double step, int limit) {
        int first = (int) Math.floor((min - origin) / step);
        int last = (int) Math.ceil((max + 1 - origin) / step);
        int[] out = new int[last - first + 1];
        for (int k = first; k <= last; k++) {
            double v = origin + k * step;
            out[k - first] = (int) Math.min(limit, Math.max(0, Math.ceil(v)));
        }
        return out;
    }

    // ---------------------------------------------------------------- source and mask

    private static final class Source {
        final int w, h;
        final int[] r, g, b;
        final boolean[] mask;
        final long[] sums;
        final String maskMethod;
        int maskCount, minX, maxX, minY, maxY;

        private Source(int w, int h, int[] r, int[] g, int[] b, boolean[] mask, String maskMethod) {
            this.w = w;
            this.h = h;
            this.r = r;
            this.g = g;
            this.b = b;
            this.mask = mask;
            this.maskMethod = maskMethod;
            this.sums = new long[(w + 1) * (h + 1)];
            minX = w;
            minY = h;
            maxX = -1;
            maxY = -1;
            for (int y = 0; y < h; y++) {
                long row = 0;
                for (int x = 0; x < w; x++) {
                    if (mask[y * w + x]) {
                        row++;
                        maskCount++;
                        minX = Math.min(minX, x);
                        maxX = Math.max(maxX, x);
                        minY = Math.min(minY, y);
                        maxY = Math.max(maxY, y);
                    }
                    sums[(y + 1) * (w + 1) + x + 1] = sums[y * (w + 1) + x + 1] + row;
                }
            }
        }

        int maskWidth() {
            return maxX - minX + 1;
        }

        int maskHeight() {
            return maxY - minY + 1;
        }

        long integral(int x0, int y0, int x1, int y1) {
            int s = w + 1;
            return sums[y1 * s + x1] - sums[y0 * s + x1] - sums[y1 * s + x0] + sums[y0 * s + x0];
        }

        static Source of(BufferedImage image) {
            int w = image.getWidth(), h = image.getHeight();
            int[] argb = image.getRGB(0, 0, w, h, null, 0, w);
            int[] r = new int[w * h], g = new int[w * h], b = new int[w * h];
            boolean transparent = false;
            for (int i = 0; i < argb.length; i++) {
                r[i] = argb[i] >> 16 & 0xFF;
                g[i] = argb[i] >> 8 & 0xFF;
                b[i] = argb[i] & 0xFF;
                transparent |= (argb[i] >>> 24) < 255;
            }
            boolean[] mask = new boolean[w * h];
            if (transparent) {
                for (int i = 0; i < argb.length; i++) {
                    mask[i] = (argb[i] >>> 24) >= 128;
                }
                return new Source(w, h, r, g, b, mask, "source_alpha");
            }
            return new Source(w, h, r, g, b, chromaKeyMask(w, h, r, g, b), "chroma_key");
        }
    }

    /** Opaque fallback: border-connected background plus a global key for saturated backgrounds. */
    private static boolean[] chromaKeyMask(int w, int h, int[] r, int[] g, int[] b) {
        int border = Math.max(1, (int) (Math.min(h, w) * 0.015625));
        List<Integer> perimeter = new ArrayList<>();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (y < border || y >= h - border || x < border || x >= w - border) {
                    perimeter.add(y * w + x);
                }
            }
        }
        int[] counts = new int[4096];
        for (int i : perimeter) {
            counts[(r[i] / 16) * 256 + (g[i] / 16) * 16 + b[i] / 16]++;
        }
        int mode = 0;
        for (int i = 1; i < counts.length; i++) {
            if (counts[i] > counts[mode]) {
                mode = i;
            }
        }
        int[][] dominant = new int[3][counts[mode]];
        int k = 0;
        for (int i : perimeter) {
            if ((r[i] / 16) * 256 + (g[i] / 16) * 16 + b[i] / 16 == mode) {
                dominant[0][k] = r[i];
                dominant[1][k] = g[i];
                dominant[2][k] = b[i];
                k++;
            }
        }
        double[] bg = {median(dominant[0]), median(dominant[1]), median(dominant[2])};
        List<Double> nearby = new ArrayList<>();
        for (int i : perimeter) {
            double d = distance(r[i], g[i], b[i], bg);
            if (d <= 32) {
                nearby.add(d);
            }
        }
        double noise = nearby.isEmpty() ? 0.0 : percentile(nearby.stream().mapToDouble(Double::doubleValue).toArray(), 98);
        double tolerance = Math.min(BACKGROUND_MAX_TOLERANCE, Math.max(BACKGROUND_MIN_TOLERANCE, noise + 4));
        double strict = Math.max(STRICT_COLOR_DISTANCE, tolerance);
        double loose = Math.max(BORDER_COLOR_DISTANCE, strict);
        boolean keyHoles = Math.max(bg[0], Math.max(bg[1], bg[2])) - Math.min(bg[0], Math.min(bg[1], bg[2])) >= GLOBAL_KEY_MIN_CHROMA;
        double[] dist = new double[w * h];
        for (int i = 0; i < dist.length; i++) {
            dist[i] = distance(r[i], g[i], b[i], bg);
        }
        boolean[] background = new boolean[w * h];
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int i = y * w + x;
                if ((y == 0 || y == h - 1 || x == 0 || x == w - 1) && dist[i] <= loose && !background[i]) {
                    background[i] = true;
                    queue.add(i);
                }
            }
        }
        floodFill(queue, background, i -> dist[i] <= loose, w, h);
        boolean[] mask = new boolean[w * h];
        for (int i = 0; i < mask.length; i++) {
            mask[i] = !(background[i] || (keyHoles && dist[i] <= strict));
        }
        removeSmallComponents(mask, w, h, 4);
        return mask;
    }

    private static double distance(int r, int g, int b, double[] bg) {
        double dr = r - bg[0], dg = g - bg[1], db = b - bg[2];
        return Math.sqrt(dr * dr + dg * dg + db * db);
    }

    private interface IntPredicate {
        boolean test(int i);
    }

    private static void floodFill(ArrayDeque<Integer> queue, boolean[] seen, IntPredicate allowed, int w, int h) {
        while (!queue.isEmpty()) {
            int i = queue.poll(), x = i % w, y = i / w;
            int[] next = {x > 0 ? i - 1 : -1, x < w - 1 ? i + 1 : -1, y > 0 ? i - w : -1, y < h - 1 ? i + w : -1};
            for (int n : next) {
                if (n >= 0 && !seen[n] && allowed.test(n)) {
                    seen[n] = true;
                    queue.add(n);
                }
            }
        }
    }

    private static void removeSmallComponents(boolean[] mask, int w, int h, int minSize) {
        boolean[] seen = new boolean[mask.length];
        for (int start = 0; start < mask.length; start++) {
            if (!mask[start] || seen[start]) {
                continue;
            }
            List<Integer> component = new ArrayList<>();
            ArrayDeque<Integer> queue = new ArrayDeque<>();
            seen[start] = true;
            queue.add(start);
            while (!queue.isEmpty()) {
                int i = queue.poll(), x = i % w, y = i / w;
                component.add(i);
                int[] next = {x > 0 ? i - 1 : -1, x < w - 1 ? i + 1 : -1, y > 0 ? i - w : -1, y < h - 1 ? i + w : -1};
                for (int n : next) {
                    if (n >= 0 && mask[n] && !seen[n]) {
                        seen[n] = true;
                        queue.add(n);
                    }
                }
            }
            if (component.size() < minSize) {
                for (int i : component) {
                    mask[i] = false;
                }
            }
        }
    }
}
