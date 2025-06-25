package com.watermelon0117.aicraft.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
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

public class RemoveItemCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context){
        dispatcher.register(Commands.literal("removeItem").requires((p_137777_) -> {
            return p_137777_.hasPermission(2);
        }).then(Commands.argument("item", MyItemArgument.item(context)).executes((p_137784_) -> {
            return execute(p_137784_.getSource(), MyItemArgument.getItem(p_137784_, "item"));
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
