package com.watermelon0117.aicraft.client.block;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;

/**
 * Finds the parts of a model's faces that are on its outside. A vanilla model is boxes that overlap, and a face inside
 * another box only stays hidden while the block is opaque. The inside comes from the faces alone, because a block's
 * shape is not its model: a fence's outline is one thick bar where the model has two thin ones.
 */
public final class InnerFaces {
    private static final int AXES = 3;

    private InnerFaces() {
    }

    /** One face of a model, in sixteenths of a block. @param axis 0, 1 or 2 for x, y or z */
    public record Face(int axis, boolean positive, int plane, int uMin, int uMax, int vMin, int vMax) {
        Rect whole() {
            return new Rect(uMin, uMax, vMin, vMax);
        }
    }

    /** A part of a {@link Face}, in the same two axes. */
    public record Rect(int uMin, int uMax, int vMin, int vMax) {
    }

    /** @return the first of the two axes in a face across this axis */
    public static int uAxis(int axis) {
        return axis == 0 ? 1 : 0;
    }

    /** @return the second of the two axes in a face across this axis */
    public static int vAxis(int axis) {
        return axis == 2 ? 1 : 2;
    }

    /**
     * A part goes if the space in front of it is inside the model, or if an earlier face already covers it the same way,
     * which would draw a tinted pixel twice. A model that is not closed has no inside, so nothing goes for that reason.
     *
     * @return for each face in order, what to keep: the face itself when all of it stays, nothing when none of it does
     */
    public static List<List<Rect>> outsideParts(List<Face> faces) {
        if (faces.isEmpty()) {
            return List.of();
        }
        Grid grid = new Grid(faces);
        boolean[] outside = grid.reachableFromOutside(faces);
        boolean[][] covered = new boolean[AXES * 2][grid.cells];
        List<List<Rect>> parts = new ArrayList<>(faces.size());
        for (Face face : faces) {
            parts.add(keptParts(face, grid, outside, covered[face.axis * 2 + (face.positive ? 1 : 0)]));
        }
        return parts;
    }

    private static List<Rect> keptParts(Face face, Grid grid, boolean[] outside, boolean[] covered) {
        int width = face.uMax - face.uMin;
        int height = face.vMax - face.vMin;
        boolean[] kept = new boolean[width * height];
        int keptCount = 0;
        int[] position = new int[AXES];
        for (int v = 0; v < height; v++) {
            for (int u = 0; u < width; u++) {
                position[uAxis(face.axis)] = face.uMin + u;
                position[vAxis(face.axis)] = face.vMin + v;
                position[face.axis] = face.positive ? face.plane : face.plane - 1;
                boolean inFrontIsInside = !outside[grid.index(position)];
                position[face.axis] = face.plane;
                int cell = grid.index(position);
                if (!inFrontIsInside && !covered[cell]) {
                    covered[cell] = true;
                    kept[v * width + u] = true;
                    keptCount++;
                }
            }
        }
        return keptCount == width * height ? List.of(face.whole()) : rectangles(face, kept, width, height);
    }

    /** Takes the largest rectangle from each uncovered corner in turn, so few quads replace the face. */
    private static List<Rect> rectangles(Face face, boolean[] kept, int width, int height) {
        List<Rect> result = new ArrayList<>();
        for (int v = 0; v < height; v++) {
            for (int u = 0; u < width; u++) {
                if (!kept[v * width + u]) {
                    continue;
                }
                int uEnd = u + 1;
                while (uEnd < width && kept[v * width + uEnd]) {
                    uEnd++;
                }
                int vEnd = v + 1;
                while (vEnd < height && rowKept(kept, width, vEnd, u, uEnd)) {
                    vEnd++;
                }
                for (int row = v; row < vEnd; row++) {
                    Arrays.fill(kept, row * width + u, row * width + uEnd, false);
                }
                result.add(new Rect(face.uMin + u, face.uMin + uEnd, face.vMin + v, face.vMin + vEnd));
            }
        }
        return result;
    }

    private static boolean rowKept(boolean[] kept, int width, int row, int uStart, int uEnd) {
        for (int u = uStart; u < uEnd; u++) {
            if (!kept[row * width + u]) {
                return false;
            }
        }
        return true;
    }

    /** Sixteenth-sized cells around every face, with one cell to spare on each side so that the outside is connected. */
    private static final class Grid {
        final int[] low = new int[AXES];
        final int[] size = new int[AXES];
        final int cells;

        Grid(List<Face> faces) {
            int[] high = new int[AXES];
            Arrays.fill(low, Integer.MAX_VALUE);
            Arrays.fill(high, Integer.MIN_VALUE);
            for (Face face : faces) {
                include(high, face.axis, face.plane, face.plane);
                include(high, uAxis(face.axis), face.uMin, face.uMax);
                include(high, vAxis(face.axis), face.vMin, face.vMax);
            }
            int total = 1;
            for (int axis = 0; axis < AXES; axis++) {
                low[axis]--;
                size[axis] = high[axis] + 1 - low[axis] + 1;
                total *= size[axis];
            }
            this.cells = total;
        }

        private void include(int[] high, int axis, int min, int max) {
            low[axis] = Math.min(low[axis], min);
            high[axis] = Math.max(high[axis], max);
        }

        int index(int[] position) {
            int index = 0;
            for (int axis = 0; axis < AXES; axis++) {
                index = index * size[axis] + position[axis] - low[axis];
            }
            return index;
        }

        boolean[] reachableFromOutside(List<Face> faces) {
            boolean[][] walls = new boolean[AXES][cells];
            int[] position = new int[AXES];
            for (Face face : faces) {
                position[face.axis] = face.plane;
                for (int u = face.uMin; u < face.uMax; u++) {
                    for (int v = face.vMin; v < face.vMax; v++) {
                        position[uAxis(face.axis)] = u;
                        position[vAxis(face.axis)] = v;
                        walls[face.axis][index(position)] = true;
                    }
                }
            }
            boolean[] reached = new boolean[cells];
            int[] start = low.clone();
            reached[index(start)] = true;
            Deque<int[]> queue = new ArrayDeque<>();
            queue.add(start);
            while (!queue.isEmpty()) {
                int[] cell = queue.remove();
                for (int axis = 0; axis < AXES; axis++) {
                    step(cell, axis, -1, walls[axis], reached, queue);
                    step(cell, axis, 1, walls[axis], reached, queue);
                }
            }
            return reached;
        }

        /** A wall at a plane stands on the low side of the cell that starts there. */
        private void step(int[] from, int axis, int direction, boolean[] walls, boolean[] reached, Deque<int[]> queue) {
            int[] to = from.clone();
            to[axis] += direction;
            if (to[axis] < low[axis] || to[axis] >= low[axis] + size[axis]) {
                return;
            }
            int index = index(to);
            if (!reached[index] && !walls[index(direction > 0 ? to : from)]) {
                reached[index] = true;
                queue.add(to);
            }
        }
    }
}
