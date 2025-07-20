package com.watermelon0117.aicraft.commands;

import com.google.gson.JsonObject;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.watermelon0117.aicraft.common.SpecialItemManager;
import com.watermelon0117.aicraft.items.MainItem;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.server.command.EnumArgument;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class MyItemArgument implements ArgumentType<String> {

    public MyItemArgument() {
    }

    /** Factory method for registration */
    public static MyItemArgument item() {
        return new MyItemArgument();
    }

    @Override
    public String parse(StringReader reader) {
        return reader.readUnquotedString();
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> ctx, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(listEscapedNames(), builder);
    }
    public static List<String> listEscapedNames() {
        SpecialItemManager mgr = SpecialItemManager.get();
        // Immutable list produced by the stream pipeline
        return mgr.list().stream()
                .map(MainItem::getID)         // Component
                .map(name -> name.replace(' ', '_'))
                .toList();
    }
}