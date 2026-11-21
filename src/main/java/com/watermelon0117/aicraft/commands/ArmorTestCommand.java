package com.watermelon0117.aicraft.commands;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.watermelon0117.aicraft.common.AIArmorInstaller;
import com.watermelon0117.aicraft.common.ArmorTexture;
import com.watermelon0117.aicraft.common.ArmorUV;
import com.watermelon0117.aicraft.common.RecipeManager;
import com.watermelon0117.aicraft.common.SpecialItemManager;
import com.watermelon0117.aicraft.gpt.GeneratedItem;
import com.watermelon0117.aicraft.gpt.ItemIdSanitizer;
import com.watermelon0117.aicraft.init.ItemInit;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

/**
 * For checking AI armor in game before the server can make it: installs pieces from files, as the armor test (test/armor_texture2)
 * wrote them. It exists only when the game is started with the environment variable AICRAFT_ARMOR_TEST (or -Daicraft.armorTest=true).
 * <p>
 * The manifest is a JSON array of {"name", "slot", "tier", "icon", "atlas", "uv"}: the piece's name, helmet/chestplate/leggings/boots,
 * the item step's tier (wooden, stone, iron, golden, diamond, netherite, bedrock), and paths to its 16x16 icon, worn picture and UV table.
 */
public final class ArmorTestCommand {
    private ArmorTestCommand() {
    }

    public static boolean enabled() {
        return System.getenv("AICRAFT_ARMOR_TEST") != null || Boolean.getBoolean("aicraft.armorTest");
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(literal("aicraft").requires(source -> source.hasPermission(2))
                .then(literal("armorTest").then(literal("import").then(argument("manifest", StringArgumentType.greedyString())
                        .executes(context -> importAll(context.getSource(), StringArgumentType.getString(context, "manifest")))))));
    }

    private static int importAll(CommandSourceStack source, String manifest) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        int done = 0;
        try {
            for (JsonElement element : JsonParser.parseString(Files.readString(Path.of(manifest.trim()), StandardCharsets.UTF_8)).getAsJsonArray()) {
                JsonObject piece = element.getAsJsonObject();
                String name = piece.get("name").getAsString();
                String id = ItemIdSanitizer.sanitize(name).trim();
                String tier = piece.get("tier").getAsString();
                try {
                    ItemStack stack = ItemInit.armorFor(ArmorUV.Slot.of(piece.get("slot").getAsString()))
                            .makeStack(id, name, GeneratedItem.tierByte(tier), "bedrock".equals(tier));
                    AIArmorInstaller.install(stack, Files.readAllBytes(Path.of(piece.get("icon").getAsString())),
                            new ArmorTexture(Files.readAllBytes(Path.of(piece.get("atlas").getAsString())),
                                    Files.readString(Path.of(piece.get("uv").getAsString()), StandardCharsets.UTF_8)));
                    SpecialItemManager.get(player.level()).put(stack);
                    RecipeManager.get().updateItem(stack);
                    if (!player.getInventory().add(stack.copy())) {
                        player.drop(stack.copy(), false);
                    }
                    done++;
                } catch (IOException | RuntimeException e) {
                    source.sendFailure(Component.literal("Could not import " + name + ": " + e.getMessage()));
                }
            }
        } catch (IOException | RuntimeException e) {
            source.sendFailure(Component.literal("Could not read " + manifest + ": " + e).withStyle(ChatFormatting.RED));
            return 0;
        }
        int imported = done;
        source.sendSuccess(() -> Component.literal("Imported " + imported + " armor pieces"), false);
        return imported;
    }
}
