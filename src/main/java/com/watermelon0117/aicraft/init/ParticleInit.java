package com.watermelon0117.aicraft.init;

import com.mojang.serialization.Codec;
import com.watermelon0117.aicraft.AICraftingTable;
import com.watermelon0117.aicraft.menu.AICraftingTableMenu;
import com.watermelon0117.aicraft.particle.DynParticleOption;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ParticleInit {
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, AICraftingTable.MODID);

    public static final RegistryObject<ParticleType<DynParticleOption>> DYN =
            PARTICLE_TYPES.register("dyn", () ->
                    new ParticleType<DynParticleOption>(false,
                            DynParticleOption.DESERIALIZER) {
                        @Override
                        public Codec<DynParticleOption> codec() {
                            return DynParticleOption.codec(this);
                        }
                    });
}
