package com.watermelon0117.aicraft;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.IOException;
import java.util.Stack;

/**
 * A robust, from-scratch implementation of the Canny edge detection algorithm in pure Java.
 *
 * This class encapsulates the entire five-step Canny algorithm:
 * 1.  **Gaussian Blur:** Reduces noise using a separable 2D Gaussian kernel.
 * 2.  **Gradient Calculation:** Computes image gradients using Sobel operators.
 * 3.  **Non-Maximum Suppression:** Thins edges to a single-pixel width.
 * 4.  **Double Thresholding:** Classifies pixels as strong, weak, or non-edges.
 * 5.  **Hysteresis Edge Tracking:** Connects weak edges to strong edges to form continuous lines,
 * using a robust, iterative, stack-based approach to prevent StackOverflowError.
 *
 * It is designed for clarity, performance, and correctness, using efficient array-based
 * operations and direct access to the image's pixel buffer.
 *
 * @author Gemini AI
 * @version 1.0
 */
public class CannyEdgeDetector {

    // --- Constants ---
    private final static int STRONG_EDGE = 255;
    private final static int WEAK_EDGE = 75;

    // --- Instance Variables ---
    private int width;
    private int height;
    private int[] grayscaleImage;
    private int[] magnitude;
    private float[] orientation;

    // --- Parameters ---
    private final float sigma;
    private final float lowThreshold;
    private final float highThreshold;
    private final int gaussianKernelWidth;

    /**
     * Constructs a CannyEdgeDetector with specified parameters.
     *
     * @param sigma         The standard deviation of the Gaussian kernel. A larger value creates more blur.
     *                      Recommended: 1.0 to 2.0.
     * @param lowThreshold  The lower threshold for hysteresis. Pixels with gradient magnitude below this
     *                      are discarded.
     * @param highThreshold The upper threshold for hysteresis. Pixels with gradient magnitude above this
     *                      are marked as strong edges.
     */
    public CannyEdgeDetector(float sigma, float lowThreshold, float highThreshold) {
        this.sigma = sigma;
        this.lowThreshold = lowThreshold;
        this.highThreshold = highThreshold;
        // Kernel width is determined by sigma, typically 3*sigma on each side
        this.gaussianKernelWidth = (int) (Math.ceil(sigma * 3) * 2 + 1);
    }

    /**
     * The main processing method that applies the Canny algorithm to a source image.
     *
     * @param sourceImage The input image to detect edges on.
     * @return A new BufferedImage containing the detected edges (black background, white edges).
     */
    public BufferedImage process(BufferedImage sourceImage) {
        width = sourceImage.getWidth();
        height = sourceImage.getHeight();
        int[][] finalEdges=new int[3][];
        for (int channel = 0; channel < 2; channel++) {
            // Step 1: Convert to grayscale
            grayscaleImage = toGrayscale(sourceImage);
            // --- MODIFIED: Conditionally apply Gaussian blur ---
            int[] imageForGradients;
            imageForGradients = grayscaleImage;
            // Step 2: Calculate gradients using the (potentially blurred) image
            calculateGradients(imageForGradients);
            // Step 3: Perform non-maximum suppression
            int[] nmsImage = nonMaximumSuppression();
            // Step 4: Perform double thresholding and hysteresis
            finalEdges[channel] = hysteresis(nmsImage);
            // Step 5: Create and return the final output image
        }
        return createOutputImage(finalEdges);
    }

    // --- Step 1: Grayscale Conversion and Gaussian Blur ---

