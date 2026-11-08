package com.watermelon0117.aicraft.client.block;

import com.watermelon0117.aicraft.client.block.InnerFaces.Face;
import com.watermelon0117.aicraft.client.block.InnerFaces.Rect;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The boxes and the sides each one has are copied from vanilla's model files. */
class InnerFacesTest {
    private static final int LOW = -4;
    private static final int SPAN = 24;

    private record Box(int x0, int y0, int z0, int x1, int y1, int z1, String sides) {
        /** A quarter turn clockwise from above, as a blockstate's y of 90 does. */
        Box turned() {
            StringBuilder turnedSides = new StringBuilder();
            for (char side : sides.toCharArray()) {
                turnedSides.append(switch (side) {
                    case 'n' -> 'e';
                    case 'e' -> 's';
                    case 's' -> 'w';
                    case 'w' -> 'n';
                    default -> side;
                });
            }
            return new Box(16 - z1, y0, x0, 16 - z0, y1, x1, turnedSides.toString());
        }

        Box turned(int quarters) {
            Box box = this;
            for (int i = 0; i < quarters; i++) {
                box = box.turned();
            }
            return box;
        }

        boolean holds(int x, int y, int z) {
            return x >= x0 && x < x1 && y >= y0 && y < y1 && z >= z0 && z < z1;
        }

        void addFaces(List<Face> faces) {
            for (char side : sides.toCharArray()) {
                faces.add(switch (side) {
                    case 'd' -> new Face(1, false, y0, x0, x1, z0, z1);
                    case 'u' -> new Face(1, true, y1, x0, x1, z0, z1);
                    case 'n' -> new Face(2, false, z0, x0, x1, y0, y1);
                    case 's' -> new Face(2, true, z1, x0, x1, y0, y1);
                    case 'w' -> new Face(0, false, x0, y0, y1, z0, z1);
                    case 'e' -> new Face(0, true, x1, y0, y1, z0, z1);
                    default -> throw new IllegalArgumentException("side " + side);
                });
            }
        }
    }

    private static final Box SLAB_PART = new Box(0, 0, 0, 16, 8, 16, "dunswe");
    private static final Box STEP = new Box(8, 8, 0, 16, 16, 16, "unswe");
    private static final Box INNER_EXTRA = new Box(0, 8, 8, 8, 16, 16, "unsw");
    private static final Box OUTER_STEP = new Box(8, 8, 8, 16, 16, 16, "unswe");
    private static final Box FENCE_POST = new Box(6, 0, 6, 10, 16, 10, "dunswe");
    private static final Box FENCE_TOP_BAR = new Box(7, 12, 0, 9, 15, 9, "dunwe");
    private static final Box FENCE_LOW_BAR = new Box(7, 6, 0, 9, 9, 9, "dunwe");
    private static final Box WALL_POST = new Box(4, 0, 4, 12, 16, 12, "dunswe");
    private static final Box WALL_LOW = new Box(5, 0, 0, 11, 14, 8, "dunwe");
    private static final Box WALL_TALL = new Box(5, 0, 0, 11, 16, 8, "dunwe");

    private static List<Face> facesOf(List<Box> boxes) {
        List<Face> faces = new ArrayList<>();
        boxes.forEach(box -> box.addFaces(faces));
        return faces;
    }

    private static String cell(int axis, boolean positive, int plane, int u, int v) {
        return axis + (positive ? "+" : "-") + plane + ":" + u + "," + v;
    }

    /** What the union of the boxes shows, worked out from the boxes and not from the faces. */
    private static Set<String> exposedCells(List<Box> boxes) {
        boolean[][][] solid = new boolean[SPAN][SPAN][SPAN];
        for (int x = 0; x < SPAN; x++) {
            for (int y = 0; y < SPAN; y++) {
                for (int z = 0; z < SPAN; z++) {
                    for (Box box : boxes) {
                        solid[x][y][z] |= box.holds(x + LOW, y + LOW, z + LOW);
                    }
                }
            }
        }
        Set<String> cells = new HashSet<>();
        int[] c = new int[3];
        for (c[0] = 1; c[0] < SPAN - 1; c[0]++) {
            for (c[1] = 1; c[1] < SPAN - 1; c[1]++) {
                for (c[2] = 1; c[2] < SPAN - 1; c[2]++) {
                    if (!solid[c[0]][c[1]][c[2]]) {
                        continue;
                    }
                    for (int axis = 0; axis < 3; axis++) {
                        for (boolean positive : new boolean[]{false, true}) {
                            int[] next = c.clone();
                            next[axis] += positive ? 1 : -1;
                            if (!solid[next[0]][next[1]][next[2]]) {
                                cells.add(cell(axis, positive, c[axis] + LOW + (positive ? 1 : 0),
                                        c[InnerFaces.uAxis(axis)] + LOW, c[InnerFaces.vAxis(axis)] + LOW));
                            }
                        }
                    }
                }
            }
        }
        return cells;
    }

