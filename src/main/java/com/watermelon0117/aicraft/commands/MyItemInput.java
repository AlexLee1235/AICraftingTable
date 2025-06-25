package com.watermelon0117.aicraft.commands;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.watermelon0117.aicraft.recipes.SpecialItemManager;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.function.Predicate;

public class MyItemInput implements Predicate<ItemStack> {
    private static final Dynamic2CommandExceptionType ERROR_STACK_TOO_BIG = new Dynamic2CommandExceptionType((p_120986_, p_120987_) -> {
        return Component.translatable("arguments.item.overstacked", p_120986_, p_120987_);
    });
    private final String name;

    public MyItemInput(String name) {
        this.name=name;
    }

    public boolean test(ItemStack p_120984_) {
        return true;//p_120984_.is(this.item) && NbtUtils.compareNbt(this.tag, p_120984_.getTag(), true);
    }
}
