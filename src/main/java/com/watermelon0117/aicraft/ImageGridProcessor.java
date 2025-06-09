package com.watermelon0117.aicraft;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ImageGridProcessor {
    private static final int WHITE_RGB = Color.WHITE.getRGB();
    private static final int BLACK_RGB = Color.BLACK.getRGB();
    private static final CannyEdgeDetector cannyEdgeDetector = new CannyEdgeDetector(1,100,200);

    public static BufferedImage process() {
        try {
            // 1. Read Image
            BufferedImage image = readImage("C:\\achieve\\AICraftingTable\\gpt\\8.png");
            System.out.println("Image shape: " + image.getWidth() + "x" + image.getHeight());

            // 2. Edge Detection
            BufferedImage edges = edgeDetection(image);
            saveImage(edges, "C:\\achieve\\AICraftingTable\\gpt\\edges.png");

            // 3. Extract Lines using Morphological Operations
            BufferedImage verticalLines = extractVerticalLines(edges);
            saveImage(verticalLines, "C:\\achieve\\AICraftingTable\\gpt\\vertical_lines.png");

            BufferedImage horizontalLines = extractHorizontalLines(edges);
            saveImage(horizontalLines, "C:\\achieve\\AICraftingTable\\gpt\\horizontal_lines.png");

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

            // 7. Refine and Insert Lines
            List<Integer> oldXLines = removeDuplicates(xPikes, 5);
            List<Integer> oldYLines = removeDuplicates(yPikes, 5);

            // Add image boundaries to the list of lines
            oldXLines.add(0, 0);
            oldXLines.add(image.getWidth());
            oldYLines.add(0, 0);
            oldYLines.add(image.getHeight());
            Collections.sort(oldXLines);
            Collections.sort(oldYLines);


            List<Integer> xLines = insertLines(oldXLines, xGridSize);
            List<Integer> yLines = insertLines(oldYLines, yGridSize);
            System.out.println("X Lines: " + xLines);
            System.out.println("Y Lines: " + yLines);

            // 8. Draw Grid and Save
            BufferedImage gridImage = drawGridLines(image, xLines, yLines);
            saveImage(gridImage, "C:\\achieve\\AICraftingTable\\gpt\\grid_image.png");

            // 9. Average Colors in Grid
            BufferedImage gridColors = averageColorsInGrid(image, xLines, yLines, xGridSize, yGridSize);
            System.out.println("Pixel shape: " + gridColors.getWidth() + "x" + gridColors.getHeight());
            saveImage(gridColors, "C:\\achieve\\AICraftingTable\\gpt\\grid_colors.png");
            return gridColors;
        } catch (IOException e) {
            System.err.println("An error occurred: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Reads an image and ensures it has an Alpha channel.
     */
    public static BufferedImage readImage(String filePath) throws IOException {
        File file = new File(filePath);
        BufferedImage originalImage = ImageIO.read(file);
        if (originalImage == null) {
            throw new IOException("Could not read image from path: " + filePath);
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
    public static void saveImage(BufferedImage image, String outputPath) throws IOException {
        File outputFile = new File(outputPath);
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
            double diff = end - start;
            int count = (int) Math.round(diff / gridSize);

            newLines.add(start);
            if (count > 1) {
                double step = diff / count;
                for (int j = 1; j < count; j++) {
                    newLines.add((int) Math.round(start + j * step));
                }
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
                    avgA=(avgA>128)?255:0;
                    colorGridImage.setRGB(i, j, new Color(avgR, avgG, avgB, avgA).getRGB());
                }
            }
        }

        return colorGridImage;
    }

}