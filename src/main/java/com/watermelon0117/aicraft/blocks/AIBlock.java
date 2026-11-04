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
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import org.jetbrains.annotations.Nullable;

/** A full opaque cube whose faces come from the id in its block entity. It is not a BaseEntityBlock, whose render shape is invisible. */
public class AIBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    private final AIBlockMaterial material;

    public AIBlock(AIBlockMaterial material) {
        super(propertiesOf(material));
        this.material = material;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    /** The values of the vanilla block each material stands for: stone, iron block, oak planks, dirt and white wool. */
    private static BlockBehaviour.Properties propertiesOf(AIBlockMaterial material) {
        BlockBehaviour.Properties properties = BlockBehaviour.Properties.of();
        return switch (material) {
            case STONE -> properties.mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM)
                    .requiresCorrectToolForDrops().strength(1.5F, 6.0F).sound(SoundType.STONE);
            case METAL -> properties.mapColor(MapColor.METAL).instrument(NoteBlockInstrument.IRON_XYLOPHONE)
                    .requiresCorrectToolForDrops().strength(5.0F, 6.0F).sound(SoundType.METAL);
            case WOOD -> properties.mapColor(MapColor.WOOD).instrument(NoteBlockInstrument.BASS)
                    .strength(2.0F, 3.0F).sound(SoundType.WOOD).ignitedByLava();
            case SOIL -> properties.mapColor(MapColor.DIRT).strength(0.5F).sound(SoundType.GRAVEL);
            case WOOL -> properties.mapColor(MapColor.SNOW).instrument(NoteBlockInstrument.GUITAR)
                    .strength(0.8F).sound(SoundType.WOOL).ignitedByLava();
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

    /** The burn odds vanilla's FireBlock gives oak planks and wool. */
    @Override
    public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return switch (material) {
            case WOOD -> 20;
            case WOOL -> 60;
            default -> 0;
        };
    }

    /** The ignite odds vanilla's FireBlock gives oak planks and wool. */
    @Override
    public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return switch (material) {
            case WOOD -> 5;
            case WOOL -> 30;
            default -> 0;
        };
    }
}