    private static int assertOnlyTheOutsideIsKept(List<Box> boxes) {
        List<Face> faces = facesOf(boxes);
        List<List<Rect>> parts = InnerFaces.outsideParts(faces);
        Set<String> kept = new HashSet<>();
        int area = 0;
        for (int i = 0; i < faces.size(); i++) {
            Face face = faces.get(i);
            for (Rect rect : parts.get(i)) {
                for (int u = rect.uMin(); u < rect.uMax(); u++) {
                    for (int v = rect.vMin(); v < rect.vMax(); v++) {
                        assertTrue(kept.add(cell(face.axis(), face.positive(), face.plane(), u, v)), "drawn twice");
                        area++;
                    }
                }
            }
        }
        assertEquals(exposedCells(boxes), kept);
        return area;
    }

    private static int area(List<Box> boxes) {
        return facesOf(boxes).stream().mapToInt(face -> (face.uMax() - face.uMin()) * (face.vMax() - face.vMin())).sum();
    }

    @Test
    void aStraightStairLosesTheSlabTopUnderItsStep() {
        List<Box> stairs = List.of(SLAB_PART, STEP);
        assertEquals(1536, area(stairs));
        assertEquals(1408, assertOnlyTheOutsideIsKept(stairs));

        List<Face> faces = facesOf(stairs);
        assertEquals(new Face(1, true, 8, 0, 16, 0, 16), faces.get(1));
        assertEquals(List.of(new Rect(0, 8, 0, 16)), InnerFaces.outsideParts(faces).get(1));
    }

    @Test
    void cornerStairsKeepOnlyTheirOutside() {
        assertEquals(1536, assertOnlyTheOutsideIsKept(List.of(SLAB_PART, STEP, INNER_EXTRA)));
        assertEquals(1280, assertOnlyTheOutsideIsKept(List.of(SLAB_PART, OUTER_STEP)));
    }

    @Test
    void aFenceKeepsOnlyItsOutsideWithAnyArms() {
        for (int arms = 0; arms < 16; arms++) {
            List<Box> boxes = new ArrayList<>(List.of(FENCE_POST));
            for (int side = 0; side < 4; side++) {
                if ((arms & 1 << side) != 0) {
                    boxes.add(FENCE_TOP_BAR.turned(side));
                    boxes.add(FENCE_LOW_BAR.turned(side));
                }
            }
            assertOnlyTheOutsideIsKept(boxes);
        }
    }

    @Test
    void aWallWithAPostKeepsOnlyItsOutsideWithAnySides() {
        Box[] heights = {null, WALL_LOW, WALL_TALL};
        for (Box north : heights) {
            for (Box east : heights) {
                for (Box south : heights) {
                    for (Box west : heights) {
                        List<Box> boxes = new ArrayList<>(List.of(WALL_POST));
                        Box[] sides = {north, east, south, west};
                        for (int side = 0; side < 4; side++) {
                            if (sides[side] != null) {
                                boxes.add(sides[side].turned(side));
                            }
                        }
                        assertOnlyTheOutsideIsKept(boxes);
                    }
                }
            }
        }
    }

    @Test
    void aWallWithoutAPostKeepsOnlyItsOutside() {
        for (Box height : List.of(WALL_LOW, WALL_TALL)) {
            assertOnlyTheOutsideIsKept(List.of(height, height.turned(2)));
            assertOnlyTheOutsideIsKept(List.of(height.turned(1), height.turned(3)));
            assertOnlyTheOutsideIsKept(List.of(height, height.turned(1), height.turned(2), height.turned(3)));
        }
    }

    private static Set<String> cellsOf(List<Face> faces) {
        Set<String> cells = new HashSet<>();
        for (Face face : faces) {
            for (int u = face.uMin(); u < face.uMax(); u++) {
                for (int v = face.vMin(); v < face.vMax(); v++) {
                    cells.add(cell(face.axis(), face.positive(), face.plane(), u, v));
                }
            }
        }
        return cells;
    }

    private static boolean frontIsInABox(Face face, int u, int v, List<Box> boxes) {
        int[] front = new int[3];
        front[face.axis()] = face.positive() ? face.plane() : face.plane() - 1;
        front[InnerFaces.uAxis(face.axis())] = u;
        front[InnerFaces.vAxis(face.axis())] = v;
        return boxes.stream().anyMatch(box -> box.holds(front[0], front[1], front[2]));
    }

