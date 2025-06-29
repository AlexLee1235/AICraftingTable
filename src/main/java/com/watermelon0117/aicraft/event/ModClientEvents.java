package com.watermelon0117.aicraft.event;

import com.watermelon0117.aicraft.AICraftingTable;
import com.watermelon0117.aicraft.client.screen.AICraftingTableScreen;
import com.watermelon0117.aicraft.client.screen.MyCraftingScreen;
import com.watermelon0117.aicraft.init.BlockInit;
import com.watermelon0117.aicraft.init.EntityInit;
import com.watermelon0117.aicraft.init.MenuInit;
import com.watermelon0117.aicraft.menu.AICraftingTableMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.commands.arguments.NbtTagArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLDedicatedServerSetupEvent;
import org.jetbrains.annotations.Nullable;

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
    @SubscribeEvent
    public static void serverSetup(FMLDedicatedServerSetupEvent event){
        event.enqueueWork(()->{

        });
    }
}
