package com.watermelon0117.aicraft.client;

import com.watermelon0117.aicraft.client.screen.AICraftingTableScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;

public class ClientHooks {
    public static void openAICraftingTableScreen(BlockPos pos){
        //Minecraft.getInstance().setScreen(new AICraftingTableScreen(pos));
    }
}
