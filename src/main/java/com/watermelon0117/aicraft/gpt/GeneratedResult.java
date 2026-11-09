package com.watermelon0117.aicraft.gpt;

/** What the texture server made of a name: an item, or a block of some shape. */
public sealed interface GeneratedResult permits GeneratedItem, GeneratedBlock {
    /** How many one craft makes. */
    int count();

    /** Whether the recipe ignores the layout of its materials. */
    boolean shapeless();

    /** What {@code /textures/auto} answers. An item fills {@code b64Image} and {@code json}; a block fills the rest. */
    record Response(String type, String b64Image, String json,
                    String model, String kind, String tool, String hardness, String light, String sound, Boolean flammable,
                    Integer count, Boolean shapeless,
                    String texture, String top, String front, String side, String attach) {
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
