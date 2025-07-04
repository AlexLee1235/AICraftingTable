package com.watermelon0117.aicraft.items;

import com.watermelon0117.aicraft.init.ParticleInit;
import com.watermelon0117.aicraft.particle.DynParticle;
import com.watermelon0117.aicraft.particle.DynParticleOption;
import net.minecraft.client.particle.BreakingItemParticle;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.Nullable;

import java.util.Random;
import java.util.function.Consumer;

public class MainFoodItem extends MainItem {
    protected static final RandomSource random = RandomSource.create();
    public MainFoodItem(Properties p_41383_) {
        super(p_41383_);
    }

    @Override
    public boolean isEdible() {
        return true;
    }
    @Override
    public @Nullable FoodProperties getFoodProperties(ItemStack stack, @Nullable LivingEntity entity) {
        CompoundTag tag = stack.getOrCreateTag().getCompound("aicraft");
        return (new FoodProperties.Builder())
                .nutrition(tag.getByte("nutrition"))
                .build();
    }
    public UseAnim getUseAnimation(ItemStack p_41358_) {
        CompoundTag tag = p_41358_.getOrCreateTag().getCompound("aicraft");
        if(tag.getBoolean("isFood"))
            return UseAnim.EAT;
        if(tag.getBoolean("isDrink"))
            return UseAnim.DRINK;
        return super.getUseAnimation(p_41358_);
    }

    @Override
    public void onUsingTick(ItemStack stack, LivingEntity player, int count) {
        CompoundTag tag = stack.getOrCreateTag().getCompound("aicraft");
        if(tag.getBoolean("isFood")) {
            int fullDuration = this.getUseDuration(stack);   // e.g. 32 ticks
            boolean nearFinish = count <= fullDuration - 7;    // last 7 ticks
            boolean shouldParticle = nearFinish && (count % 4 == 0);

            if (shouldParticle && player.level.isClientSide) {
                spawnDynParticles(player, stack, 5);
            }
        }
        super.onUsingTick(stack, player, count);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack p_41409_, Level p_41410_, LivingEntity p_41411_) {
        CompoundTag tag = p_41409_.getOrCreateTag().getCompound("aicraft");
        if(tag.getBoolean("isFood"))
            spawnDynParticles(p_41411_, p_41409_, 16);
        return super.finishUsingItem(p_41409_, p_41410_, p_41411_);
    }
    public static void spawnDynParticles(LivingEntity ent, ItemStack stack, int count) {
        for(int i = 0; i < count; ++i) {
            Vec3 vec3 = new Vec3(((double)random.nextFloat() - 0.5D) * 0.1D, Math.random() * 0.1D + 0.1D, 0.0D);
            vec3 = vec3.xRot(-ent.getXRot() * ((float)Math.PI / 180F));
            vec3 = vec3.yRot(-ent.getYRot() * ((float)Math.PI / 180F));
            double d0 = (double)(-random.nextFloat()) * 0.6D - 0.3D;
            Vec3 vec31 = new Vec3(((double)random.nextFloat() - 0.5D) * 0.3D, d0, 0.6D);
            vec31 = vec31.xRot(-ent.getXRot() * ((float)Math.PI / 180F));
            vec31 = vec31.yRot(-ent.getYRot() * ((float)Math.PI / 180F));
            vec31 = vec31.add(ent.getX(), ent.getEyeY(), ent.getZ());
            if (ent.level instanceof ServerLevel) //Forge: Fix MC-2518 spawnParticle is nooped on server, need to use server specific variant
                ((ServerLevel)ent.level).sendParticles(new DynParticleOption(ParticleInit.DYN.get(), stack), vec31.x, vec31.y, vec31.z, 1, vec3.x, vec3.y + 0.05D, vec3.z, 0.0D);
            else
                ent.level.addParticle(new DynParticleOption(ParticleInit.DYN.get(), stack), vec31.x, vec31.y, vec31.z, vec3.x, vec3.y + 0.05D, vec3.z);
        }
    }
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return super.use(level, player, hand);
    }
    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return renderer;
            }
        });
    }
}