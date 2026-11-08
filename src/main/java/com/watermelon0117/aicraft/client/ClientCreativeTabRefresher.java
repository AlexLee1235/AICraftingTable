package com.watermelon0117.aicraft.client;

import com.watermelon0117.aicraft.init.CreativeModeTabInit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class ClientCreativeTabRefresher {
    private ClientCreativeTabRefresher() {
    }

    /** Puts the AI items as last synced into the creative tabs, and into the creative screen if it is open. */
    public static void refresh() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (minecraft.level == null || player == null) {
            return;
        }

        FeatureFlagSet features = player.connection.enabledFeatures();
        // What the creative screen asks for, as vanilla opens it with the operator items option. Asking for anything else
        // rebuilds every tab here and again in the screen, and shows the operator tab while the option is off.
        boolean operatorItemsTab = minecraft.options.operatorItemsTab().get();
        boolean hasPermissions = player.canUseGameMasterBlocks() && operatorItemsTab;
        HolderLookup.Provider holders = player.level().registryAccess();
        if (!CreativeModeTabs.tryRebuildTabContents(features, hasPermissions, holders)) {
            // The game rebuilds only when these three change, which a new AI item does not. The AI items are in this
            // mod's tab and in the search tab, which is built from every other tab, so only those two need building.
            CreativeModeTab.ItemDisplayParameters parameters = new CreativeModeTab.ItemDisplayParameters(features, hasPermissions, holders);
            CreativeModeTabInit.MAIN.get().buildContents(parameters);
            CreativeModeTabs.searchTab().buildContents(parameters);
        }

        if (minecraft.screen instanceof CreativeModeInventoryScreen) {
            minecraft.setScreen(new CreativeModeInventoryScreen(player, features, operatorItemsTab));
        }
    }
}
