package com.watermelon0117.aicraft.common;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Where each face of a worn armor piece sits in its picture: one rectangle per face, read from the image model's own pixels, so faces
 * of one piece can hold different pixel counts. Vanilla's 64x32 box layout gives every face of a box the same scale and cannot.
 * The format is the one the armor test (test/armor_texture2, method W) writes: {"slot", "size": [w, h], "faces": {"BOX.face": [x, y, w, h, ...]}}.
 */
public record ArmorUV(Slot slot, int width, int height, Map<String, Rect> faces) {
    /** The largest picture a piece may have. The test's largest is 118x48. */
    public static final int MAX_SIDE = 256;

    public static final String HEAD = "HEAD";
    public static final String BODY = "BODY";
    public static final String R_ARM = "R_ARM";
    public static final String R_LEG = "R_LEG";
    public static final List<String> FACE_NAMES = List.of("front", "back", "right", "left", "top", "bottom");

    /** The slot and the faces vanilla's armor covers on it: a piece missing one would show a hole there. Left limbs read the right ones, mirrored. */
    public enum Slot {
        HELMET("helmet", Map.of(HEAD, List.of("front", "back", "right", "left", "top"))),
        CHESTPLATE("chestplate", Map.of(BODY, List.of("front", "back", "right", "left", "top"),
                R_ARM, List.of("front", "back", "right", "left", "top"))),
        LEGGINGS("leggings", Map.of(BODY, List.of("front", "back", "right", "left"),
                R_LEG, List.of("front", "back", "right", "left", "top"))),
        BOOTS("boots", Map.of(R_LEG, List.of("front", "back", "right", "left", "bottom")));

        private final String key;
        private final Map<String, List<String>> required;

        Slot(String key, Map<String, List<String>> required) {
            this.key = key;
            this.required = required;
        }

        public String key() {
            return key;
        }

        /** @return the boxes this slot draws, and the faces each one must have */
        public Map<String, List<String>> required() {
            return required;
        }

        public static Slot of(String key) {
            for (Slot slot : values()) {
                if (slot.key.equals(key)) {
                    return slot;
                }
            }
            throw new IllegalArgumentException("Unknown armor slot: " + key);
        }
    }

    /** A face's block of the picture, in pixels from its top left. */
    public record Rect(int x, int y, int w, int h) {
    }

    /** @throws IllegalArgumentException with the reason, if the table is not one a piece can be drawn from */
    public static ArmorUV parse(String json) {
        JsonObject root;
        try {
            root = JsonParser.parseString(json).getAsJsonObject();
        } catch (JsonParseException | IllegalStateException e) {
            throw new IllegalArgumentException("The armor UV table is not a JSON object", e);
        }
        Slot slot = Slot.of(string(root, "slot"));
        JsonArray size = array(root, "size");
        if (size.size() != 2) {
            throw new IllegalArgumentException("The armor UV table's size is not [width, height]");
        }
        int width = integer(size.get(0), "size"), height = integer(size.get(1), "size");
        if (width < 1 || height < 1 || width > MAX_SIDE || height > MAX_SIDE) {
            throw new IllegalArgumentException("The armor picture is " + width + "x" + height + ", outside 1 to " + MAX_SIDE);
        }
        JsonElement facesElement = root.get("faces");
        if (facesElement == null || !facesElement.isJsonObject()) {
            throw new IllegalArgumentException("The armor UV table has no faces");
        }
        Map<String, Rect> faces = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : facesElement.getAsJsonObject().entrySet()) {
            String key = entry.getKey();
            int dot = key.indexOf('.');
            String box = dot < 0 ? key : key.substring(0, dot);
            String face = dot < 0 ? "" : key.substring(dot + 1);
            if (!slot.required.containsKey(box) || !FACE_NAMES.contains(face)) {
                throw new IllegalArgumentException("A " + slot.key + " has no face " + key);
            }
            if (!entry.getValue().isJsonArray() || entry.getValue().getAsJsonArray().size() < 4) {
                throw new IllegalArgumentException("Face " + key + " is not [x, y, w, h]");
            }
            JsonArray r = entry.getValue().getAsJsonArray();
            Rect rect = new Rect(integer(r.get(0), key), integer(r.get(1), key), integer(r.get(2), key), integer(r.get(3), key));
            if (rect.x < 0 || rect.y < 0 || rect.w < 1 || rect.h < 1 || rect.x + rect.w > width || rect.y + rect.h > height) {
                throw new IllegalArgumentException("Face " + key + " " + rect + " lies outside the " + width + "x" + height + " picture");
            }
            faces.put(key, rect);
        }
        slot.required.forEach((box, names) -> {
            for (String name : names) {
                if (!faces.containsKey(box + "." + name)) {
                    throw new IllegalArgumentException("The " + slot.key + " has no " + box + "." + name);
                }
            }
        });
        return new ArmorUV(slot, width, height, Collections.unmodifiableMap(faces));
    }

    /** One pixel for every face the slot draws, for a piece whose picture has not arrived. */
    public static ArmorUV plain(Slot slot) {
        Map<String, Rect> faces = new LinkedHashMap<>();
        slot.required.forEach((box, names) -> names.forEach(name -> faces.put(box + "." + name, new Rect(0, 0, 1, 1))));
        return new ArmorUV(slot, 1, 1, Collections.unmodifiableMap(faces));
    }

    public Rect face(String box, String face) {
        return faces.get(box + "." + face);
    }

    public Set<String> boxes() {
        return slot.required.keySet();
    }

    private static String string(JsonObject root, String key) {
        JsonElement element = root.get(key);
        if (element == null || !element.isJsonPrimitive()) {
            throw new IllegalArgumentException("The armor UV table has no " + key);
        }
        return element.getAsString().toLowerCase(Locale.ROOT);
    }

    private static JsonArray array(JsonObject root, String key) {
        JsonElement element = root.get(key);
        if (element == null || !element.isJsonArray()) {
            throw new IllegalArgumentException("The armor UV table has no " + key);
        }
        return element.getAsJsonArray();
    }

    private static int integer(JsonElement element, String where) {
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
            throw new IllegalArgumentException("A number in " + where + " is not one");
        }
        double value = element.getAsDouble();
        if (value != Math.rint(value) || Math.abs(value) > 1_000_000) {
            throw new IllegalArgumentException("A number in " + where + " is not a whole number: " + value);
        }
        return (int) value;
    }
}