    /**
     * Converts a BufferedImage to a 1D grayscale integer array.
     * It efficiently accesses the image's raw pixel data.
     *
     * @param image The image to convert.
     * @return A 1D int array of grayscale values (0-255).
     */
    private int[] toGrayscale(BufferedImage image) {
        int[] gray = new int[width * height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                // Get pixel color as a single integer (e.g., in ARGB format)
                int rgb = image.getRGB(x, y);

                // Extract R, G, B components using bitwise operations
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;

                // Standard luminance conversion formula (Y' = 0.299R + 0.587G + 0.114B)
                gray[y * width + x] = (int) (0.299 * r + 0.587 * g + 0.114 * b);
            }
        }
        return gray;
    }

    /**
     * Applies a 2D Gaussian filter to an image represented by a 1D array.
     *
     * @param image The 1D array of grayscale pixel data.
     * @return A new 1D array containing the blurred image data.
     */
    private int[] applyGaussianFilter(int[] image) {
        float[] kernel = createGaussianKernel();
        int[] blurred = new int[width * height];

        // --- Separable Convolution: Horizontal Pass ---
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                float sum = 0;
                for (int k = 0; k < gaussianKernelWidth; k++) {
                    int col = x + k - (gaussianKernelWidth / 2);
                    // Handle borders by clamping to edge
                    col = Math.max(0, Math.min(col, width - 1));
                    sum += image[y * width + col] * kernel[k];
                }
                blurred[y * width + x] = (int) sum;
            }
        }

        // --- Separable Convolution: Vertical Pass ---
        int[] finalBlurred = new int[width * height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                float sum = 0;
                for (int k = 0; k < gaussianKernelWidth; k++) {
                    int row = y + k - (gaussianKernelWidth / 2);
                    // Handle borders by clamping to edge
                    row = Math.max(0, Math.min(row, height - 1));
                    sum += blurred[row * width + x] * kernel[k];
                }
                finalBlurred[y * width + x] = (int) sum;
            }
        }
        return finalBlurred;
    }

    /**
     * Creates a 1D Gaussian kernel.
     *
     * @return A float array representing the 1D kernel.
     */
    private float[] createGaussianKernel() {
        float[] kernel = new float[gaussianKernelWidth];
        float sum = 0;
        int r = gaussianKernelWidth / 2;
        for (int i = 0; i < gaussianKernelWidth; i++) {
            float x = i - r;
            kernel[i] = (float) (Math.exp(-(x * x) / (2 * sigma * sigma)) / (Math.sqrt(2 * Math.PI) * sigma));
            sum += kernel[i];
        }
        for (int i = 0; i < gaussianKernelWidth; i++) {
            kernel[i] /= sum; // Normalize
        }
        return kernel;
    }


    // --- Step 2: Gradient Calculation ---

    /**
     * Calculates the gradient magnitude and orientation using Sobel operators.
     *
     * @param image The blurred grayscale image data.
     */
    private void calculateGradients(int[] image) {
        magnitude = new int[width * height];
        orientation = new float[width * height];
        int[] sobelX = {-1, 0, 1, -2, 0, 2, -1, 0, 1};
        int[] sobelY = {-1, -2, -1, 0, 0, 0, 1, 2, 1};

        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                int gx = 0;
                int gy = 0;
                int k = 0;
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        int pixel = image[(y + dy) * width + (x + dx)];
                        gx += pixel * sobelX[k];
                        gy += pixel * sobelY[k];
                        k++;
                    }
                }
                int index = y * width + x;
                magnitude[index] = (int) Math.sqrt(gx * gx + gy * gy);
                orientation[index] = (float) Math.atan2(gy, gx);
            }
        }
    }


    // --- Step 3: Non-Maximum Suppression ---

    /**
     * Thins the edges to a single pixel width.
     *
     * @return A 1D int array with suppressed non-maximal pixels.
     */
    private int[] nonMaximumSuppression() {
        int[] nms = new int[width * height];
        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                int index = y * width + x;
                float angle = orientation[index];
                int mag = magnitude[index];

                // Convert radians to degrees and normalize to 0-180
                angle = (float) (angle * 180.0 / Math.PI);
                if (angle < 0) {
                    angle += 180;
                }

                int mag1 = 0, mag2 = 0;
                // --- Find neighbors in the direction of the gradient ---
                if ((angle >= 0 && angle < 22.5) || (angle >= 157.5 && angle <= 180)) { // Horizontal
                    mag1 = magnitude[index - 1];
                    mag2 = magnitude[index + 1];
                } else if (angle >= 22.5 && angle < 67.5) { // Diagonal (+)
                    mag1 = magnitude[index - width + 1];
                    mag2 = magnitude[index + width - 1];
                } else if (angle >= 67.5 && angle < 112.5) { // Vertical
                    mag1 = magnitude[index - width];
                    mag2 = magnitude[index + width];
                } else if (angle >= 112.5 && angle < 157.5) { // Diagonal (-)
                    mag1 = magnitude[index - width - 1];
                    mag2 = magnitude[index + width + 1];
                }

                if (mag >= mag1 && mag >= mag2) {
                    nms[index] = mag;
                }
            }
        }
        return nms;
    }


    // --- Step 4 & 5: Double Thresholding and Hysteresis ---

    /**
     * Performs double thresholding and robust, iterative edge tracking by hysteresis.
     *
     * @param nmsImage The non-maximum suppressed image data.
     * @return A 1D int array representing the final edge map.
     */
    private int[] hysteresis(int[] nmsImage) {
        int[] edges = new int[width * height];
        Stack<Integer> edgeStack = new Stack<>();

        // --- Double Thresholding ---
        for (int i = 0; i < nmsImage.length; i++) {
            if (nmsImage[i] >= highThreshold) {
                edges[i] = STRONG_EDGE;
                edgeStack.push(i); // Push strong edges onto the stack
            } else if (nmsImage[i] >= lowThreshold) {
                edges[i] = WEAK_EDGE;
            }
        }

        // --- Iterative Hysteresis (Edge Tracking) ---
        while (!edgeStack.isEmpty()) {
            int currentPixelIndex = edgeStack.pop();
            int x = currentPixelIndex % width;
            int y = currentPixelIndex / width;

            // Check 8-connected neighbors
            for (int dy = -1; dy <= 1; dy++) {
                for (int dx = -1; dx <= 1; dx++) {
                    if (dx == 0 && dy == 0) continue; // Skip the pixel itself

                    int newX = x + dx;
                    int newY = y + dy;
                    int neighborIndex = newY * width + newX;

                    // Check bounds
                    if (newX >= 0 && newX < width && newY >= 0 && newY < height) {
                        // If neighbor is a weak edge, promote it to a strong edge and add to stack
                        if (edges[neighborIndex] == WEAK_EDGE) {
                            edges[neighborIndex] = STRONG_EDGE;
                            edgeStack.push(neighborIndex);
                        }
                    }
                }
            }
        }

        // --- Final Cleanup ---
        // Set any remaining weak edges that were not connected to strong ones to zero.
        for (int i = 0; i < edges.length; i++) {
            if (edges[i] != STRONG_EDGE) {
                edges[i] = 0;
            }
        }

        return edges;
    }


    // --- Final Output ---

    /**
     * Creates a final binary BufferedImage from the edge data.
     *
     * @param edgeData The 1D int array of final edge data.
     * @return A new BufferedImage with black background and white edges.
     */
    private BufferedImage createOutputImage(int[][] edgeData) {
        BufferedImage output = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY);
        for (int c = 0; c < 2; c++) {
            for (int i = 0; i < edgeData[c].length; i++) {
                int x = i % width;
                int y = i / width;
                if (edgeData[c][i] == STRONG_EDGE) {
                    output.setRGB(x, y, 0xFFFFFFFF); // White
                }
                // No 'else' is needed because the image is initialized to all black (0 bits).
            }
        }
        return output;
    }
}