package com.watermelon0117.aicraft.items;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.mojang.datafixers.util.Pair;
import com.watermelon0117.aicraft.client.renderer.MyBlockEntityWithoutLevelRenderer;
import com.watermelon0117.aicraft.init.ItemInit;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.common.ToolActions;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class MainItem extends Item {
    public static MyBlockEntityWithoutLevelRenderer renderer;
    public MainItem(Properties p_41383_) {
        super(p_41383_);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return super.use(level, player, hand);
    }
    public InteractionResult useOn(UseOnContext context) {
        CompoundTag tag = context.getItemInHand().getOrCreateTag().getCompound("aicraft");
        if (tag.getBoolean("isAxe"))
            return axeUseOn(context);
        if (tag.getBoolean("isShovel"))
            return shovelUseOn(context);
        if (tag.getBoolean("isHoe"))
            return hoeUseOn(context);
        return InteractionResult.PASS;
    }
    public boolean canPerformAction(ItemStack stack, net.minecraftforge.common.ToolAction toolAction) {
        return toolAction!= ToolActions.SWORD_SWEEP;
    }
    private static boolean isCorrectTool(ItemStack stack, BlockState state){
        CompoundTag tag = stack.getOrCreateTag().getCompound("aicraft");
        boolean ret = false;
        if (tag.getBoolean("isPickaxe"))
            ret = ret || state.is(BlockTags.MINEABLE_WITH_PICKAXE);
        if (tag.getBoolean("isAxe"))
            ret = ret || state.is(BlockTags.MINEABLE_WITH_AXE);
        if (tag.getBoolean("isShovel"))
            ret = ret || state.is(BlockTags.MINEABLE_WITH_SHOVEL);
        if (tag.getBoolean("isHoe"))
            ret = ret || state.is(BlockTags.MINEABLE_WITH_HOE);
        return ret;
    }
    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
        CompoundTag tag = stack.getOrCreateTag().getCompound("aicraft");
        return isCorrectTool(stack, state) && net.minecraftforge.common.TierSortingRegistry.isCorrectTierForDrops(Tiers.values()[tag.getByte("tier")], state);
    }
    @Override
    public float getDestroySpeed(ItemStack itemStack, BlockState state) {
        CompoundTag tag = itemStack.getOrCreateTag().getCompound("aicraft");
        return isCorrectTool(itemStack, state) ? Tiers.values()[tag.getByte("tier")].getSpeed() : 1.0F;
    }
    public boolean hurtEnemy(ItemStack p_40994_, LivingEntity p_40995_, LivingEntity p_40996_) {
        p_40994_.hurtAndBreak(2, p_40996_, (p_41007_) -> {
            p_41007_.broadcastBreakEvent(EquipmentSlot.MAINHAND);
        });
        return true;
    }
    public boolean mineBlock(ItemStack p_40998_, Level p_40999_, BlockState p_41000_, BlockPos p_41001_, LivingEntity p_41002_) {
        if (!p_40999_.isClientSide && p_41000_.getDestroySpeed(p_40999_, p_41001_) != 0.0F) {
            p_40998_.hurtAndBreak(1, p_41002_, (p_40992_) -> {
                p_40992_.broadcastBreakEvent(EquipmentSlot.MAINHAND);
            });
        }
        return true;
    }
    @Override
    public boolean isDamageable(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag().getCompound("aicraft");
        return (tag.getBoolean("isPickaxe") ||
                tag.getBoolean("isAxe") ||
                tag.getBoolean("isShovel")) ||
                tag.getBoolean("isHoe") ||
                tag.getBoolean("isMelee");
    }
    @Override
    public int getMaxStackSize(ItemStack stack) {
        return isDamageable(stack) ? 1 : 64;
    }
    @Override
    public int getMaxDamage(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag().getCompound("aicraft");
        return Tiers.values()[tag.getByte("tier")].getUses();
    }
    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot p_43274_, ItemStack stack) {
        if(!this.isDamageable(stack))
            return ImmutableMultimap.of();
        CompoundTag tag = stack.getOrCreateTag().getCompound("aicraft");
        double num1=tag.getDouble("attackDamage");
        double num2=tag.getDouble("attackSpeed");
        double attackDamage=num1 + Tiers.values()[tag.getByte("tier")].getAttackDamageBonus();
        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
        builder.put(Attributes.ATTACK_DAMAGE,
                new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Tool modifier",
                        attackDamage, AttributeModifier.Operation.ADDITION));
        builder.put(Attributes.ATTACK_SPEED,
                new AttributeModifier(BASE_ATTACK_SPEED_UUID, "Tool modifier",
                        num2, AttributeModifier.Operation.ADDITION));
        return p_43274_ == EquipmentSlot.MAINHAND ? builder.build() : ImmutableMultimap.of();
    }
    /*@Override
    public boolean isRepairable(ItemStack stack) {
        return this.isDamageable(stack);
    }
    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack material) {
        CompoundTag tag = stack.getOrCreateTag();
        return tag.getString("repairMaterial").contentEquals(strip(material.getDisplayName().getString()));
    }*/

    @Override
    public Component getName(ItemStack itemStack) {
        CompoundTag tag = itemStack.getOrCreateTag().getCompound("aicraft");
        String name = tag.getString("name");
        if (name.contentEquals("")) {
            name = "Main Item";
        }
        return Component.literal(name);
    }
    public static String getID(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !MainItem.isMainItem(stack))
            return null;
        CompoundTag tag = stack.getOrCreateTag().getCompound("aicraft");
        return tag.contains("id", Tag.TAG_STRING) ? tag.getString("id") : null;
    }
    public static boolean isMainItem(ItemStack a) {
        if (a == null) return false;
        return (a.is(ItemInit.MAIN_ITEM.get()) || a.is(ItemInit.MAIN_FOOD_ITEM.get()));
    }
    public static boolean isSameMainItem(ItemStack a, ItemStack b) {
        if (a == null || b == null) return false;
        return (a.is(ItemInit.MAIN_ITEM.get()) && b.is(ItemInit.MAIN_ITEM.get())) ||
                (a.is(ItemInit.MAIN_FOOD_ITEM.get()) && b.is(ItemInit.MAIN_FOOD_ITEM.get()));
    }
    @Override
    public Object getRenderPropertiesInternal() {
        return super.getRenderPropertiesInternal();
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        renderer=new MyBlockEntityWithoutLevelRenderer();
        consumer.accept(new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return renderer;
            }
        });
    }
    private InteractionResult axeUseOn(UseOnContext p_40529_){
        Level level = p_40529_.getLevel();
        BlockPos blockpos = p_40529_.getClickedPos();
        Player player = p_40529_.getPlayer();
        BlockState blockstate = level.getBlockState(blockpos);
        Optional<BlockState> optional = Optional.ofNullable(blockstate.getToolModifiedState(p_40529_, net.minecraftforge.common.ToolActions.AXE_STRIP, false));
        Optional<BlockState> optional1 = optional.isPresent() ? Optional.empty() : Optional.ofNullable(blockstate.getToolModifiedState(p_40529_, net.minecraftforge.common.ToolActions.AXE_SCRAPE, false));
        Optional<BlockState> optional2 = optional.isPresent() || optional1.isPresent() ? Optional.empty() : Optional.ofNullable(blockstate.getToolModifiedState(p_40529_, net.minecraftforge.common.ToolActions.AXE_WAX_OFF, false));
        ItemStack itemstack = p_40529_.getItemInHand();
        Optional<BlockState> optional3 = Optional.empty();
        if (optional.isPresent()) {
            level.playSound(player, blockpos, SoundEvents.AXE_STRIP, SoundSource.BLOCKS, 1.0F, 1.0F);
            optional3 = optional;
        } else if (optional1.isPresent()) {
            level.playSound(player, blockpos, SoundEvents.AXE_SCRAPE, SoundSource.BLOCKS, 1.0F, 1.0F);
            level.levelEvent(player, 3005, blockpos, 0);
            optional3 = optional1;
        } else if (optional2.isPresent()) {
            level.playSound(player, blockpos, SoundEvents.AXE_WAX_OFF, SoundSource.BLOCKS, 1.0F, 1.0F);
            level.levelEvent(player, 3004, blockpos, 0);
            optional3 = optional2;
        }

        if (optional3.isPresent()) {
            if (player instanceof ServerPlayer) {
                CriteriaTriggers.ITEM_USED_ON_BLOCK.trigger((ServerPlayer)player, blockpos, itemstack);
            }

            level.setBlock(blockpos, optional3.get(), 11);
            level.gameEvent(GameEvent.BLOCK_CHANGE, blockpos, GameEvent.Context.of(player, optional3.get()));
            if (player != null) {
                itemstack.hurtAndBreak(1, player, (p_150686_) -> {
                    p_150686_.broadcastBreakEvent(p_40529_.getHand());
                });
            }

            return InteractionResult.sidedSuccess(level.isClientSide);
        } else {
            return InteractionResult.PASS;
        }
    }
    private InteractionResult shovelUseOn(UseOnContext p_43119_) {
        Level level = p_43119_.getLevel();
        BlockPos blockpos = p_43119_.getClickedPos();
        BlockState blockstate = level.getBlockState(blockpos);
        if (p_43119_.getClickedFace() == Direction.DOWN) {
            return InteractionResult.PASS;
        } else {
            Player player = p_43119_.getPlayer();
            BlockState blockstate1 = blockstate.getToolModifiedState(p_43119_, net.minecraftforge.common.ToolActions.SHOVEL_FLATTEN, false);
            BlockState blockstate2 = null;
            if (blockstate1 != null && level.isEmptyBlock(blockpos.above())) {
                level.playSound(player, blockpos, SoundEvents.SHOVEL_FLATTEN, SoundSource.BLOCKS, 1.0F, 1.0F);
                blockstate2 = blockstate1;
            } else if (blockstate.getBlock() instanceof CampfireBlock && blockstate.getValue(CampfireBlock.LIT)) {
                if (!level.isClientSide()) {
                    level.levelEvent((Player)null, 1009, blockpos, 0);
                }

                CampfireBlock.dowse(p_43119_.getPlayer(), level, blockpos, blockstate);
                blockstate2 = blockstate.setValue(CampfireBlock.LIT, Boolean.valueOf(false));
            }

            if (blockstate2 != null) {
                if (!level.isClientSide) {
                    level.setBlock(blockpos, blockstate2, 11);
                    level.gameEvent(GameEvent.BLOCK_CHANGE, blockpos, GameEvent.Context.of(player, blockstate2));
                    if (player != null) {
                        p_43119_.getItemInHand().hurtAndBreak(1, player, (p_43122_) -> {
                            p_43122_.broadcastBreakEvent(p_43119_.getHand());
                        });
                    }
                }

                return InteractionResult.sidedSuccess(level.isClientSide);
            } else {
                return InteractionResult.PASS;
            }
        }
    }
    private InteractionResult hoeUseOn(UseOnContext p_41341_) {
        Level level = p_41341_.getLevel();
        BlockPos blockpos = p_41341_.getClickedPos();
        BlockState toolModifiedState = level.getBlockState(blockpos).getToolModifiedState(p_41341_, net.minecraftforge.common.ToolActions.HOE_TILL, false);
        Pair<Predicate<UseOnContext>, Consumer<UseOnContext>> pair = toolModifiedState == null ? null : Pair.of(ctx -> true, HoeItem.changeIntoState(toolModifiedState));
        if (pair == null) {
            return InteractionResult.PASS;
        } else {
            Predicate<UseOnContext> predicate = pair.getFirst();
            Consumer<UseOnContext> consumer = pair.getSecond();
            if (predicate.test(p_41341_)) {
                Player player = p_41341_.getPlayer();
                level.playSound(player, blockpos, SoundEvents.HOE_TILL, SoundSource.BLOCKS, 1.0F, 1.0F);
                if (!level.isClientSide) {
                    consumer.accept(p_41341_);
                    if (player != null) {
                        p_41341_.getItemInHand().hurtAndBreak(1, player, (p_150845_) -> {
                            p_150845_.broadcastBreakEvent(p_41341_.getHand());
                        });
                    }
                }

                return InteractionResult.sidedSuccess(level.isClientSide);
            } else {
                return InteractionResult.PASS;
            }
        }
    }
}