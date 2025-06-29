package com.watermelon0117.aicraft.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.watermelon0117.aicraft.recipes.RecipeManager;
import com.watermelon0117.aicraft.recipes.SpecialItemManager;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.commands.GiveCommand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;

import static net.minecraft.commands.Commands.literal;
import static net.minecraft.commands.Commands.argument;

public class RemoveItemCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context) {
        dispatcher.register(
                literal("removeItem")
                        .then(argument("items", MyItemArgument.item(context))
                                .executes(ctx -> {
                                    String selected = ctx.getArgument("items", String.class).replace('_', ' ');
                                    RecipeManager.removeItem(selected);
                                    SpecialItemManager.removeItem(selected);
                                    ctx.getSource().sendSuccess(Component.literal("You chose: " + selected), false);
                                    return 1;
                                })));
    }
    private static int execute(CommandSourceStack command, MyItemInput myItemInput){
        if(command.getEntity() instanceof Player){
            Player player = (Player) command.getEntity();
            //SpecialItemManager.removeItem("");
        }
        return Command.SINGLE_SUCCESS;
    }
}
