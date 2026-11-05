package com.watermelon0117.aicraft.blocks;

import com.watermelon0117.aicraft.blockentities.AIBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.MapColor;
import org.jetbrains.annotations.Nullable;

/** A full opaque cube whose faces come from the id in its block entity. It is not a BaseEntityBlock, whose render shape is invisible. */
public class AIBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    private final AIBlockTool tool;

    public AIBlock(AIBlockTool tool) {
        super(propertiesOf(tool));
        this.tool = tool;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    /** Fixed for now, whatever the block looks like: the values of stone, oak planks, dirt, a hay bale and white wool. */
    private static BlockBehaviour.Properties propertiesOf(AIBlockTool tool) {
        BlockBehaviour.Properties properties = BlockBehaviour.Properties.of();
        return switch (tool) {
            case PICKAXE -> properties.mapColor(MapColor.STONE).requiresCorrectToolForDrops().strength(1.5F, 6.0F).sound(SoundType.STONE);
            case AXE -> properties.mapColor(MapColor.WOOD).strength(2.0F, 3.0F).sound(SoundType.WOOD);
            case SHOVEL -> properties.mapColor(MapColor.DIRT).strength(0.5F).sound(SoundType.GRAVEL);
            case HOE -> properties.mapColor(MapColor.COLOR_YELLOW).strength(0.5F).sound(SoundType.GRASS);
            case NONE -> properties.mapColor(MapColor.SNOW).strength(0.8F).sound(SoundType.WOOL);
        };
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AIBlockEntity(pos, state);
    }

    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        ItemStack stack = super.getCloneItemStack(level, pos, state);
        if (level.getBlockEntity(pos) instanceof AIBlockEntity blockEntity) {
            CompoundTag data = blockEntity.getAicraftTag();
            if (!data.isEmpty()) {
                stack.getOrCreateTag().put(AIBlockEntity.DATA_KEY, data.copy());
            }
        }
        return stack;
    }

    /** The burn odds vanilla's FireBlock gives oak planks, a hay bale and wool. */
    @Override
    public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return switch (tool) {
            case AXE, HOE -> 20;
            case NONE -> 60;
            default -> 0;
        };
    }

    /** The ignite odds vanilla's FireBlock gives oak planks, a hay bale and wool. */
    @Override
    public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return switch (tool) {
            case AXE -> 5;
            case HOE -> 60;
            case NONE -> 30;
            default -> 0;
        };
    }
}
