package com.watermelon0117.aicraft.init;

import com.mojang.serialization.Codec;
import com.watermelon0117.aicraft.AICraftingTable;
import com.watermelon0117.aicraft.particle.DynFoodParticleOption;
import net.minecraft.core.particles.ParticleType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ParticleInit {
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, AICraftingTable.MODID);

    public static final RegistryObject<ParticleType<DynFoodParticleOption>> DYN_FOOD =
            PARTICLE_TYPES.register("dyn_food", () ->
                    new ParticleType<>(false, DynFoodParticleOption.DESERIALIZER) {
                        @Override
                        public Codec<DynFoodParticleOption> codec() {
                            return DynFoodParticleOption.codec(this);
                        }
                    });
}
