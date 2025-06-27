package com.watermelon0117.aicraft.items;

import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class MainFoodItem extends MainItem {
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
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return renderer;
            }
        });
    }
}