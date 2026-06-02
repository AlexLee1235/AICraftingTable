package com.watermelon0117.aicraft.commands;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.watermelon0117.aicraft.common.SpecialItemManager;
import com.watermelon0117.aicraft.items.MainItem;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.concurrent.CompletableFuture;

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
        List<ItemStack> items;
        if (ctx.getSource() instanceof CommandSourceStack source) {
            items = SpecialItemManager.get(source.getLevel()).list();
        } else {
            items = SpecialItemManager.ClientSide.list();
        }
        return SharedSuggestionProvider.suggest(listEscapedNames(items), builder);
    }

    private static List<String> listEscapedNames(List<ItemStack> items) {
        // Immutable list produced by the stream pipeline
        return items.stream()
                .map(MainItem::getID)         // Component
                .map(name -> name.replace(' ', '_'))
                .distinct()
                .sorted()
                .toList();
    }
}
