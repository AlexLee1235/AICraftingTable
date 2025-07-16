package com.watermelon0117.aicraft.gpt;

import com.watermelon0117.aicraft.common.ItemStackArray;

import java.util.Base64;
import java.util.concurrent.CompletableFuture;

public class LocalItemGenerator {

    public CompletableFuture<GeneratedItem> generate(String id, String name, ItemStackArray recipe, String user) {
        ItemRequest request = new ItemRequest(recipe.getDisplayNames(), id, name);
        return client.send(request, user).thenApply(itemRespond -> {
            byte[] bytes = Base64.getDecoder().decode(itemRespond.b64Image);
            return GeneratedItem.fromJson(itemRespond.json, id, name, bytes);
        });
    }
    public record ItemRequest(String[] craftingGrid, String id, String name) {
    }
    public static class ItemRespond {
        String b64Image;
        String json;
        public ItemRespond(String b64Image, String json) {
            this.b64Image = b64Image;
            this.json = json;
        }
    }
}
