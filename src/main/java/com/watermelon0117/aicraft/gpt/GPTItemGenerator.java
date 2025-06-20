package com.watermelon0117.aicraft.gpt;

import com.watermelon0117.aicraft.ImageGridProcessor;
import com.watermelon0117.aicraft.blockentities.AICraftingTableBlockEntity;
import com.watermelon0117.aicraft.init.ItemInit;
import com.watermelon0117.aicraft.items.MainItem;
import com.watermelon0117.aicraft.recipes.RecipeManager;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

public class GPTItemGenerator {
    private final OpenAIHttpClient client = new OpenAIHttpClient("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",
            "gpt-4o",  //gpt-4o gpt-4.1
            0.0,
            1024,
            "",
            "text");;
    private final GPTImageGenerator imgClient = new GPTImageGenerator();
    private static String buildPrompt(String name, String[] recipe){
        return "";
    }
    private static void applyTexture(byte[] bytes, String name) {
        try {
            Files.write(Path.of("C:\\achieve\\AICraftingTable\\process\\source.png"), bytes);
            Files.write(Path.of("C:\\achieve\\AICraftingTable\\image\\" + name + ".png"), bytes);
            BufferedImage txt = ImageGridProcessor.process("C:\\achieve\\AICraftingTable\\process\\source.png");
            ImageGridProcessor.saveImage(txt, "C:\\achieve\\AICraftingTable\\temp\\" + name + ".png");
            MainItem.renderer.loadNewFile(name);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public CompletableFuture<ItemStack> generate(String name, String[] recipe, AICraftingTableBlockEntity be, Predicate<AICraftingTableBlockEntity> predicate) {
        return client.chat(buildPrompt(name, recipe)).thenCompose(rawJson -> {
            ItemStack itemStack = new ItemStack(ItemInit.MAIN_ITEM.get());
            itemStack.getOrCreateTag().putString("texture", name);

            // do something to itemStack based on rawJson
            return imgClient.generateItem(name, recipe/*prompt from json*/).thenApply(textureBytes -> {
                if (predicate.test(be))
                    applyTexture(textureBytes, name);
                return itemStack;
            });
        });
    }
}
