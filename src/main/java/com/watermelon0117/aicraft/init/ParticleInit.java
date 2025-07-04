package com.watermelon0117.aicraft.init;

import com.mojang.serialization.Codec;
import com.watermelon0117.aicraft.AICraftingTable;
import com.watermelon0117.aicraft.particle.DynParticleOption;
import net.minecraft.core.particles.ParticleType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ParticleInit {
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, AICraftingTable.MODID);

    public static final RegistryObject<ParticleType<DynParticleOption>> DYN =
            PARTICLE_TYPES.register("dyn", () ->
                    new ParticleType<>(false,
                            DynParticleOption.DESERIALIZER) {
                        @Override
                        public Codec<DynParticleOption> codec() {
                            return DynParticleOption.codec(this);
                        }
                    });
}
