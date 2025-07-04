package com.watermelon0117.aicraft.items;

import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class MainFoodItem extends MainItem {
    protected final RandomSource random = RandomSource.create();
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
        int fullDuration   = this.getUseDuration(stack);   // e.g. 32 ticks
        boolean nearFinish = count <= fullDuration - 7;    // last 7 ticks
        boolean shouldParticle = nearFinish && (count % 4 == 0);

        if (shouldParticle && player.level.isClientSide) {
            this.spawnItemParticles(player, stack, 5);
        }
        super.onUsingTick(stack, player, count);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack p_41409_, Level p_41410_, LivingEntity p_41411_) {
        this.spawnItemParticles(p_41411_, p_41409_, 16);
        return super.finishUsingItem(p_41409_, p_41410_, p_41411_);
    }
    private void spawnItemParticles(LivingEntity entity, ItemStack stack, int p_21062_) {
        for(int i = 0; i < p_21062_; ++i) {
            Vec3 vec3 = new Vec3(((double)random.nextFloat() - 0.5D) * 0.1D, Math.random() * 0.1D + 0.1D, 0.0D);
            vec3 = vec3.xRot(-entity.getXRot() * ((float)Math.PI / 180F));
            vec3 = vec3.yRot(-entity.getYRot() * ((float)Math.PI / 180F));
            double d0 = (double)(-random.nextFloat()) * 0.6D - 0.3D;
            Vec3 vec31 = new Vec3(((double)random.nextFloat() - 0.5D) * 0.3D, d0, 0.6D);
            vec31 = vec31.xRot(-entity.getXRot() * ((float)Math.PI / 180F));
            vec31 = vec31.yRot(-entity.getYRot() * ((float)Math.PI / 180F));
            vec31 = vec31.add(entity.getX(), entity.getEyeY(), entity.getZ());
            if (entity.level instanceof ServerLevel) //Forge: Fix MC-2518 spawnParticle is nooped on server, need to use server specific variant
                ((ServerLevel)entity.level).sendParticles(new ItemParticleOption(ParticleTypes.ITEM, stack), vec31.x, vec31.y, vec31.z, 1, vec3.x, vec3.y + 0.05D, vec3.z, 0.0D);
            else
                entity.level.addParticle(new ItemParticleOption(ParticleTypes.ITEM, Items.APPLE.getDefaultInstance()), vec31.x, vec31.y, vec31.z, vec3.x, vec3.y + 0.05D, vec3.z);
        }

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