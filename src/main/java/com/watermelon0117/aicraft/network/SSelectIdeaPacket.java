package com.watermelon0117.aicraft.network;

import com.watermelon0117.aicraft.blockentities.AICraftingTableBlockEntity;
import com.watermelon0117.aicraft.common.AIBlockInstaller;
import com.watermelon0117.aicraft.gpt.GeneratedBlock;
import com.watermelon0117.aicraft.gpt.GeneratedItem;
import com.watermelon0117.aicraft.common.TextureManager;
import com.watermelon0117.aicraft.gpt.GeneratedResult;
import com.watermelon0117.aicraft.gpt.ItemIdSanitizer;
import com.watermelon0117.aicraft.gpt.delegate.ItemGenerator;
import com.watermelon0117.aicraft.common.ItemStackArray;
import com.watermelon0117.aicraft.common.RecipeManager;
import com.watermelon0117.aicraft.common.SpecialItemManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.io.IOException;
import java.util.Arrays;
import java.util.function.Supplier;

public class SSelectIdeaPacket {
    private final BlockPos pos;
    private final String id;
    private final String name;
    private final ItemStack[] recipe;
    private final boolean override;
    private static int incrementID = 0;

    public SSelectIdeaPacket(BlockPos pos, String id, String name, ItemStack[] recipe, boolean override) {
        this.pos = pos;
        this.id = id;
        this.name = name;
        this.recipe = recipe;
        this.override = override;
    }

    public SSelectIdeaPacket(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.id = buf.readUtf();
        this.name = buf.readUtf();
        ItemStack[] recipe = new ItemStack[9];
        for (int i = 0; i < 9; i++)
            recipe[i] = buf.readItem();
        this.recipe = recipe;
        this.override = buf.readBoolean();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeUtf(id);
        buf.writeUtf(name);
        for (int i = 0; i < 9; i++)
            buf.writeItemStack(recipe[i], true);
        buf.writeBoolean(override);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        ItemGenerator generator = new ItemGenerator();
        ServerPlayer player = contextSupplier.get().getSender();
        if (player != null && !player.level().isClientSide) {
            BlockEntity blockEntity = player.level().getBlockEntity(pos);
            if (blockEntity instanceof AICraftingTableBlockEntity be) {
                boolean exists = SpecialItemManager.get(player.level()).hasItem(id);
                // Ids from before that rule were already written once, so only a new id has to pass it.
                if (!exists && !ItemIdSanitizer.isClean(id)) {
                    // A new id becomes a file name. The idea step cleans every id, so only a changed client gets here.
                    player.sendSystemMessage(Component.literal("The AI crafting table refused the id " + id).withStyle(ChatFormatting.RED));
                    return;
                }
                if (exists && !override) {  //use exist item
                    ItemStack stack = SpecialItemManager.get(player.level()).getItem(id).copyWithCount(RecipeManager.get().craftedCount(id));
                    RecipeManager.get().putRecipe(stack, recipe, RecipeManager.get().itemIsShapeless(stack));
                    be.getInventory().setStackInSlot(0, stack);
                    be.setProgress(580);
                    player.level().sendBlockUpdated(pos, player.level().getBlockState(pos), player.level().getBlockState(pos), Block.UPDATE_ALL);
                } else {
                    be.setProgress(10);  //show a pixel first
                    int tId = incrementID++;
                    ItemStack[] recipeSnapshot = Arrays.copyOf(recipe, recipe.length);
                    be.taskID = tId;
                    player.level().sendBlockUpdated(pos, player.level().getBlockState(pos), player.level().getBlockState(pos), Block.UPDATE_ALL);
                    generator.generate(id, name, new ItemStackArray(recipeSnapshot), player.getStringUUID()).thenAccept(generated -> {
                        if (player.getServer() == null) {
                            return;
                        }
                        player.getServer().execute(() -> applyGeneratedResult(player, pos, id, name, recipeSnapshot, tId, generated));
                    }).exceptionally(ex -> {
                        if (player.getServer() != null) {
                            player.getServer().execute(() -> applyGeneratedFailure(player, pos, ex));
                        } else {
                            ex.printStackTrace();
                        }
                        return null;
                    });
                }
            }
        }
    }

    private static void applyGeneratedResult(ServerPlayer player, BlockPos pos, String id, String name, ItemStack[] recipe, int taskId, GeneratedResult generated) {
        if (player.level().isClientSide) {
            return;
        }
        BlockEntity blockEntity = player.level().getBlockEntity(pos);
        if (!(blockEntity instanceof AICraftingTableBlockEntity be)) {
            return;
        }
        if (be.taskID != taskId || be.getProgress() == 0) {
            System.out.println("Canceled, not putting image");
            return;
        }

        ItemStack itemStack;
        if (generated instanceof GeneratedBlock block) {
            try {
                itemStack = AIBlockInstaller.install(id, name, block);
            } catch (IOException e) {
                applyGeneratedFailure(player, pos, e);
                return;
            }
        } else {
            GeneratedItem generatedItem = (GeneratedItem) generated;
            itemStack = generatedItem.itemStack();
            byte[] processedTexture = TextureManager.applyTexture(generatedItem.rawTexture(), id);
            PacketHandler.sendToAllClients(new CAddTexturePacket(id, processedTexture));
        }
        // The list of AI items holds one of each, as the creative tab refuses any other count; the recipe holds what one craft makes.
        SpecialItemManager.get(player.level()).put(itemStack);
        ItemStack crafted = itemStack.copyWithCount(generated.count());
        RecipeManager.get().putRecipe(crafted, recipe, generated.shapeless());
        be.getInventory().setStackInSlot(0, crafted);
        be.setProgress(580);
        player.level().sendBlockUpdated(pos, player.level().getBlockState(pos), player.level().getBlockState(pos), Block.UPDATE_ALL);
    }

    private static void applyGeneratedFailure(ServerPlayer player, BlockPos pos, Throwable error) {
        error.printStackTrace();
        if (player.level().isClientSide) {
            return;
        }
        BlockEntity blockEntity = player.level().getBlockEntity(pos);
        if (blockEntity instanceof AICraftingTableBlockEntity be) {
            be.setProgress(0);
        }
        sendErrToAll(player.level(), unwrapMessage(error));
    }

    private static String unwrapMessage(Throwable error) {
        Throwable current = error;
        while (current != null) {
            if (current.getMessage() != null && !current.getMessage().isBlank()) {
                return current.getMessage();
            }
            current = current.getCause();
        }
        return "Unknown error";
    }

    private static void sendErrToAll(Level level, String msg) {
        for (ServerPlayer player : ((ServerLevel) level).getPlayers(p -> true)) {
            player.sendSystemMessage(Component.literal("An error occurred when using the AI crafting table.").withStyle(ChatFormatting.RED));
            player.sendSystemMessage(Component.literal(msg).withStyle(ChatFormatting.RED));
        }
    }
}
