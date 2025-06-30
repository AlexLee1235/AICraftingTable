package com.watermelon0117.aicraft.event;

import com.watermelon0117.aicraft.AICraftingTable;
import com.watermelon0117.aicraft.commands.ListItemCommand;
import com.watermelon0117.aicraft.commands.MyItemArgument;
import com.watermelon0117.aicraft.commands.RemoveItemCommand;
import com.watermelon0117.aicraft.common.RecipeManager;
import com.watermelon0117.aicraft.common.TextureManager;
import com.watermelon0117.aicraft.items.MainItem;
import com.watermelon0117.aicraft.common.SpecialItemManager;
import com.watermelon0117.aicraft.network.CSendAllTexturePacket;
import com.watermelon0117.aicraft.network.CSyncSpecialItemsPacket;
import com.watermelon0117.aicraft.network.PacketHandler;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
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
            } else {
                System.out.println("server");
                RecipeManager.loadFromFile();
            }
        }
    }
    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event){
        ListItemCommand.register(event.getDispatcher());
        RemoveItemCommand.register(event.getDispatcher(), event.getBuildContext());
        ArgumentTypeInfos.registerByClass(MyItemArgument.class, SingletonArgumentInfo.contextAware(MyItemArgument::item));
    }
    @SubscribeEvent
    public static void onServerStart(ServerAboutToStartEvent e) {
        SpecialItemManager.ServerSide.init(e.getServer());

    }

    /** Give joining player a copy of the list */
    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp) {
            PacketHandler.sendToPlayer(new CSyncSpecialItemsPacket(SpecialItemManager.ServerSide.data().data), sp);
            PacketHandler.sendToPlayer(new CSendAllTexturePacket(TextureManager.loadFromFile()), sp);
        }
    }
}
