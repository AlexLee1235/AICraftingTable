package com.watermelon0117.aicraft.client.armor;

import com.watermelon0117.aicraft.AICraftingTable;
import com.watermelon0117.aicraft.common.ArmorTexture;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;

/** Keeps the client-only textures out of the packet classes, which a dedicated server also loads. */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = AICraftingTable.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ClientArmorPackets {
    private ClientArmorPackets() {
    }

    public static void add(String id, ArmorTexture texture) {
        AIArmorTextures.put(id, texture);
    }

    public static void addAll(Map<String, ArmorTexture> textures) {
        textures.forEach(AIArmorTextures::put);
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        AIArmorTextures.reset();
    }
}
