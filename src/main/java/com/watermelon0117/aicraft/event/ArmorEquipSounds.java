package com.watermelon0117.aicraft.event;

import com.watermelon0117.aicraft.AICraftingTable;
import com.watermelon0117.aicraft.items.AIArmorItem;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Plays an AI piece's equip sound, which follows its tier. Vanilla plays it in {@code LivingEntity.onEquipItem} from
 * {@code Equipable.getEquipSound()}, which asks the item and not the stack, so the four items are silent there (their material's sound
 * is {@code SoundEvents.EMPTY}) and the sound is played here instead, under onEquipItem's own conditions.
 * <p>
 * The change event comes at the entity's next update, and on its first update after it is loaded every piece it wears looks newly put on,
 * as vanilla's last-seen list starts empty. So each entity's pieces are noted when it joins a level, and a stack still in its slot since
 * then, damaged or not, makes no sound: only another stack being put there does.
 */
@Mod.EventBusSubscriber(modid = AICraftingTable.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ArmorEquipSounds {
    /** Server thread only. Each entity's armor stacks as last seen, by slot index (feet 0 to head 3). */
    private static final Map<LivingEntity, ItemStack[]> SEEN = new WeakHashMap<>();

    private ArmorEquipSounds() {
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof LivingEntity entity) {
            SEEN.put(entity, armorOf(entity));
        }
    }

    @SubscribeEvent
    public static void onChange(LivingEquipmentChangeEvent event) {
        LivingEntity entity = event.getEntity();
        EquipmentSlot slot = event.getSlot();
        if (entity.level().isClientSide() || slot.getType() != EquipmentSlot.Type.ARMOR) {
            return;
        }
        ItemStack[] seen = SEEN.get(entity);
        if (seen == null) {
            // Never seen joining, so there is nothing to tell a real change from the first update by.
            SEEN.put(entity, armorOf(entity));
            return;
        }
        ItemStack before = seen[slot.getIndex()];
        ItemStack now = event.getTo();
        seen[slot.getIndex()] = now;
        if (before == now) {
            return;
        }
        if (now.getItem() instanceof AIArmorItem armor && armor.getEquipmentSlot() == slot
                && !ItemStack.isSameItemSameTags(before, now) && !entity.isSpectator() && !entity.isSilent()) {
            entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(), AIArmorItem.tier(now).material().getEquipSound(),
                    entity.getSoundSource(), 1.0F, 1.0F);
        }
    }

    private static ItemStack[] armorOf(LivingEntity entity) {
        ItemStack[] stacks = new ItemStack[4];
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() == EquipmentSlot.Type.ARMOR) {
                stacks[slot.getIndex()] = entity.getItemBySlot(slot);
            }
        }
        return stacks;
    }
}
