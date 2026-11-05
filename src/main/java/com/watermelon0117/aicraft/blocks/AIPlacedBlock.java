package com.watermelon0117.aicraft.blocks;

import com.watermelon0117.aicraft.blockentities.AIBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.extensions.IForgeBlock;
import org.jetbrains.annotations.Nullable;

/**
 * What an AI block of any shape shares: its faces, tool and hardness come from its block entity.
 * Light alone is a block state, because the server's light engine runs on a thread that cannot read block entities.
 * Methods here replace IForgeBlock's, which Block only inherits. Those Block declares itself cannot be replaced from an interface, so each shape forwards {@link #destroyProgress}.
 */
public interface AIPlacedBlock extends EntityBlock, IForgeBlock {
    EnumProperty<AIBlockLight> LIGHT = EnumProperty.create("light", AIBlockLight.class);

    static BlockBehaviour.Properties properties() {
        return BlockBehaviour.Properties.of()
                // Only what vanilla sees where it asks a block for one number, which it only compares with 0 and -1: mining and explosions read the block entity below.
                .strength(AIBlockHardness.NORMAL.hardness(), AIBlockHardness.NORMAL.blastResistance())
                .lightLevel(state -> state.getValue(LIGHT).level());
    }

    static BlockState withLightOfItem(BlockState state, BlockPlaceContext context) {
        CompoundTag data = context.getItemInHand().getTagElement(AIBlockEntity.DATA_KEY);
        return data == null ? state : state.setValue(LIGHT, LIGHT.getValue(data.getString(LIGHT.getName())).orElse(AIBlockLight.NONE));
    }

    /** With no block entity here, such as on a thread that may not read it, a block acts like the defaults of a tag with no values. */
    private static AIBlockTool toolAt(BlockGetter level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof AIBlockEntity blockEntity ? blockEntity.tool() : AIBlockTool.PICKAXE;
    }

    private static AIBlockHardness hardnessAt(BlockGetter level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof AIBlockEntity blockEntity ? blockEntity.hardness() : AIBlockHardness.NORMAL;
    }

    /** Vanilla's formula, asked of the tool's stand-in, with this block's hardness. */
    static float destroyProgress(Player player, BlockGetter level, BlockPos pos) {
        AIBlockHardness hardness = hardnessAt(level, pos);
        if (hardness == AIBlockHardness.UNBREAKABLE) {
            return 0.0F;
        }
        BlockState standIn = toolAt(level, pos).standIn();
        int divisor = ForgeHooks.isCorrectToolForDrops(standIn, player) ? 30 : 100;
        return player.getDigSpeed(standIn, pos) / hardness.hardness() / divisor;
    }

    @Nullable
    @Override
    default BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AIBlockEntity(pos, state);
    }

    @Override
    default ItemStack getCloneItemStack(BlockState state, HitResult target, BlockGetter level, BlockPos pos, Player player) {
        ItemStack stack = IForgeBlock.super.getCloneItemStack(state, target, level, pos, player);
        if (level.getBlockEntity(pos) instanceof AIBlockEntity blockEntity) {
            CompoundTag data = blockEntity.getAicraftTag();
            if (!data.isEmpty()) {
                stack.getOrCreateTag().put(AIBlockEntity.DATA_KEY, data.copy());
            }
        }
        return stack;
    }

    @Override
    default boolean canHarvestBlock(BlockState state, BlockGetter level, BlockPos pos, Player player) {
        return ForgeHooks.isCorrectToolForDrops(toolAt(level, pos).standIn(), player);
    }

    @Override
    default SoundType getSoundType(BlockState state, LevelReader level, BlockPos pos, @Nullable Entity entity) {
        return toolAt(level, pos).standIn().getSoundType();
    }

    @Override
    default MapColor getMapColor(BlockState state, BlockGetter level, BlockPos pos, MapColor defaultColor) {
        return toolAt(level, pos).standIn().getMapColor(level, pos);
    }

    @Override
    default float getExplosionResistance(BlockState state, BlockGetter level, BlockPos pos, Explosion explosion) {
        return hardnessAt(level, pos).blastResistance();
    }

    /** Bedrock keeps out withers and the dragon with tags, which cannot differ between block entities of one block. */
    @Override
    default boolean canEntityDestroy(BlockState state, BlockGetter level, BlockPos pos, Entity entity) {
        return hardnessAt(level, pos) != AIBlockHardness.UNBREAKABLE && IForgeBlock.super.canEntityDestroy(state, level, pos, entity);
    }

    /** Fire must not remove an unbreakable block. */
    @Override
    default int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return hardnessAt(level, pos) == AIBlockHardness.UNBREAKABLE ? 0 : toolAt(level, pos).standIn().getFlammability(level, pos, direction);
    }

    @Override
    default int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return hardnessAt(level, pos) == AIBlockHardness.UNBREAKABLE ? 0 : toolAt(level, pos).standIn().getFireSpreadSpeed(level, pos, direction);
    }
}
