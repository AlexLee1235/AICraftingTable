package com.watermelon0117.aicraft;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ImageGridProcessor {
    private static final int WHITE_RGB = Color.WHITE.getRGB();
    private static final int BLACK_RGB = Color.BLACK.getRGB();
    private static final CannyEdgeDetector cannyEdgeDetector = new CannyEdgeDetector(100,200);

    public static BufferedImage process(BufferedImage image) {
        try {
            // 1. Read Image
            System.out.println("Image shape: " + image.getWidth() + "x" + image.getHeight());

            saveImage(CannyEdgeDetector.overlayCannyEdges(image,100,200),
                    FileUtil.getTempFolder("edges_image.png"));

            // 2. Edge Detection
            BufferedImage edges = edgeDetection(image);
            saveImage(edges, FileUtil.getTempFolder("edges.png"));

            // Remove Background
            image=removeBackgroundAuto(image, edges);

            // 3. Extract Lines using Morphological Operations
            BufferedImage verticalLines = extractVerticalLines(edges);
            saveImage(verticalLines, FileUtil.getTempFolder("vertical_lines.png"));
            BufferedImage horizontalLines = extractHorizontalLines(edges);
            saveImage(horizontalLines, FileUtil.getTempFolder("horizontal_lines.png"));

            // 4. Create Histograms
            int[] xHistogram = histogramNonZero(verticalLines, 'x');
            int[] yHistogram = histogramNonZero(horizontalLines, 'y');

            // 5. Find Pikes (peaks) in histograms
            int[] xPikes = findPikes(xHistogram, 10);
            System.out.println("X Pikes: " + Arrays.toString(xPikes));
            int[] yPikes = findPikes(yHistogram, 10);
            System.out.println("Y Pikes: " + Arrays.toString(yPikes));

            // 6. Calculate Grid Size
            int[] xDiff = removeSmallLines(calculateDifferences(xPikes));
            int[] yDiff = removeSmallLines(calculateDifferences(yPikes));

            double xGridSize = clusterAverage(xDiff);
            double yGridSize = clusterAverage(yDiff);
            System.out.println("Grid size: " + xGridSize + ", " + yGridSize);
            int[] ratio = findNearestRatio(xGridSize, yGridSize);
            System.out.println("Nearest ratio:"+ratio[0]+", "+ratio[1]);
            xGridSize/=ratio[0];
            yGridSize/=ratio[1];
            System.out.println("Adjusted Grid size:" + xGridSize + ", " + yGridSize);

            // 7. Refine and Insert Lines
            List<Integer> oldXLines = removeDuplicates(xPikes, 5);
            List<Integer> oldYLines = removeDuplicates(yPikes, 5);

            List<Integer> xLines = insertLines(oldXLines, xGridSize);
            List<Integer> yLines = insertLines(oldYLines, yGridSize);
            System.out.println("X Lines: " + xLines);
            System.out.println("Y Lines: " + yLines);

            // 8. Draw Grid and Save
            BufferedImage gridImage = drawGridLines(image, xLines, yLines);
            saveImage(gridImage, FileUtil.getTempFolder("grid_image.png"));

            // 9. Average Colors in Grid
            BufferedImage gridColors = averageColorsInGrid(image, xLines, yLines, xGridSize, yGridSize);
            System.out.println("Pixel shape: " + gridColors.getWidth() + "x" + gridColors.getHeight());
            BufferedImage pixels = padImageCentered(gridColors, calcPaddingSize(gridColors.getWidth(), gridColors.getHeight(), 1));
            saveImage(pixels, FileUtil.getTempFolder("grid_colors.png"));
            return pixels;
        } catch (IOException e) {
            System.err.println("An error occurred: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Reads an image and ensures it has an Alpha channel.
     */
    public static BufferedImage readImageFromBytes(byte[] data) throws IOException {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(data)) {
            return ImageIO.read(bais);  // may return null if not a supported format
        }
    }
    public static BufferedImage readImage(File file) throws IOException {
        BufferedImage originalImage = ImageIO.read(file);
        if (originalImage == null) {
            throw new IOException("Could not read image from path: " + file);
        }

        // Ensure image is of type ARGB
        if (originalImage.getType() == BufferedImage.TYPE_INT_ARGB) {
            return originalImage;
        }

        BufferedImage imageWithAlpha = new BufferedImage(originalImage.getWidth(), originalImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = imageWithAlpha.createGraphics();
        g.drawImage(originalImage, 0, 0, null);
        g.dispose();
        return imageWithAlpha;
    }

    /**
     * Saves a BufferedImage to a file.
     */
    public static void saveImage(BufferedImage image, File outputFile) throws IOException {
        ImageIO.write(image, "png", outputFile);
    }

    /**
     * Placeholder for Canny edge detection. Converts to grayscale and applies a threshold.
     */
    public static BufferedImage edgeDetection(BufferedImage image) {
        return cannyEdgeDetector.process(image);
    }

    /**
     * Extracts vertical lines using a morphological 'open' operation.
     */
    public static BufferedImage extractVerticalLines(BufferedImage edges) {
        // Vertical kernel of size 1x5
        int kernelWidth = 1;
        int kernelHeight = 5;
        BufferedImage eroded = erode(edges, kernelWidth, kernelHeight);
        return dilate(eroded, kernelWidth, kernelHeight);
    }

    /**
     * Extracts horizontal lines using a morphological 'open' operation.
     */
    public static BufferedImage extractHorizontalLines(BufferedImage edges) {
        // Horizontal kernel of size 5x1
        int kernelWidth = 5;
        int kernelHeight = 1;
        BufferedImage eroded = erode(edges, kernelWidth, kernelHeight);
        return dilate(eroded, kernelWidth, kernelHeight);
    }

    /**
     * Performs erosion on a binary image with a rectangular kernel.
     * In a binary image, erosion means the output pixel is white only if all pixels under the kernel are white.
     */
    private static BufferedImage erode(BufferedImage source, int kernelWidth, int kernelHeight) {
        BufferedImage destination = new BufferedImage(source.getWidth(), source.getHeight(), source.getType());
        int kw_half = kernelWidth / 2;
        int kh_half = kernelHeight / 2;

        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                boolean allWhite = true;
                for (int ky = -kh_half; ky <= kh_half; ky++) {
                    for (int kx = -kw_half; kx <= kw_half; kx++) {
                        int px = x + kx;
                        int py = y + ky;
                        if (px >= 0 && px < source.getWidth() && py >= 0 && py < source.getHeight()) {
                            if (source.getRGB(px, py) == BLACK_RGB) {
                                allWhite = false;
                                break;
                            }
                        }
                    }
                    if (!allWhite) break;
                }
                destination.setRGB(x, y, allWhite ? WHITE_RGB : BLACK_RGB);
            }
        }
        return destination;
    }

    /**
     * Performs dilation on a binary image with a rectangular kernel.
     * In a binary image, dilation means the output pixel is white if any pixel under the kernel is white.
     */
    private static BufferedImage dilate(BufferedImage source, int kernelWidth, int kernelHeight) {
        BufferedImage destination = new BufferedImage(source.getWidth(), source.getHeight(), source.getType());
        int kw_half = kernelWidth / 2;
        int kh_half = kernelHeight / 2;

        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                // Initialize to black
                destination.setRGB(x, y, BLACK_RGB);
                // If the source pixel is white, "paint" the kernel area white on the destination.
                if (source.getRGB(x, y) == WHITE_RGB) {
                    for (int ky = -kh_half; ky <= kh_half; ky++) {
                        for (int kx = -kw_half; kx <= kw_half; kx++) {
                            int px = x + kx;
                            int py = y + ky;
                            if (px >= 0 && px < source.getWidth() && py >= 0 && py < source.getHeight()) {
                                destination.setRGB(px, py, WHITE_RGB);
                            }
                        }
                    }
                }
            }
        }
        return destination;
    }

    /**
     * Creates a projection histogram of non-zero (white) pixels along the specified axis.
     */
    public static int[] histogramNonZero(BufferedImage edges, char axis) {
        int width = edges.getWidth();
        int height = edges.getHeight();
        int[] hist = (axis == 'x') ? new int[width] : new int[height];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (edges.getRGB(x, y) == WHITE_RGB) {
                    if (axis == 'x') {
                        hist[x]++;
                    } else {
                        hist[y]++;
                    }
                }
            }
        }
        return hist;
    }

    /**
     * Finds the indices of histogram values greater than a threshold.
     */
    public static int[] findPikes(int[] histogram, int threshold) {
        return IntStream.range(0, histogram.length)
                .filter(i -> histogram[i] > threshold)
                .toArray();
    }

    /**
     * Calculates the difference between consecutive elements in an array.
     */
    public static int[] calculateDifferences(int[] data) {
        if (data.length < 2) {
            return new int[0];
        }
        return IntStream.range(0, data.length - 1)
                .map(i -> data[i + 1] - data[i])
                .toArray();
    }

    /**
     * Combines small gaps into larger ones, similar to the Python logic.
     */
    public static int[] removeSmallLines(int[] array) {
        List<Integer> newList = new ArrayList<>();
        int temp = 0;
        for (int value : array) {
            if (value > 10) {
                newList.add(value + temp);
                temp = 0;
            } else {
                temp += value;
            }
        }
        return newList.stream().mapToInt(Integer::intValue).toArray();
    }

    /**
     * A simple gap-based clustering that finds the average of the first main cluster.
     */
    public static double clusterAverage(int[] data) {
        if (data.length == 0) return 0.0;

        List<Integer> sortedData = Arrays.stream(data).sorted().boxed().collect(Collectors.toList());

        // Find the first major gap to define the main cluster
        int threshold = 10;
        int splitIndex = sortedData.size(); // Default to including all data
        for (int i = 1; i < sortedData.size(); i++) {
            if (Math.abs(sortedData.get(i) - sortedData.get(i - 1)) > threshold) {
                splitIndex = i;
                break;
            }
        }

        // Average the values in the first cluster
        return sortedData.subList(0, splitIndex).stream()
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0.0);
    }

    /**
     * Removes nearby duplicate pikes from a sorted list.
     */
    public static List<Integer> removeDuplicates(int[] pikes, int threshold) {
        List<Integer> uniquePikes = new ArrayList<>();
        if (pikes.length == 0) return uniquePikes;

        uniquePikes.add(pikes[0]);
        for (int i = 1; i < pikes.length; i++) {
            if (Math.abs(pikes[i] - uniquePikes.get(uniquePikes.size() - 1)) > threshold) {
                uniquePikes.add(pikes[i]);
            }
        }
        return uniquePikes;
    }

    /**
     * Inserts new lines into the grid based on the expected grid size.
     */
    public static List<Integer> insertLines(List<Integer> lines, double gridSize) {
        List<Integer> newLines = new ArrayList<>();
        if (lines.isEmpty() || gridSize < 1) return lines;

        for (int i = 0; i < lines.size() - 1; i++) {
            int start = lines.get(i);
            int end = lines.get(i + 1);
            double d = end - start;
            double count = d / gridSize;
            int insertCount = (int) Math.round(count) - 1;
            if (insertCount < 0)
                continue;
            double dist = d / (insertCount + 1);
            newLines.add(start);

            for (int j = 0; j < insertCount; j++) {
                newLines.add((int) (start + dist * (j + 1)));
            }
        }
        newLines.add(lines.get(lines.size() - 1));

        // Remove duplicates that might have been introduced
        return newLines.stream().distinct().sorted().collect(Collectors.toList());
    }

    /**
     * Draws grid lines onto a copy of the image.
     */
    public static BufferedImage drawGridLines(BufferedImage image, List<Integer> xLines, List<Integer> yLines) {
        BufferedImage imageWithGrid = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = imageWithGrid.createGraphics();
        g.drawImage(image, 0, 0, null);
        g.setColor(Color.BLACK);

        for (int x : xLines) {
            g.drawLine(x, 0, x, image.getHeight());
        }
        for (int y : yLines) {
            g.drawLine(0, y, image.getWidth(), y);
        }
        g.dispose();
        return imageWithGrid;
    }

    /**
     * Calculates the average color within each grid cell and creates a new image from these colors.
     */
    public static BufferedImage averageColorsInGrid(BufferedImage image, List<Integer> xLines, List<Integer> yLines, double xGridSize, double yGridSize) {
        int gridCols = xLines.size() - 1;
        int gridRows = yLines.size() - 1;

        if (gridCols <= 0 || gridRows <= 0) {
            return new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        }

        BufferedImage colorGridImage = new BufferedImage(gridCols, gridRows, BufferedImage.TYPE_INT_ARGB);
        int deltaX = (int) (xGridSize / 4);
        int deltaY = (int) (yGridSize / 4);

        for (int i = 0; i < gridCols; i++) {
            for (int j = 0; j < gridRows; j++) {
                int xStart = xLines.get(i) + deltaX;
                int xEnd = xLines.get(i + 1) - deltaX;
                int yStart = yLines.get(j) + deltaY;
                int yEnd = yLines.get(j + 1) - deltaY;

                if (xStart >= xEnd || yStart >= yEnd) continue;

                long totalR = 0, totalG = 0, totalB = 0, totalA = 0;
                int pixelCount = 0;

                for (int x = xStart; x < xEnd; x++) {
                    for (int y = yStart; y < yEnd; y++) {
                        Color pixelColor = new Color(image.getRGB(x, y), true);
                        totalR += pixelColor.getRed();
                        totalG += pixelColor.getGreen();
                        totalB += pixelColor.getBlue();
                        totalA += pixelColor.getAlpha();
                        pixelCount++;
                    }
                }

                if (pixelCount > 0) {
                    int avgR = (int) (totalR / pixelCount);
                    int avgG = (int) (totalG / pixelCount);
                    int avgB = (int) (totalB / pixelCount);
                    int avgA = (int) (totalA / pixelCount);
                    avgA = (avgA > 128) ? 255 : 0;
                    colorGridImage.setRGB(i, j, new Color(avgR, avgG, avgB, avgA).getRGB());
                }
            }
        }

        return colorGridImage;
    }

    public static int[] computeBoundingBox(BufferedImage edgeImage, int pad) {
        int width = edgeImage.getWidth();
        int height = edgeImage.getHeight();
        int xMin = width, xMax = 0, yMin = height, yMax = 0;
        boolean found = false;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if ((edgeImage.getRGB(x, y) & 0xFFFFFF) != 0) {
                    xMin = Math.min(xMin, x);
                    xMax = Math.max(xMax, x);
                    yMin = Math.min(yMin, y);
                    yMax = Math.max(yMax, y);
                    found = true;
                }
            }
        }

        if (!found) return new int[]{0, 0, width, height};

        return new int[]{
                Math.max(xMin - pad, 0),
                Math.max(yMin - pad, 0),
                Math.min(xMax + pad + 1, width),
                Math.min(yMax + pad + 1, height)
        };
    }

    public static BufferedImage removeBackgroundAuto(BufferedImage image, BufferedImage edgeImage) {
        int width = image.getWidth();
        int height = image.getHeight();

        int[] bbox = computeBoundingBox(edgeImage, 5);
        int x1 = bbox[0], y1 = bbox[1], x2 = bbox[2], y2 = bbox[3];

        long totalR = 0, totalG = 0, totalB = 0;
        int count = 0;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (x < x1 || x >= x2 || y < y1 || y >= y2) {
                    Color c = new Color(image.getRGB(x, y), true);
                    totalR += c.getRed();
                    totalG += c.getGreen();
                    totalB += c.getBlue();
                    count++;
                }
            }
        }

        if (count == 0) return image;

        float bgR = totalR / (float) count;
        float bgG = totalG / (float) count;
        float bgB = totalB / (float) count;

        float maxDist = 0f;

        float[][] distMap = new float[width][height];
        boolean[][] isOutside = new boolean[width][height];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                boolean outside = (x < x1 || x >= x2 || y < y1 || y >= y2);
                Color c = new Color(image.getRGB(x, y), true);
                float dist = (float) Math.sqrt(
                        Math.pow(c.getRed() - bgR, 2) +
                                Math.pow(c.getGreen() - bgG, 2) +
                                Math.pow(c.getBlue() - bgB, 2)
                );
                distMap[x][y] = dist;
                isOutside[x][y] = outside;
                if (outside && dist > maxDist) maxDist = dist;
            }
        }

        float tolerance = maxDist + 1e-5f;
        BufferedImage output = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color c = new Color(image.getRGB(x, y), true);
                int alpha = distMap[x][y] < tolerance ? 0 : 255;
                Color newColor = new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
                output.setRGB(x, y, newColor.getRGB());
            }
        }

        return output;
    }

    public static int calcPaddingSize(int x, int y, int expand) {
        x += expand;
        y += expand;
        int i = Math.max(x, y);
        i = ((i + 7) / 8) * 8; // ceil to nearest multiple of 8
        return Math.max(i, 16);
    }

    public static BufferedImage padImageCentered(BufferedImage image, int targetSize) {
        int w = image.getWidth();
        int h = image.getHeight();

        int padLeft = (targetSize - w) / 2;
        int padTop = (targetSize - h) / 2;

        BufferedImage padded = new BufferedImage(targetSize, targetSize, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = padded.createGraphics();
        g.setColor(new Color(0, 0, 0, 0));
        g.fillRect(0, 0, targetSize, targetSize);
        g.drawImage(image, padLeft, padTop, null);
        g.dispose();
        return padded;
    }

    public static int[] findNearestRatio(double x, double y) {
        double[][] ratios = {
                {1, 1}, {1, 2}, {1, 3}, {1, 4}, {2, 3},
                {2, 1}, {3, 1}, {4, 1}, {3, 2}
        };
        double r = x / y;
        double minDiff = Double.MAX_VALUE;
        int[] best = {1, 1};

        for (double[] ratio : ratios) {
            double diff = Math.abs((ratio[0] / ratio[1]) - r);
            if (diff < minDiff) {
                minDiff = diff;
                best[0] = (int) ratio[0];
                best[1] = (int) ratio[1];
            }
        }

        return best;
    }

}