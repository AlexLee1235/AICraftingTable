package com.watermelon0117.aicraft.event;

import com.watermelon0117.aicraft.AICraftingTable;
import com.watermelon0117.aicraft.client.screen.AICraftingTableScreen;
import com.watermelon0117.aicraft.client.screen.MyCraftingScreen;
import com.watermelon0117.aicraft.common.EnglishNames;
import com.watermelon0117.aicraft.init.*;
import com.watermelon0117.aicraft.particle.DynFoodParticle;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.resources.language.ClientLanguage;
import net.minecraft.locale.Language;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import java.util.List;

@Mod.EventBusSubscriber(modid = AICraftingTable.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ModClientEvents {
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event){
/*PROGRAM INSERT POINT*/
    }
    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event){
        event.enqueueWork(()->{
            MenuScreens.register(MenuInit.MAIN_MENU.get(), AICraftingTableScreen::new);
            MenuScreens.register(MenuInit.MY_CRAFTING_MENU.get(), MyCraftingScreen::new);
        });
    }

    /** Reads the packs in English beside the player's own language, so recipe names sent to the AI stay English. */
    @SubscribeEvent
    public static void registerReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((ResourceManagerReloadListener) manager ->
                EnglishNames.setClient(ClientLanguage.loadFrom(manager, List.of(Language.DEFAULT), false)));
    }

    @SubscribeEvent
    public static void registerParticleProvidersEvent(RegisterParticleProvidersEvent event) {
        event.registerSpecial(ParticleInit.DYN_FOOD.get(), new DynFoodParticle.Provider());
    }
}
