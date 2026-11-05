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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.common.ForgeHooks;
import org.jetbrains.annotations.Nullable;

/**
 * A full opaque cube whose faces come from the id in its block entity. It is not a BaseEntityBlock, whose render shape is invisible.
 * Hardness and light are block states, because the server's light engine runs on a thread that cannot read block entities.
 */
public class AIBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<AIBlockHardness> HARDNESS = EnumProperty.create("hardness", AIBlockHardness.class);
    public static final EnumProperty<AIBlockLight> LIGHT = EnumProperty.create("light", AIBlockLight.class);

    private final AIBlockTool tool;

    public AIBlock(AIBlockTool tool) {
        super(propertiesOf(tool));
        this.tool = tool;
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(HARDNESS, AIBlockHardness.NORMAL)
                .setValue(LIGHT, AIBlockLight.NONE));
    }

    /**
     * Color and sound are fixed for now, whatever the block looks like: those of stone, oak planks, dirt, a hay bale and white wool.
     * The strength here is only what vanilla sees where it asks a block for one number, which it only compares with 0 and -1:
     * mining and explosions read the state below.
     */
    private static BlockBehaviour.Properties propertiesOf(AIBlockTool tool) {
        BlockBehaviour.Properties properties = BlockBehaviour.Properties.of()
                .strength(AIBlockHardness.NORMAL.hardness(), AIBlockHardness.NORMAL.blastResistance())
                .lightLevel(state -> state.getValue(LIGHT).level());
        return switch (tool) {
            case PICKAXE -> properties.mapColor(MapColor.STONE).requiresCorrectToolForDrops().sound(SoundType.STONE);
            case AXE -> properties.mapColor(MapColor.WOOD).sound(SoundType.WOOD);
            case SHOVEL -> properties.mapColor(MapColor.DIRT).sound(SoundType.GRAVEL);
            case HOE -> properties.mapColor(MapColor.COLOR_YELLOW).sound(SoundType.GRASS);
            case NONE -> properties.mapColor(MapColor.SNOW).sound(SoundType.WOOL);
        };
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, HARDNESS, LIGHT);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        CompoundTag data = context.getItemInHand().getTagElement(AIBlockEntity.DATA_KEY);
        BlockState state = defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
        if (data == null) {
            return state;
        }
        return state
                .setValue(HARDNESS, HARDNESS.getValue(data.getString(HARDNESS.getName())).orElse(AIBlockHardness.NORMAL))
                .setValue(LIGHT, LIGHT.getValue(data.getString(LIGHT.getName())).orElse(AIBlockLight.NONE));
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

    /** Vanilla's formula, which only knows the one hardness a block is registered with. */
    @Override
    public float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        AIBlockHardness hardness = state.getValue(HARDNESS);
        if (hardness == AIBlockHardness.UNBREAKABLE) {
            return 0.0F;
        }
        int divisor = ForgeHooks.isCorrectToolForDrops(state, player) ? 30 : 100;
        return player.getDigSpeed(state, pos) / hardness.hardness() / divisor;
    }

    @Override
    public float getExplosionResistance(BlockState state, BlockGetter level, BlockPos pos, Explosion explosion) {
        return state.getValue(HARDNESS).blastResistance();
    }

    /** Bedrock keeps out withers and the dragon with tags, which cannot differ between states of one block. */
    @Override
    public boolean canEntityDestroy(BlockState state, BlockGetter level, BlockPos pos, Entity entity) {
        return state.getValue(HARDNESS) != AIBlockHardness.UNBREAKABLE && super.canEntityDestroy(state, level, pos, entity);
    }

    /** The burn odds vanilla's FireBlock gives oak planks, a hay bale and wool. Fire must not remove an unbreakable block either. */
    @Override
    public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        if (state.getValue(HARDNESS) == AIBlockHardness.UNBREAKABLE) {
            return 0;
        }
        return switch (tool) {
            case AXE, HOE -> 20;
            case NONE -> 60;
            default -> 0;
        };
    }

    /** The ignite odds vanilla's FireBlock gives oak planks, a hay bale and wool. */
    @Override
    public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        if (state.getValue(HARDNESS) == AIBlockHardness.UNBREAKABLE) {
            return 0;
        }
        return switch (tool) {
            case AXE -> 5;
            case HOE -> 60;
            case NONE -> 30;
            default -> 0;
        };
    }
}
