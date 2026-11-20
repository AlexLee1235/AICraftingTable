package com.watermelon0117.aicraft.gpt;

/** What the texture server made of a name: an item, or a block of some shape. */
public sealed interface GeneratedResult permits GeneratedItem, GeneratedBlock {
    /** How many one craft makes. */
    int count();

    /** Whether the recipe ignores the layout of its materials. */
    boolean shapeless();

    /**
     * What {@code /textures/auto} answers. An item fills {@code b64Image} and {@code json}; a block fills the rest.
     * A class, not the record 1.20.1 uses: Minecraft 1.19.2 ships Gson 2.8.9, which cannot fill a record's fields.
     */
    final class Response {
        private String type;
        private String b64Image;
        private String json;
        private String model;
        private String kind;
        private String tool;
        private String hardness;
        private String light;
        private String sound;
        private Boolean flammable;
        private Integer count;
        private Boolean shapeless;
        private String texture;
        private String top;
        private String front;
        private String side;
        private String attach;

        public Response(String type, String b64Image, String json,
                        String model, String kind, String tool, String hardness, String light, String sound, Boolean flammable,
                        Integer count, Boolean shapeless,
                        String texture, String top, String front, String side, String attach) {
            this.type = type;
            this.b64Image = b64Image;
            this.json = json;
            this.model = model;
            this.kind = kind;
            this.tool = tool;
            this.hardness = hardness;
            this.light = light;
            this.sound = sound;
            this.flammable = flammable;
            this.count = count;
            this.shapeless = shapeless;
            this.texture = texture;
            this.top = top;
            this.front = front;
            this.side = side;
            this.attach = attach;
        }

        public String type() { return type; }
        public String b64Image() { return b64Image; }
        public String json() { return json; }
        public String model() { return model; }
        public String kind() { return kind; }
        public String tool() { return tool; }
        public String hardness() { return hardness; }
        public String light() { return light; }
        public String sound() { return sound; }
        public Boolean flammable() { return flammable; }
        public Integer count() { return count; }
        public Boolean shapeless() { return shapeless; }
        public String texture() { return texture; }
        public String top() { return top; }
        public String front() { return front; }
        public String side() { return side; }
        public String attach() { return attach; }
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
