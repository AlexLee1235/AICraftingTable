package com.watermelon0117.aicraft.network;

import com.watermelon0117.aicraft.blockentities.AICraftingTableBlockEntity;
import com.watermelon0117.aicraft.common.BlockFaces;
import com.watermelon0117.aicraft.common.FileUtil;
import com.watermelon0117.aicraft.common.GeneratedInstaller;
import com.watermelon0117.aicraft.common.ShapedBlockRecipe;
import com.watermelon0117.aicraft.gpt.GeneratedBlock;
import com.watermelon0117.aicraft.gpt.GeneratedResult;
import com.watermelon0117.aicraft.gpt.GenerationException;
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
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Supplier;

public class SSelectIdeaPacket {
    private final BlockPos pos;
    private final String id;
    private final String name;
    private final ItemStack[] recipe;
    private final boolean override;
    /** A vanilla material's faces as the crafting client read them, or null. A 16x16 PNG is far smaller than the limit on each. */
    @Nullable
    private final BlockFaces vanillaFaces;
    private static final int MAX_FACE_BYTES = 16384;
    private static int incrementID = 0;

    public SSelectIdeaPacket(BlockPos pos, String id, String name, ItemStack[] recipe, boolean override, @Nullable BlockFaces vanillaFaces) {
        this.pos = pos;
        this.id = id;
        this.name = name;
        this.recipe = recipe;
        this.override = override;
        this.vanillaFaces = vanillaFaces;
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
        this.vanillaFaces = buf.readNullable(SSelectIdeaPacket::readFaces);
    }

    private static BlockFaces readFaces(FriendlyByteBuf buf) {
        return new BlockFaces(buf.readByteArray(MAX_FACE_BYTES), buf.readByteArray(MAX_FACE_BYTES), buf.readByteArray(MAX_FACE_BYTES));
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeUtf(id);
        buf.writeUtf(name);
        for (int i = 0; i < 9; i++)
            buf.writeItemStack(recipe[i], true);
        buf.writeBoolean(override);
        buf.writeNullable(vanillaFaces, CAddBlockTexturePacket::writeFaces);
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
                    player.sendSystemMessage(Component.translatable("error.aicraft.table_refused_id", id).withStyle(ChatFormatting.RED));
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
                    Optional<ShapedBlockRecipe> shaped = ShapedBlockRecipe.match(recipeSnapshot, id)
                            // Only clients have vanilla's pictures, so without one the texture server draws the block.
                            .filter(found -> found.vanillaBlock() == null || vanillaFaces != null);
                    if (shaped.isPresent()) {
                        // Vanilla's recipe for this shape out of a full block looks like that block, so the texture server is not asked.
                        makeShapedBlock(player, pos, id, name, recipeSnapshot, tId, shaped.get(), vanillaFaces);
                        return;
                    }
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

    private static void makeShapedBlock(ServerPlayer player, BlockPos pos, String id, String name, ItemStack[] recipe, int taskId,
                                        ShapedBlockRecipe shaped, @Nullable BlockFaces vanillaFaces) {
        GeneratedBlock block;
        try {
            block = shaped.block(FileUtil.getBlockTextureFolder(), vanillaFaces);
        } catch (IOException | RuntimeException e) {
            applyGeneratedFailure(player, pos, e);
            return;
        }
        applyGeneratedResult(player, pos, id, name, recipe, taskId, block);
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
        try {
            itemStack = GeneratedInstaller.install(id, name, generated);
        } catch (IOException | RuntimeException e) {
            applyGeneratedFailure(player, pos, e);
            return;
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
        sendErrToAll(player.level(), pos, GenerationException.describe(error));
    }

    private static void sendErrToAll(Level level, BlockPos pos, Component msg) {
        for (ServerPlayer player : ((ServerLevel) level).getPlayers(p -> true)) {
            player.sendSystemMessage(Component.translatable("error.aicraft.table_failed").withStyle(ChatFormatting.RED));
            player.sendSystemMessage(msg.copy().withStyle(ChatFormatting.RED));
            PacketHandler.sendToPlayer(new CGenerationFailedPacket(pos), player);
        }
    }
}
