package com.watermelon0117.aicraft.commands;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.arguments.item.ItemParser;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class MyItemArgument implements ArgumentType<MyItemInput> {
    private static final Collection<String> EXAMPLES = Arrays.asList("stick", "minecraft:stick", "stick{foo=bar}");

    public MyItemArgument(CommandBuildContext p_235278_) {
    }

    public static MyItemArgument item(CommandBuildContext p_235280_) {
        return new MyItemArgument(p_235280_);
    }

    public MyItemInput parse(StringReader p_120962_) throws CommandSyntaxException {
        return new MyItemInput(p_120962_.getString());
    }

    public static <S> MyItemInput getItem(CommandContext<S> p_120964_, String p_120965_) {
        return p_120964_.getArgument(p_120965_, MyItemInput.class);
    }

    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        return CompletableFuture.completedFuture(
                new Suggestions(new StringRange(11,12), List.of(
                        new Suggestion(new StringRange(12,15), "hi "))));
    }

    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}