    /** Vanilla leaves a side's end without a face, so where nothing meets that end the air gets in, and only what stays sealed counts as inside. */
    @Test
    void aWallWithAHoleLosesOnlyWhatAnotherBoxCovers() {
        Box[] heights = {null, WALL_LOW, WALL_TALL};
        int closed = 0, holed = 0;
        for (Box north : heights) {
            for (Box east : heights) {
                for (Box south : heights) {
                    for (Box west : heights) {
                        List<Box> boxes = new ArrayList<>();
                        Box[] sides = {north, east, south, west};
                        for (int side = 0; side < 4; side++) {
                            if (sides[side] != null) {
                                boxes.add(sides[side].turned(side));
                            }
                        }
                        if (boxes.isEmpty()) {
                            continue;
                        }
                        List<Face> faces = facesOf(boxes);
                        if (cellsOf(faces).containsAll(exposedCells(boxes))) {
                            closed++;
                            assertOnlyTheOutsideIsKept(boxes);
                            continue;
                        }
                        holed++;
                        List<List<Rect>> parts = InnerFaces.outsideParts(faces);
                        List<Face> keptFaces = new ArrayList<>();
                        int area = 0;
                        for (int i = 0; i < faces.size(); i++) {
                            Face face = faces.get(i);
                            for (Rect rect : parts.get(i)) {
                                keptFaces.add(new Face(face.axis(), face.positive(), face.plane(), rect.uMin(), rect.uMax(), rect.vMin(), rect.vMax()));
                                area += (rect.uMax() - rect.uMin()) * (rect.vMax() - rect.vMin());
                            }
                        }
                        Set<String> kept = cellsOf(keptFaces);
                        assertEquals(kept.size(), area, "drawn twice");
                        Set<String> visible = exposedCells(boxes);
                        visible.retainAll(cellsOf(faces));
                        assertTrue(kept.containsAll(visible), "lost a visible face");
                        for (Face face : faces) {
                            for (int u = face.uMin(); u < face.uMax(); u++) {
                                for (int v = face.vMin(); v < face.vMax(); v++) {
                                    assertTrue(kept.contains(cell(face.axis(), face.positive(), face.plane(), u, v)) || frontIsInABox(face, u, v, boxes), "cut in the open");
                                }
                            }
                        }
                    }
                }
            }
        }
        assertEquals(24, closed);
        assertEquals(56, holed);
    }

    @Test
    void inventoryModelsThatLeaveTheBlockKeepOnlyTheirOutside() {
        assertOnlyTheOutsideIsKept(List.of(
                new Box(6, 0, 0, 10, 16, 4, "dunswe"), new Box(6, 0, 12, 10, 16, 16, "dunswe"),
                new Box(7, 12, 0, 9, 15, 16, "duwe"), new Box(7, 12, -2, 9, 15, 0, "dunwe"), new Box(7, 12, 16, 9, 15, 18, "duswe"),
                new Box(7, 6, 0, 9, 9, 16, "duwe"), new Box(7, 6, -2, 9, 9, 0, "dunwe"), new Box(7, 6, 16, 9, 9, 18, "duswe")));
        assertOnlyTheOutsideIsKept(List.of(WALL_POST, new Box(5, 0, 0, 11, 13, 16, "dunswe")));
    }

    @Test
    void aSingleBoxKeepsEveryFaceWhole() {
        List<Face> faces = facesOf(List.of(new Box(0, 0, 0, 16, 8, 16, "dunswe")));
        List<List<Rect>> parts = InnerFaces.outsideParts(faces);
        for (int i = 0; i < faces.size(); i++) {
            assertEquals(List.of(faces.get(i).whole()), parts.get(i));
        }
    }

    @Test
    void aModelThatIsNotClosedHasNoInside() {
        List<Face> faces = facesOf(List.of(new Box(0, 0, 0, 16, 8, 16, "dnswe"), STEP));
        List<List<Rect>> parts = InnerFaces.outsideParts(faces);
        for (int i = 0; i < faces.size(); i++) {
            assertEquals(List.of(faces.get(i).whole()), parts.get(i));
        }
    }

    /** A wall state with no post and no sides draws nothing. */
    @Test
    void aModelWithNoFacesHasNothingToKeep() {
        assertEquals(List.of(), InnerFaces.outsideParts(List.of()));
    }

    @Test
    void anOverlapFacingTheSameWayIsDrawnOnce() {
        List<Face> faces = List.of(new Face(1, true, 16, 4, 12, 4, 12), new Face(1, true, 16, 5, 11, 0, 8));
        List<List<Rect>> parts = InnerFaces.outsideParts(faces);
        assertEquals(List.of(new Rect(4, 12, 4, 12)), parts.get(0));
        assertEquals(List.of(new Rect(5, 11, 0, 4)), parts.get(1));
    }
}
