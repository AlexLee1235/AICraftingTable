package com.watermelon0117.aicraft.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandExceptionType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.watermelon0117.aicraft.common.RecipeManager;
import com.watermelon0117.aicraft.common.SpecialItemManager;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.commands.GiveCommand;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class RemoveItemCommand {
    private static final SimpleCommandExceptionType ITEM_NOT_FOUND = new SimpleCommandExceptionType(
            Component.literal("Error: Item not found.")
    );
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context) {
        dispatcher.register(
                literal("aicraft").requires(cs -> cs.hasPermission(2)).then(
                        literal("removeItem").then(
                                argument("items", MyItemArgument.item())
                                        .executes(ctx -> {
                                            String selected = ctx.getArgument("items", String.class).replace('_', ' ');
                                            if(!SpecialItemManager.get().hasItem(selected)){
                                                throw ITEM_NOT_FOUND.create();
                                            }
                                            RecipeManager.get().removeItem(selected);
                                            SpecialItemManager.get().remove(selected);
                                            ctx.getSource().sendSuccess(Component.literal("You removed: " + selected), false);
                                            return 1;
                                        })
                        )
                )
        );
    }
}
