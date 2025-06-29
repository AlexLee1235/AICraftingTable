package com.watermelon0117.aicraft.commands;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.watermelon0117.aicraft.SpecialItemManager;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.SharedSuggestionProvider;

import java.util.concurrent.CompletableFuture;

public class MyItemArgument implements ArgumentType<String> {
    private final CommandBuildContext context;

    public MyItemArgument(CommandBuildContext context) {
        this.context = context;
    }

    /** Factory method for registration */
    public static MyItemArgument item(CommandBuildContext context) {
        return new MyItemArgument(context);
    }

    @Override
    public String parse(StringReader reader) {
        return reader.readUnquotedString();
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> ctx, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(SpecialItemManager.getAllItemNames(), builder);
    }
}