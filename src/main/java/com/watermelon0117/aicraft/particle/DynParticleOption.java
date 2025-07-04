package com.watermelon0117.aicraft.particle;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.commands.arguments.item.ItemParser;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

public class DynParticleOption  implements ParticleOptions {
    public static final ParticleOptions.Deserializer<DynParticleOption> DESERIALIZER = new ParticleOptions.Deserializer<DynParticleOption>() {
        public DynParticleOption fromCommand(ParticleType<DynParticleOption> p_123721_, StringReader p_123722_) throws CommandSyntaxException {
            p_123722_.expect(' ');
            ItemParser.ItemResult itemparser$itemresult = ItemParser.parseForItem(HolderLookup.forRegistry(Registry.ITEM), p_123722_);
            ItemStack itemstack = (new ItemInput(itemparser$itemresult.item(), itemparser$itemresult.nbt())).createItemStack(1, false);
            return new DynParticleOption(p_123721_, itemstack);
        }

        public DynParticleOption fromNetwork(ParticleType<DynParticleOption> p_123724_, FriendlyByteBuf p_123725_) {
            return new DynParticleOption(p_123724_, p_123725_.readItem());
        }
    };
    public final ParticleType<DynParticleOption> type;
    private final ItemStack itemStack;

    public static Codec<DynParticleOption> codec(ParticleType<DynParticleOption> p_123711_) {
        return ItemStack.CODEC.xmap((p_123714_) -> {
            return new DynParticleOption(p_123711_, p_123714_);
        }, (p_123709_) -> {
            return p_123709_.itemStack;
        });
    }

    public DynParticleOption(ParticleType<DynParticleOption> p_123705_, ItemStack p_123706_) {
        this.type = p_123705_;
        this.itemStack = p_123706_.copy(); //Forge: Fix stack updating after the fact causing particle changes.
    }

    public void writeToNetwork(FriendlyByteBuf p_123716_) {
        p_123716_.writeItem(this.itemStack);
    }

    public String writeToString() {
        return Registry.PARTICLE_TYPE.getKey(this.getType()) + " " + (new ItemInput(this.itemStack.getItemHolder(), this.itemStack.getTag())).serialize();
    }

    public ParticleType<DynParticleOption> getType() {
        return this.type;
    }

    public ItemStack getItem() {
        return this.itemStack;
    }
}

