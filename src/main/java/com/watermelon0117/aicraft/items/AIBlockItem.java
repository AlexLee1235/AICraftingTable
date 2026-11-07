package com.watermelon0117.aicraft.items;

import com.watermelon0117.aicraft.blockentities.AIBlockEntity;
import com.watermelon0117.aicraft.blocks.AIBlockHardness;
import com.watermelon0117.aicraft.blocks.AIBlockLight;
import com.watermelon0117.aicraft.blocks.AIBlockSound;
import com.watermelon0117.aicraft.blocks.AIBlockTool;
import com.watermelon0117.aicraft.blocks.AIBlockTransparency;
import com.watermelon0117.aicraft.blocks.AIPlacedBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class AIBlockItem extends BlockItem {
    public AIBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    public ItemStack makeStack(String id, String name, AIBlockTool tool, AIBlockHardness hardness, AIBlockLight light,
                               AIBlockSound sound, @Nullable String soundOf, boolean flammable, AIBlockTransparency transparency) {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", id);
        tag.putString("name", name);
        tag.putString(AIBlockEntity.TOOL_KEY, tool.getSerializedName());
        tag.putString(AIBlockEntity.HARDNESS_KEY, hardness.getSerializedName());
        tag.putString(AIPlacedBlock.LIGHT.getName(), light.getSerializedName());
        tag.putString(AIBlockEntity.SOUND_KEY, sound.getSerializedName());
        if (soundOf != null) {
            tag.putString(AIBlockEntity.SOUND_OF_KEY, soundOf);
        }
        tag.putBoolean(AIBlockEntity.FLAMMABLE_KEY, flammable);
        tag.putString(AIPlacedBlock.TRANSPARENCY.getName(), transparency.getSerializedName());
        ItemStack stack = new ItemStack(this);
        stack.getOrCreateTag().put(AIBlockEntity.DATA_KEY, tag);
        return stack;
    }

    @Override
    protected boolean updateCustomBlockEntityTag(BlockPos pos, Level level, @Nullable Player player, ItemStack stack, BlockState state) {
        boolean changed = super.updateCustomBlockEntityTag(pos, level, player, stack, state);
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(AIBlockEntity.DATA_KEY, Tag.TAG_COMPOUND)
                && level.getBlockEntity(pos) instanceof AIBlockEntity blockEntity) {
            blockEntity.setAicraftTag(tag.getCompound(AIBlockEntity.DATA_KEY));
            return true;
        }
        return changed;
    }

    @Override
    public Component getName(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        String name = tag == null ? "" : tag.getCompound(AIBlockEntity.DATA_KEY).getString("name");
        return name.isEmpty() ? super.getName(stack) : Component.literal(name);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        CompoundTag tag = stack.getTag();
        if (flag.isAdvanced() && tag != null) {
            tooltip.add(Component.literal(tag.getCompound(AIBlockEntity.DATA_KEY).toString()).withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
