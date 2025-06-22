package com.watermelon0117.aicraft.event;

import com.watermelon0117.aicraft.AICraftingTable;
import com.watermelon0117.aicraft.recipes.RecipeManager;
import com.watermelon0117.aicraft.items.MainItem;
import com.watermelon0117.aicraft.recipes.SpecialItemManager;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AICraftingTable.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class EventHandler {
    @SubscribeEvent
    public static void playerLoggedInEvent(PlayerEvent.PlayerLoggedInEvent event){

    }
    @SubscribeEvent
    public static void CommandEvent(CommandEvent event){
    }
    @SubscribeEvent
    public static void EntityJoinLevelEvent(EntityJoinLevelEvent event){
        if(event.getEntity() instanceof Player) {
            System.out.println("EntityJoinLevelEvent");
            if (event.getEntity().level.isClientSide) {
                System.out.println("client");
                MainItem.renderer.loadFromFile();
                RecipeManager.loadFromFile();
                SpecialItemManager.loadFromFile();
            } else {
                System.out.println("server");
            }
        }
    }
}
