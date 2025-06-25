package com.watermelon0117.aicraft.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.watermelon0117.aicraft.recipes.SpecialItemManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class ListItemCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher){
        dispatcher.register(Commands.literal("listItem").executes(ListItemCommand::execute));
    }
    private static int execute(CommandContext<CommandSourceStack> command){
        if(command.getSource().getEntity() instanceof Player){
            Player player = (Player) command.getSource().getEntity();
            for(ItemStack stack: SpecialItemManager.getAllItems()){
                player.sendSystemMessage(Component.literal(stack.getHoverName().getString()+" "+
                        stack.getOrCreateTag().getAsString()));
            }
        }
        return Command.SINGLE_SUCCESS;
    }
}
