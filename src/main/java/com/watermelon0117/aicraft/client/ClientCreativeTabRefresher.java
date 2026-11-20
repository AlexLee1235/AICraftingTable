package com.watermelon0117.aicraft.client;

import com.watermelon0117.aicraft.client.screen.AICraftingTableScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class ClientCreativeTabRefresher {
    private ClientCreativeTabRefresher() {
    }

    /**
     * Shows the AI items as last synced in the creative screen or the AI table's recipe book, if one is open.
     * The AI tab needs no rebuilding here: 1.19.2 fills a tab again each time it is picked.
     */
    public static void refresh() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (minecraft.level == null || player == null) {
            return;
        }

        if (minecraft.screen instanceof CreativeModeInventoryScreen) {
            // Opens on the tab that was picked last, which fills it again.
            minecraft.setScreen(new CreativeModeInventoryScreen(player));
        }

        if (minecraft.screen instanceof AICraftingTableScreen screen) {
            screen.recipeBookComponent.itemsUpdated();
        }
    }
}
