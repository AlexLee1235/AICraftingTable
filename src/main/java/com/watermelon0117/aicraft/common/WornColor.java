package com.watermelon0117.aicraft.common;

/** OKLab as {@code test/armor_texture2/read.oklab} computes it, for {@link WornReader}. */
final class WornColor {
    private static final double[][] M1 = {
            {0.4122214708, 0.5363325363, 0.0514459929},
            {0.2119034982, 0.6806995451, 0.1073969566},
            {0.0883024619, 0.2817188376, 0.6299787005}};
    private static final double[][] M2 = {
            {0.2104542553, 0.7936177850, -0.0040720468},
            {1.9779984951, -2.4285922050, 0.4505937099},
            {0.0259040371, 0.7827717662, -0.8086757660}};
    private static final double[] LINEAR = new double[256];

    static {
        for (int v = 0; v < 256; v++) {
            double c = v / 255.0;
            LINEAR[v] = c <= 0.04045 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
        }
    }

    private WornColor() {
    }

    static double[] oklab(int rgb) {
        return oklab(LINEAR[rgb >> 16 & 0xFF], LINEAR[rgb >> 8 & 0xFF], LINEAR[rgb & 0xFF]);
    }

    static double[] oklab(int[] rgb) {
        return oklab(LINEAR[rgb[0]], LINEAR[rgb[1]], LINEAR[rgb[2]]);
    }

    /** numpy's N x 3 matrix products give exactly these fused sums. */
    private static double[] oklab(double r, double g, double b) {
        double l = Math.cbrt(Math.fma(b, M1[0][2], Math.fma(g, M1[0][1], r * M1[0][0])));
        double m = Math.cbrt(Math.fma(b, M1[1][2], Math.fma(g, M1[1][1], r * M1[1][0])));
        double s = Math.cbrt(Math.fma(b, M1[2][2], Math.fma(g, M1[2][1], r * M1[2][0])));
        return new double[]{
                Math.fma(s, M2[0][2], Math.fma(m, M2[0][1], l * M2[0][0])),
                Math.fma(s, M2[1][2], Math.fma(m, M2[1][1], l * M2[1][0])),
                Math.fma(s, M2[2][2], Math.fma(m, M2[2][1], l * M2[2][0]))};
    }
}
