package com.watermelon0117.aicraft.items;

import com.watermelon0117.aicraft.GPTImageClient;
import com.watermelon0117.aicraft.ImageGridProcessor;
import com.watermelon0117.aicraft.MyBlockEntityWithoutLevelRenderer;
import com.watermelon0117.aicraft.OpenAIHttpClient;
import com.watermelon0117.aicraft.init.ItemInit;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

public class MainItem extends Item {
    public static MyBlockEntityWithoutLevelRenderer renderer = new MyBlockEntityWithoutLevelRenderer();
    OpenAIHttpClient client = new OpenAIHttpClient("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",
            "gpt-4o",
            0.7,
            1024,
            "You are MinecraftGPT, you answer question related to minecraft.");
    GPTImageClient imgClient = new GPTImageClient("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");
    public MainItem(Properties p_41383_) {
        super(p_41383_);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide) {
            /*try {
                String reply = client.chat("Hi, what's your name?");
                player.sendSystemMessage(Component.literal(reply));
            } catch (IOException | InterruptedException e) {
                player.sendSystemMessage(Component.literal(e.getMessage()));
            }*/
            player.sendSystemMessage(Component.literal("Start..."));
            imgClient.generateAsync(
                            "A 16x16 pixel art depiction of a cup of coffee with clearly separated background color",
                            "1024x1024", "opaque", "low", "high")
                    .thenAccept(bytes -> {
                        try {
                            Files.write(Path.of("C:\\achieve\\AICraftingTable\\process\\source.png"), bytes);
                            BufferedImage txt = ImageGridProcessor.process("C:\\achieve\\AICraftingTable\\process\\source.png");
                            ImageGridProcessor.saveImage(txt, "C:\\achieve\\AICraftingTable\\temp\\coffee.png");
                            player.sendSystemMessage(Component.literal("Done"));
                            renderer.loadNewFile("coffee");
                            ItemStack itemStack = new ItemStack(ItemInit.MAIN_ITEM.get());
                            itemStack.getOrCreateTag().putString("texture", "coffee");
                            player.getInventory().add(itemStack);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .exceptionally(ex -> {
                        ex.printStackTrace();
                        return null;
                    });
            //BufferedImage texture = ImageGridProcessor.process("C:\\achieve\\AICraftingTable\\gpt\\11.png");
            //renderer.update("default", texture);

        }
        ItemStack itemStack=player.getItemInHand(hand);
        return InteractionResultHolder.success(itemStack);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return renderer;
            }
        });
    }
}