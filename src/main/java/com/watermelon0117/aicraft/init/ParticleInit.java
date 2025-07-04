package com.watermelon0117.aicraft.init;

import com.watermelon0117.aicraft.AICraftingTable;
import com.watermelon0117.aicraft.menu.AICraftingTableMenu;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ParticleInit {
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES = DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, AICraftingTable.MODID);
    public static final RegistryObject<SimpleParticleType> MAIN_FOOD_PARTICLE = PARTICLE_TYPES.register("main_food_particle",
            () -> new SimpleParticleType(true));
}
