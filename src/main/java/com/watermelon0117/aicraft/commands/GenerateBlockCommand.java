package com.watermelon0117.aicraft.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.watermelon0117.aicraft.common.AICraftingTableCommonConfigs;
import com.watermelon0117.aicraft.common.BlockTextureStore;
import com.watermelon0117.aicraft.common.FileUtil;
import com.watermelon0117.aicraft.common.ItemStackArray;
import com.watermelon0117.aicraft.common.SpecialItemManager;
import com.watermelon0117.aicraft.gpt.GeneratedBlock;
import com.watermelon0117.aicraft.gpt.ItemIdSanitizer;
import com.watermelon0117.aicraft.gpt.ProxyBlockGenerator;
import com.watermelon0117.aicraft.init.BlockInit;
import com.watermelon0117.aicraft.network.CAddBlockTexturePacket;
import com.watermelon0117.aicraft.network.PacketHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

/** Until blocks can be crafted, this is the only way to get one: it is for checking how they look and behave in game. */
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

    private static final ProxyBlockGenerator GENERATOR = new ProxyBlockGenerator();
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
        GENERATOR.generate(id, new ItemStackArray().getDisplayNames(), player.getStringUUID())
                .whenComplete((block, error) -> player.getServer().execute(() -> {
                    IN_FLIGHT.remove(id);
                    if (error != null) {
                        fail(player, error);
                    } else {
                        apply(player, id, name.trim(), block);
                    }
                }));
        return 1;
    }

    private static void apply(ServerPlayer player, String id, String name, GeneratedBlock block) {
        try {
            BlockTextureStore.save(FileUtil.getBlockTextureFolder(), id, block.faces());
        } catch (IOException e) {
            fail(player, e);
            return;
        }
        PacketHandler.sendToAllClients(new CAddBlockTexturePacket(id, block.faces()));
        ItemStack stack = BlockInit.aiBlockItem(block.tool()).makeStack(id, name, block.hardness(), block.light());
        SpecialItemManager.get(player.level()).put(stack);
        ItemStack given = stack.copyWithCount(GIVEN_COUNT);
        if (!player.getInventory().add(given)) {
            player.drop(given, false);
        }
        player.sendSystemMessage(Component.literal("Generated " + id + " (" + block.tool().blockName() + ", " + block.hardness().getSerializedName() + ", " + block.light().getSerializedName() + ")"));
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
