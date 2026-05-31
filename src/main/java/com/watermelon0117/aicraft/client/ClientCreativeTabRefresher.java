package com.watermelon0117.aicraft.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class ClientCreativeTabRefresher {
    private ClientCreativeTabRefresher() {
    }

    public static void refresh() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }

        boolean hasPermissions = minecraft.player.getPermissionLevel() >= 2;
        CreativeModeTabs.tryRebuildTabContents(
                minecraft.level.enabledFeatures(),
                hasPermissions,
                minecraft.level.registryAccess());

        if (minecraft.screen instanceof CreativeModeInventoryScreen) {
            minecraft.setScreen(new CreativeModeInventoryScreen(
                    minecraft.player,
                    minecraft.level.enabledFeatures(),
                    hasPermissions));
        }
    }
}
