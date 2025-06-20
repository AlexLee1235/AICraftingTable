package com.watermelon0117.aicraft.event;

import com.watermelon0117.aicraft.AICraftingTable;
import com.watermelon0117.aicraft.RecipeManager;
import com.watermelon0117.aicraft.init.BlockInit;
import com.watermelon0117.aicraft.init.EntityInit;
import com.watermelon0117.aicraft.init.ItemInit;
import com.watermelon0117.aicraft.items.MainItem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Mod.EventBusSubscriber(modid = AICraftingTable.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class EventHandler {
    @SubscribeEvent
    public static void playerLoggedInEvent(PlayerEvent.PlayerLoggedInEvent event){

    }
    @SubscribeEvent
    public static void CommandEvent(CommandEvent event){
        System.out.println("CommandEvent");
    }
    @SubscribeEvent
    public static void EntityJoinLevelEvent(EntityJoinLevelEvent event){
        if(event.getEntity() instanceof Player) {
            System.out.println("EntityJoinLevelEvent");
            if (event.getEntity().level.isClientSide) {
                System.out.println("client");
                MainItem.renderer.loadFromFile();
                RecipeManager.loadFromFile();
            } else {
                System.out.println("server");
            }
        }
    }
}
