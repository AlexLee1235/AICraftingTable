package com.watermelon0117.aicraft.event;

import com.watermelon0117.aicraft.AICraftingTable;
import com.watermelon0117.aicraft.commands.RemoveItemCommand;
import com.watermelon0117.aicraft.common.AICraftingTableCommonConfigs;
import com.watermelon0117.aicraft.common.RecipeManager;
import com.watermelon0117.aicraft.common.SpecialItemManager;
import com.watermelon0117.aicraft.common.TextureManager;
import com.watermelon0117.aicraft.gpt.opanai.OpenAIChatClient;
import com.watermelon0117.aicraft.gpt.opanai.OpenAIImageClient;
import com.watermelon0117.aicraft.network.CSendAllTexturePacket;
import com.watermelon0117.aicraft.network.CSyncRecipesPacket;
import com.watermelon0117.aicraft.network.CSyncSpecialItemsPacket;
import com.watermelon0117.aicraft.network.PacketHandler;
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
            if (!event.getEntity().level().isClientSide) {
                if (!AICraftingTableCommonConfigs.useOpenAI) {
                    // todo: test connect server
                }
            }
        }
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event){
        RemoveItemCommand.register(event.getDispatcher(), event.getBuildContext());
    }

    @SubscribeEvent
    public static void onServerStart(ServerAboutToStartEvent e) {
        SpecialItemManager.ServerSide.init(e.getServer());
        RecipeManager.ServerSide.init(e.getServer());
        String key = AICraftingTableCommonConfigs.OPENAI_API_KEY.get();
        AICraftingTableCommonConfigs.useOpenAI = !key.isEmpty();
        if(!key.isEmpty()) {
            OpenAIChatClient.apiKey = key;
            OpenAIImageClient.apiKey = key;
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp) {
            PacketHandler.sendToPlayer(new CSyncSpecialItemsPacket(SpecialItemManager.ServerSide.data().data), sp);
            PacketHandler.sendToPlayer(new CSyncRecipesPacket(RecipeManager.ServerSide.data().recipes), sp);
            TextureManager.loadFromFileAsync().thenAccept(map-> PacketHandler.sendToPlayer(new CSendAllTexturePacket(map), sp));
        }
    }
}
