package com.watermelon0117.aicraft.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.watermelon0117.aicraft.blockentities.AIBlockEntity;
import com.watermelon0117.aicraft.common.GeneratedInstaller;
import com.watermelon0117.aicraft.common.ItemStackArray;
import com.watermelon0117.aicraft.common.RecipeManager;
import com.watermelon0117.aicraft.common.SpecialItemManager;
import com.watermelon0117.aicraft.gpt.GeneratedBlock;
import com.watermelon0117.aicraft.gpt.GeneratedResult;
import com.watermelon0117.aicraft.gpt.GenerationException;
import com.watermelon0117.aicraft.gpt.ItemIdSanitizer;
import com.watermelon0117.aicraft.gpt.delegate.ItemGenerator;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.io.IOException;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

/** Gives a full stack of an item or a block from its name alone, with no recipe, regenerating the id if it exists: it is for checking how they look and behave in game. */
public class GenerateCommand {
    private static final SimpleCommandExceptionType BAD_NAME = new SimpleCommandExceptionType(
            Component.literal("The name needs at least one English letter or digit."));
    private static final SimpleCommandExceptionType IN_PROGRESS = new SimpleCommandExceptionType(
            Component.literal("That name is already being generated."));

    private static final ItemGenerator GENERATOR = new ItemGenerator();
    private static final Set<String> IN_FLIGHT = ConcurrentHashMap.newKeySet();

    private interface Request {
        CompletableFuture<GeneratedResult> send(String id, String name, String user);
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                literal("aicraft").requires(source -> source.hasPermission(2))
                        .then(literal("generateBlock").then(
                                argument("name", StringArgumentType.greedyString())
                                        .executes(context -> generateBlock(context.getSource(), StringArgumentType.getString(context, "name")))
                        ))
                        .then(literal("generateItem").then(
                                argument("name", StringArgumentType.greedyString())
                                        .executes(context -> generateItem(context.getSource(), StringArgumentType.getString(context, "name")))
                        ))
        );
    }

    private static int generateBlock(CommandSourceStack source, String name) throws CommandSyntaxException {
        return generate(source, name, (id, blockName, user) ->
                GENERATOR.generate(id, blockName, new ItemStackArray(), user).thenApply(GenerateCommand::requireBlock));
    }

    private static int generateItem(CommandSourceStack source, String name) throws CommandSyntaxException {
        return generate(source, name, (id, itemName, user) -> GENERATOR.generateItem(id, itemName, new ItemStackArray(), user));
    }

    private static GeneratedResult requireBlock(GeneratedResult result) {
        if (result instanceof GeneratedBlock) {
            return result;
        }
        // The item's image has already been paid for by now.
        throw new GenerationException("It came out as an item, not a block. Try a more block-like name, or use /aicraft generateItem.");
    }

    private static int generate(CommandSourceStack source, String name, Request request) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        String id = ItemIdSanitizer.sanitize(name).trim();
        if (id.isEmpty()) {
            throw BAD_NAME.create();
        }
        if (!IN_FLIGHT.add(id)) {
            throw IN_PROGRESS.create();
        }
        SpecialItemManager items = SpecialItemManager.get(player.level());
        boolean exists = items.hasItem(id);
        // The id is English, and an item from the table can have a name in the player's language. The table's regenerate button keeps it too.
        String displayName = exists ? items.getItem(id).getHoverName().getString() : name.trim();
        source.sendSuccess(() -> Component.literal((exists ? "Regenerating " : "Generating ") + id + "..."), false);
        request.send(id, displayName, player.getStringUUID())
                .whenComplete((result, error) -> player.getServer().execute(() -> {
                    IN_FLIGHT.remove(id);
                    if (error != null) {
                        fail(player, id, error);
                    } else {
                        apply(player, id, displayName, result);
                    }
                }));
        return 1;
    }

    private static void apply(ServerPlayer player, String id, String name, GeneratedResult generated) {
        ItemStack stack;
        try {
            stack = GeneratedInstaller.install(id, name, generated);
        } catch (IOException | RuntimeException e) {
            fail(player, id, e);
            return;
        }
        SpecialItemManager.get(player.level()).put(stack);
        RecipeManager.get().updateItem(stack);
        ItemStack given = stack.copyWithCount(stack.getMaxStackSize());
        if (!player.getInventory().add(given)) {
            player.drop(given, false);
        }
        player.sendSystemMessage(Component.literal("Generated " + id + describe(generated, stack)));
    }

    private static String describe(GeneratedResult generated, ItemStack stack) {
        if (generated instanceof GeneratedBlock block) {
            // The stack's own hardness, as the config can forbid the one the block asked for.
            String hardness = stack.getTagElement(AIBlockEntity.DATA_KEY).getString(AIBlockEntity.HARDNESS_KEY);
            return " (" + block.shape().name().toLowerCase(Locale.ROOT) + ", " + block.tool().getSerializedName() + ", " + hardness + ", " + block.light().getSerializedName()
                    + ", " + block.sound().getSerializedName() + (block.flammable() ? ", flammable" : "") + ")";
        }
        CompoundTag properties = stack.getTagElement("aicraft").copy();
        properties.remove("id");
        properties.remove("name");
        return properties.isEmpty() ? "" : " " + properties;
    }

    private static void fail(ServerPlayer player, String id, Throwable error) {
        error.printStackTrace();
        player.sendSystemMessage(Component.literal("Could not generate " + id + ": " + GenerationException.describe(error)).withStyle(ChatFormatting.RED));
    }
}
