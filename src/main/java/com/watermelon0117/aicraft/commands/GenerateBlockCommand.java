package com.watermelon0117.aicraft.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.watermelon0117.aicraft.common.AIBlockInstaller;
import com.watermelon0117.aicraft.common.AICraftingTableCommonConfigs;
import com.watermelon0117.aicraft.common.ItemStackArray;
import com.watermelon0117.aicraft.common.SpecialItemManager;
import com.watermelon0117.aicraft.gpt.GeneratedBlock;
import com.watermelon0117.aicraft.gpt.ItemIdSanitizer;
import com.watermelon0117.aicraft.gpt.ProxyItemGenerator;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.io.IOException;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

/** Gives a stack of a block from its name alone, with no recipe: it is for checking how blocks look and behave in game. */
public class GenerateBlockCommand {
    private static final int GIVEN_COUNT = 64;
    private static final SimpleCommandExceptionType NEEDS_TEXTURE_SERVER = new SimpleCommandExceptionType(
            Component.literal("AI blocks need the texture server, so they do not work with your own OpenAI key yet."));
    private static final SimpleCommandExceptionType BAD_NAME = new SimpleCommandExceptionType(
            Component.literal("The name needs at least one English letter or digit."));
    private static final SimpleCommandExceptionType ALREADY_EXISTS = new SimpleCommandExceptionType(
            Component.literal("That id already exists. Remove it with /aicraft removeItem first."));
    private static final SimpleCommandExceptionType IN_PROGRESS = new SimpleCommandExceptionType(
            Component.literal("That block is already being generated."));

    private static final ProxyItemGenerator GENERATOR = new ProxyItemGenerator();
    private static final Set<String> IN_FLIGHT = ConcurrentHashMap.newKeySet();

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                literal("aicraft").requires(source -> source.hasPermission(2)).then(
                        literal("generateBlock").then(
                                argument("name", StringArgumentType.greedyString())
                                        .executes(context -> generate(context.getSource(), StringArgumentType.getString(context, "name")))
                        )
                )
        );
    }

    private static int generate(CommandSourceStack source, String name) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (AICraftingTableCommonConfigs.useOpenAI) {
            throw NEEDS_TEXTURE_SERVER.create();
        }
        String id = ItemIdSanitizer.sanitize(name).trim();
        if (id.isEmpty()) {
            throw BAD_NAME.create();
        }
        if (SpecialItemManager.get(player.level()).hasItem(id)) {
            throw ALREADY_EXISTS.create();
        }
        if (!IN_FLIGHT.add(id)) {
            throw IN_PROGRESS.create();
        }
        source.sendSuccess(() -> Component.literal("Generating " + id + "..."), false);
        GENERATOR.generate(id, name.trim(), new ItemStackArray(), player.getStringUUID())
                .whenComplete((result, error) -> player.getServer().execute(() -> {
                    IN_FLIGHT.remove(id);
                    if (error != null) {
                        fail(player, error);
                    } else if (result instanceof GeneratedBlock block) {
                        apply(player, id, name.trim(), block);
                    } else {
                        // The server has already paid for the item's image by now.
                        fail(player, new IllegalArgumentException("The judge says this is not a block"));
                    }
                }));
        return 1;
    }

    private static void apply(ServerPlayer player, String id, String name, GeneratedBlock block) {
        ItemStack stack;
        try {
            stack = AIBlockInstaller.install(id, name, block);
        } catch (IOException e) {
            fail(player, e);
            return;
        }
        SpecialItemManager.get(player.level()).put(stack);
        ItemStack given = stack.copyWithCount(GIVEN_COUNT);
        if (!player.getInventory().add(given)) {
            player.drop(given, false);
        }
        player.sendSystemMessage(Component.literal("Generated " + id + " (" + block.shape().name().toLowerCase(Locale.ROOT) + ", " + block.tool().getSerializedName() + ", " + block.hardness().getSerializedName() + ", " + block.light().getSerializedName()
                + ", " + block.sound().getSerializedName() + (block.flammable() ? ", flammable" : "") + ")"));
    }

    private static void fail(ServerPlayer player, Throwable error) {
        error.printStackTrace();
        Throwable cause = error;
        while ((cause instanceof CompletionException || cause.getMessage() == null) && cause.getCause() != null) {
            cause = cause.getCause();
        }
        player.sendSystemMessage(Component.literal("Could not generate the block: " + cause.getMessage()).withStyle(ChatFormatting.RED));
    }
}
