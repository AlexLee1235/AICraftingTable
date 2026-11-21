package com.watermelon0117.aicraft.items;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.watermelon0117.aicraft.base.ModArmorMaterial;
import com.watermelon0117.aicraft.client.armor.AIArmorTextures;
import com.watermelon0117.aicraft.common.ArmorUV;
import net.minecraft.ChatFormatting;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.fml.DistExecutor;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * One of the four AI armor items, one per slot: vanilla draws worn armor only for an {@link ArmorItem} whose item-wide slot is the one
 * being drawn. The piece itself is the stack's {@code aicraft} tag, as for {@link MainItem}: {@code id}, {@code name}, {@code tier}
 * (vanilla's tool tier ordinal, see {@link AIArmorTier}) and {@code invincible}. Its worn picture is kept by id, like its icon.
 */
public class AIArmorItem extends ArmorItem {
    public static final String DATA_KEY = "aicraft";
    public static final String TIER_KEY = "tier";
    public static final String INVINCIBLE_KEY = "invincible";

    /** Iron's numbers, which only things that ask the item rather than the stack see (mobs choosing armor). Silent: the equip sound follows the tier, see ArmorEquipSounds. */
    public static final ArmorMaterial BASE = new ModArmorMaterial("ai", 15, new int[]{2, 5, 6, 2}, 9, SoundEvents.EMPTY,
            0.0F, 0.0F, () -> Ingredient.EMPTY);

    /** ArmorItem's own, so swapping an AI piece for a vanilla one replaces its modifiers. */
    private static final Map<Type, UUID> MODIFIER_UUIDS = new EnumMap<>(Map.of(
            Type.BOOTS, UUID.fromString("845DB27C-C624-495F-8C9F-6020A9A58B6B"),
            Type.LEGGINGS, UUID.fromString("D8499B04-0E66-4726-AB29-64469D734E0D"),
            Type.CHESTPLATE, UUID.fromString("9F3D476D-C118-4544-8365-64846904B48E"),
            Type.HELMET, UUID.fromString("2AD3F246-FEE1-4E67-B886-69FD380BB150")));

    public AIArmorItem(Type type, Properties properties) {
        super(BASE, type, properties);
    }

    public ArmorUV.Slot uvSlot() {
        return switch (type) {
            case HELMET -> ArmorUV.Slot.HELMET;
            case CHESTPLATE -> ArmorUV.Slot.CHESTPLATE;
            case LEGGINGS -> ArmorUV.Slot.LEGGINGS;
            case BOOTS -> ArmorUV.Slot.BOOTS;
        };
    }

    public ItemStack makeStack(String id, String name, byte toolTier, boolean invincible) {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", id);
        tag.putString("name", name);
        tag.putByte(TIER_KEY, toolTier);
        if (invincible) {
            tag.putBoolean(INVINCIBLE_KEY, true);
        }
        CompoundTag root = new CompoundTag();
        root.put(DATA_KEY, tag);
        ItemStack stack = new ItemStack(this);
        // setTag marks it undamaged, as saving and sending it do, so recipes match it the same before and after (see GeneratedItem).
        stack.setTag(root);
        return stack;
    }

    public static AIArmorTier tier(ItemStack stack) {
        CompoundTag tag = stack.getTagElement(DATA_KEY);
        return AIArmorTier.ofToolTier(tag == null || !tag.contains(TIER_KEY) ? -1 : tag.getByte(TIER_KEY));
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        if (slot != getEquipmentSlot()) {
            return ImmutableMultimap.of();
        }
        ArmorMaterial material = tier(stack).material();
        UUID uuid = MODIFIER_UUIDS.get(type);
        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
        builder.put(Attributes.ARMOR, new AttributeModifier(uuid, "Armor modifier", material.getDefenseForType(type), AttributeModifier.Operation.ADDITION));
        builder.put(Attributes.ARMOR_TOUGHNESS, new AttributeModifier(uuid, "Armor toughness", material.getToughness(), AttributeModifier.Operation.ADDITION));
        if (material.getKnockbackResistance() > 0) {
            builder.put(Attributes.KNOCKBACK_RESISTANCE, new AttributeModifier(uuid, "Armor knockback resistance", material.getKnockbackResistance(), AttributeModifier.Operation.ADDITION));
        }
        return builder.build();
    }

    @Override
    public int getMaxDamage(ItemStack stack) {
        return tier(stack).material().getDurabilityForType(type);
    }

    @Override
    public boolean isDamageable(ItemStack stack) {
        CompoundTag tag = stack.getTagElement(DATA_KEY);
        return (tag == null || !tag.getBoolean(INVINCIBLE_KEY)) && super.isDamageable(stack);
    }

    @Override
    public int getEnchantmentValue(ItemStack stack) {
        return tier(stack).material().getEnchantmentValue();
    }

    @Override
    public boolean makesPiglinsNeutral(ItemStack stack, LivingEntity wearer) {
        return tier(stack) == AIArmorTier.GOLD;
    }

    @Override
    public Component getName(ItemStack stack) {
        CompoundTag tag = stack.getTagElement(DATA_KEY);
        String name = tag == null ? "" : tag.getString("name");
        return name.isEmpty() ? super.getName(stack) : Component.literal(name);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        CompoundTag tag = stack.getTagElement(DATA_KEY);
        if (flag.isAdvanced() && tag != null) {
            tooltip.add(Component.literal(tag.toString()).withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    /** Only drawing asks for it, on the client. */
    @Override
    public @Nullable String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        return DistExecutor.unsafeCallWhenOn(Dist.CLIENT, () -> () -> AIArmorTextures.textureName(MainItem.getID(stack), uvSlot()));
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                // Read when drawn: MainItem makes it when it is constructed, and the order items are made in is not promised.
                return MainItem.renderer;
            }

            @Override
            public HumanoidModel<?> getHumanoidArmorModel(LivingEntity entity, ItemStack stack, EquipmentSlot slot, HumanoidModel<?> original) {
                return AIArmorTextures.model(MainItem.getID(stack), uvSlot());
            }
        });
    }
}
