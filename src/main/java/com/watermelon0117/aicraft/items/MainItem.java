package com.watermelon0117.aicraft.items;

import com.watermelon0117.aicraft.client.renderer.MyBlockEntityWithoutLevelRenderer;
import com.watermelon0117.aicraft.gpt.OpenAIImageClient;
import com.watermelon0117.aicraft.gpt.OpenAIHttpClient;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class MainItem extends Item {
    public static MyBlockEntityWithoutLevelRenderer renderer = new MyBlockEntityWithoutLevelRenderer();
    OpenAIHttpClient client = new OpenAIHttpClient("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",
            "gpt-4o",
            1.0,
            1024,
            "You are MinecraftGPT, you answer question related to minecraft.");
    OpenAIImageClient imgClient = new OpenAIImageClient("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");
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
            /*GPTItemGenerator generator=new GPTItemGenerator();
            try {
                generator.generate(new String[]{"Iron Ingot", "Iron Ingot", "Iron Ingot",
                        "Iron Ingot", "Stick", "Iron Ingot",
                        "empty", "Stick", "empty"});
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }*/
            /*player.sendSystemMessage(Component.literal("Start..."));
            imgClient.generateAsync(
                            "A 16x16 pixel art depiction of a blueberry with clearly separated background color",
                            "1024x1024", "opaque", "low", "medium")
                    .thenAccept(bytes -> {
                        try {
                            Files.write(Path.of("C:\\achieve\\AICraftingTable\\process\\source.png"), bytes);
                            BufferedImage txt = ImageGridProcessor.process("C:\\achieve\\AICraftingTable\\process\\source.png");
                            ImageGridProcessor.saveImage(txt, "C:\\achieve\\AICraftingTable\\temp\\blueberry.png");
                            player.sendSystemMessage(Component.literal("Done"));
                            renderer.loadNewFile("blueberry");
                            ItemStack itemStack = new ItemStack(ItemInit.MAIN_ITEM.get());
                            itemStack.getOrCreateTag().putString("texture", "blueberry");
                            player.getInventory().add(itemStack);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .exceptionally(ex -> {
                        ex.printStackTrace();
                        return null;
                    });*/
            //BufferedImage texture = ImageGridProcessor.process("C:\\achieve\\AICraftingTable\\gpt\\12.png");
            //renderer.update("default", texture);
        }
        //ItemStack itemStack=player.getItemInHand(hand);
        //return InteractionResultHolder.success(itemStack);
        return super.use(level,player,hand);
    }

    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
        return state.is(BlockTags.MINEABLE_WITH_PICKAXE) && net.minecraftforge.common.TierSortingRegistry.isCorrectTierForDrops(Tiers.STONE, state);
    }
    @Override
    public float getDestroySpeed(ItemStack itemStack, BlockState state) {
        return state.is(BlockTags.MINEABLE_WITH_PICKAXE) ? Tiers.IRON.getSpeed() : 1.0F;
    }
    public boolean hurtEnemy(ItemStack p_40994_, LivingEntity p_40995_, LivingEntity p_40996_) {
        p_40994_.hurtAndBreak(2, p_40996_, (p_41007_) -> {
            p_41007_.broadcastBreakEvent(EquipmentSlot.MAINHAND);
        });
        return true;
    }
    public boolean mineBlock(ItemStack p_40998_, Level p_40999_, BlockState p_41000_, BlockPos p_41001_, LivingEntity p_41002_) {
        if (!p_40999_.isClientSide && p_41000_.getDestroySpeed(p_40999_, p_41001_) != 0.0F) {
            p_40998_.hurtAndBreak(1, p_41002_, (p_40992_) -> {
                p_40992_.broadcastBreakEvent(EquipmentSlot.MAINHAND);
            });
        }
        return true;
    }
    @Override
    public boolean isDamageable(ItemStack stack) {
        return true;
    }
    @Override
    public int getMaxDamage(ItemStack stack) {
        return 100;
    }

    @Override
    public boolean isEdible() {
        return true;
    }
    @Override
    public @Nullable FoodProperties getFoodProperties(ItemStack stack, @Nullable LivingEntity entity) {
        return (new FoodProperties.Builder()).nutrition(3).build();
    }

    @Override
    public Component getName(ItemStack itemStack) {
        CompoundTag tag = itemStack.getTag();
        String id = "Main Item";
        if (tag != null && !tag.getString("texture").contentEquals("")) {
            id = tag.getString("texture");
        }
        return Component.literal(id);
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