package com.watermelon0117.aicraft.gpt;

/** What the texture server made of a name: an item, or a block of some shape. */
public sealed interface GeneratedResult permits GeneratedItem, GeneratedBlock {
    /** How many one craft makes. */
    int count();

    /** Whether the recipe ignores the layout of its materials. */
    boolean shapeless();

    /**
     * What {@code /textures/auto} answers. An item fills {@code b64Image} and {@code json}, and {@code armor} when the answer names
     * the slot it is worn in; a block fills the rest.
     */
    record Response(String type, String b64Image, String json,
                    String model, String kind, String tool, String hardness, String light, String sound, Boolean flammable,
                    Integer count, Boolean shapeless,
                    String texture, String top, String front, String side, String attach, Armor armor) {
        /** Every answer but an armor piece's. */
        public Response(String type, String b64Image, String json,
                        String model, String kind, String tool, String hardness, String light, String sound, Boolean flammable,
                        Integer count, Boolean shapeless,
                        String texture, String top, String front, String side, String attach) {
            this(type, b64Image, json, model, kind, tool, hardness, light, sound, flammable, count, shapeless, texture, top, front, side, attach, null);
        }
    }

    /** The picture of an armor piece as worn: the atlas as base64 PNG and its UV table (ArmorUV). */
    record Armor(String slot, String atlas, String uv) {
    }

    /** @throws IllegalArgumentException if the answer is neither a usable item nor a usable block */
    static GeneratedResult fromResponse(Response response, String id, String name) {
        if ("block".equals(response.type())) {
            return GeneratedBlock.fromResponse(response);
        }
        if ("item".equals(response.type())) {
            return GeneratedItem.fromResponse(response, id, name);
        }
        throw new IllegalArgumentException("The texture server's answer has an unknown type: " + response.type());
    }
}
