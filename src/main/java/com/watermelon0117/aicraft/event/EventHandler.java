package com.watermelon0117.aicraft.event;

import com.watermelon0117.aicraft.AICraftingTable;
import com.watermelon0117.aicraft.commands.MyItemArgument;
import com.watermelon0117.aicraft.commands.RemoveItemCommand;
import com.watermelon0117.aicraft.common.*;
import com.watermelon0117.aicraft.gpt.*;
import com.watermelon0117.aicraft.network.CSendAllTexturePacket;
import com.watermelon0117.aicraft.network.CSyncRecipesPacket;
import com.watermelon0117.aicraft.network.CSyncSpecialItemsPacket;
import com.watermelon0117.aicraft.network.PacketHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

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
        if(event.getEntity() instanceof Player player) {
            if (!event.getEntity().level.isClientSide) {
                if (!AIChatClient.useOpenAI) {
                    ProxyChatClient.testConnect().exceptionally(err->{
                        player.sendSystemMessage(Component.literal("AICraftingTable: Unable to connect to AI server").withStyle(ChatFormatting.RED));
                        return null;
                    });
                }
            }
        }
    }
    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event){
        RemoveItemCommand.register(event.getDispatcher(), event.getBuildContext());
        ArgumentTypeInfos.registerByClass(MyItemArgument.class, SingletonArgumentInfo.contextAware(MyItemArgument::item));
    }
    @SubscribeEvent
    public static void onServerStart(ServerAboutToStartEvent e) {
        SpecialItemManager.ServerSide.init(e.getServer());
        RecipeManager.ServerSide.init(e.getServer());
        String key = AICraftingTableCommonConfigs.OPENAI_API_KEY.get();
        AIChatClient.useOpenAI = !key.isEmpty();
        AIImageClient.useOpenAI = !key.isEmpty();
        OpenAIChatClient.apiKey = key;
        OpenAIImageClient.apiKey = key;
    }


    /** Give joining player a copy of the list */
    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp) {
            PacketHandler.sendToPlayer(new CSyncSpecialItemsPacket(SpecialItemManager.ServerSide.data().data), sp);
            PacketHandler.sendToPlayer(new CSyncRecipesPacket(RecipeManager.ServerSide.data().recipes), sp);
            TextureManager.loadFromFileAsync().thenAccept(map-> PacketHandler.sendToPlayer(new CSendAllTexturePacket(map), sp));
        }
    }
}
