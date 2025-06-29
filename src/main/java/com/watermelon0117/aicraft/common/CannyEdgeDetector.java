package com.watermelon0117.aicraft.common;

import java.awt.image.BufferedImage;
import java.util.Stack;



public class CannyEdgeDetector {

    // ---------- constants ----------
    private static final int STRONG_EDGE = 200;
    private static final int WEAK_EDGE = 100;

    // ---------- image-size-dependent ----------
    private int width;
    private int height;
    private int[] magnitude;      // final (max-per-pixel) magnitude
    private float[] orientation;  // orientation matching that max

    // ---------- configurable parameters ----------
    private final float lowThreshold;
    private final float highThreshold;

    public CannyEdgeDetector(float lowThreshold, float highThreshold) {
        this.lowThreshold = lowThreshold;
        this.highThreshold = highThreshold;
    }

    /* ───────────────────────────────────────────────────────────
       Whole pipeline – now fuses the 3 channels                */
    public BufferedImage process(BufferedImage src) {
        width = src.getWidth();
        height = src.getHeight();

        /* Arrays that will finally hold the *maximum-magnitude*
           gradient among the three channels.                    */
        magnitude = new int[width * height];
        orientation = new float[width * height];

        /* --- 1 & 2.  Per-channel gradients, then keep the max --- */
        for (int channel = 0; channel < 3; channel++) {             // ★ changed (3 channels)
            int[] gray = pickChannel(src, channel);
            Gradients g = calcGradients(gray);                      // ★ changed (returns local mags/oris)

            for (int i = 0; i < magnitude.length; i++) {            // keep the strongest one
                if (g.mag[i] > magnitude[i]) {
                    magnitude[i] = g.mag[i];
                    orientation[i] = g.ori[i];
                }
            }
        }

        /* --- 3.  NMS on the fused gradient field --- */
        int[] nms = nonMaximumSuppression();

        /* --- 4 & 5.  Double threshold + hysteresis --- */
        int[] edges = hysteresis(nms);

        /* --- build binary output image --- */
        return createOutputImage(edges);
    }

    /* ───────────────────────────── helper: extract a single RGB channel */
    private int[] pickChannel(BufferedImage img, int c) {
        int[] out = new int[width * height];
        for (int y = 0, idx = 0; y < height; y++) {
            for (int x = 0; x < width; x++, idx++) {
                int rgb = img.getRGB(x, y);
                int v = (c == 0) ? (rgb >> 16 & 0xFF)
                        : (c == 1) ? (rgb >> 8 & 0xFF)
                        : (rgb & 0xFF);
                out[idx] = v;
            }
        }
        return out;
    }

    /* ───────────────────────────── gradients for one gray image */
    private static class Gradients {               // ★ added tiny record-like holder
        final int[] mag;
        final float[] ori;

        Gradients(int[] m, float[] o) {
            this.mag = m;
            this.ori = o;
        }
    }

    private Gradients calcGradients(int[] img) {   // ★ changed – returns Gradients object
        int[] mag = new int[width * height];
        float[] ori = new float[width * height];

        final int[] kx = {-1, 0, 1, -2, 0, 2, -1, 0, 1};
        final int[] ky = {-1, -2, -1, 0, 0, 0, 1, 2, 1};

        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                int gx = 0, gy = 0, k = 0;
                for (int dy = -1; dy <= 1; dy++)
                    for (int dx = -1; dx <= 1; dx++, k++) {
                        int p = img[(y + dy) * width + (x + dx)];
                        gx += p * kx[k];
                        gy += p * ky[k];
                    }
                int idx = y * width + x;
                mag[idx] = (int) Math.hypot(gx, gy);
                ori[idx] = (float) Math.atan2(gy, gx);
            }
        }
        return new Gradients(mag, ori);
    }

    /* ── NMS uses the *global* magnitude & orientation arrays we fused above */
    private int[] nonMaximumSuppression() {
        int[] nms = new int[width * height];

        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                int idx = y * width + x;
                float angle = orientation[idx] * 180.0f / (float) Math.PI;
                if (angle < 0) angle += 180;

                int mag = magnitude[idx];
                int mag1 = 0, mag2 = 0;

                if ((angle >= 0 && angle < 22.5) || (angle >= 157.5)) {      // 0°
                    mag1 = magnitude[idx - 1];
                    mag2 = magnitude[idx + 1];
                } else if (angle >= 22.5 && angle < 67.5) {                     // 45°
                    mag1 = magnitude[idx - width + 1];
                    mag2 = magnitude[idx + width - 1];
                } else if (angle >= 67.5 && angle < 112.5) {                    // 90°
                    mag1 = magnitude[idx - width];
                    mag2 = magnitude[idx + width];
                } else {                                                        // 135°
                    mag1 = magnitude[idx - width - 1];
                    mag2 = magnitude[idx + width + 1];
                }

                if (mag >= mag1 && mag >= mag2)
                    nms[idx] = mag;
            }
        }
        return nms;
    }

    /* hysteresis, createOutputImage — unchanged except signature ↓ */
    private int[] hysteresis(int[] nms) {
        int[] out = new int[width * height];
        Stack<Integer> st = new Stack<>();

        for (int i = 0; i < nms.length; i++) {
            if (nms[i] >= highThreshold) {
                out[i] = STRONG_EDGE;
                st.push(i);
            } else if (nms[i] >= lowThreshold) out[i] = WEAK_EDGE;
        }

        while (!st.isEmpty()) {
            int idx = st.pop();
            int x = idx % width, y = idx / width;
            for (int dy = -1; dy <= 1; dy++)
                for (int dx = -1; dx <= 1; dx++) {
                    if (dx == 0 && dy == 0) continue;
                    int nx = x + dx, ny = y + dy;
                    if (0 <= nx && nx < width && 0 <= ny && ny < height) {
                        int nIdx = ny * width + nx;
                        if (out[nIdx] == WEAK_EDGE) {
                            out[nIdx] = STRONG_EDGE;
                            st.push(nIdx);
                        }
                    }
                }
        }
        for (int i = 0; i < out.length; i++)
            if (out[i] != STRONG_EDGE) out[i] = 0;
        return out;
    }

    private BufferedImage createOutputImage(int[] edges) {            // ★ changed signature
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY);
        for (int i = 0; i < edges.length; i++) {
            if (edges[i] == STRONG_EDGE) {
                int x = i % width;
                int y = i / width;
                img.setRGB(x, y, 0xFFFFFFFF);
            }
        }
        return img;
    }

    public static BufferedImage overlayCannyEdges(
            BufferedImage src,
            float lowThresh,
            float highThresh) {

        final int EDGE_COLOR = 0xFF000000;          // fully-opaque red

        // --- 1. run Canny ---
        CannyEdgeDetector canny = new CannyEdgeDetector(lowThresh, highThresh);
        BufferedImage edgeImg = canny.process(src);     // TYPE_BYTE_BINARY: white = edge

        int w = src.getWidth();
        int h = src.getHeight();

        // --- 2. copy original into an ARGB buffer we can draw on ---
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        out.getGraphics().drawImage(src, 0, 0, null);

        // --- 3. paint edges wherever edgeImg pixel is white (0xFFFFFFFF) ---
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (edgeImg.getRGB(x, y) == 0xFFFFFFFF) {
                    out.setRGB(x, y, EDGE_COLOR);
                }
            }
        }
        return out;
    }
}
