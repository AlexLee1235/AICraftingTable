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
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.extensions.IForgeBlock;
import org.jetbrains.annotations.Nullable;

/**
 * What an AI block of any shape shares: its faces, tool, hardness, sound and flammability come from its block entity.
 * Light alone is a block state, because the server's light engine runs on a thread that cannot read block entities.
 * Methods here replace IForgeBlock's, which Block only inherits. Those Block declares itself cannot be replaced from an interface, so each shape forwards {@link #destroyProgress}.
 */
public interface AIPlacedBlock extends EntityBlock, IForgeBlock {
    EnumProperty<AIBlockLight> LIGHT = EnumProperty.create("light", AIBlockLight.class);
    /**
     * A state, like light, because occlusion, skylight and shading are cached per state and read where block entities
     * cannot be. Every shape but the plant has it; the plant is cut out already.
     */
    EnumProperty<AIBlockTransparency> TRANSPARENCY = EnumProperty.create("transparency", AIBlockTransparency.class);

    /** The predicates are vanilla's defaults, except that a see-through state is none of them, as with glass. */
    static BlockBehaviour.Properties properties() {
        return BlockBehaviour.Properties.of()
                // Only what vanilla sees where it asks a block for one number, which it only compares with 0 and -1: mining and explosions read the block entity below.
                .strength(AIBlockHardness.NORMAL.hardness(), AIBlockHardness.NORMAL.blastResistance())
                // Lava reads this flag alongside isFlammable, and nothing else reads it, so a block that is not flammable stays safe from lava.
                .ignitedByLava()
                .lightLevel(state -> state.getValue(LIGHT).level())
                .isRedstoneConductor((state, level, pos) -> !seeThrough(state) && state.isCollisionShapeFullBlock(level, pos))
                .isSuffocating((state, level, pos) -> !seeThrough(state) && state.blocksMotion() && state.isCollisionShapeFullBlock(level, pos))
                .isViewBlocking((state, level, pos) -> !seeThrough(state) && state.blocksMotion() && state.isCollisionShapeFullBlock(level, pos))
                .isValidSpawn((state, level, pos, type) -> !seeThrough(state) && state.isFaceSturdy(level, pos, Direction.UP)
                        && state.getLightEmission(level, pos) < 14);
    }

    /** Copies the light and transparency in the item's tag into the state being placed. */
    static BlockState withStateOfItem(BlockState state, BlockPlaceContext context) {
        CompoundTag data = context.getItemInHand().getTagElement(AIBlockEntity.DATA_KEY);
        if (data == null) {
            return state;
        }
        state = state.setValue(LIGHT, LIGHT.getValue(data.getString(LIGHT.getName())).orElse(AIBlockLight.NONE));
        return state.hasProperty(TRANSPARENCY)
                ? state.setValue(TRANSPARENCY, AIBlockTransparency.byName(data.getString(TRANSPARENCY.getName())))
                : state;
    }

    static boolean seeThrough(BlockState state) {
        return state.hasProperty(TRANSPARENCY) && state.getValue(TRANSPARENCY).seeThrough();
    }

    /** For the occlusion and visual shapes: neighbors draw their faces against a see-through block and light passes, as with glass. */
    static VoxelShape emptyIfSeeThrough(BlockState state, VoxelShape solid) {
        return seeThrough(state) ? Shapes.empty() : solid;
    }

    /** Water in a see-through block still dims the sky, as vanilla's own waterlogged blocks do. */
    static boolean propagatesSkylightDown(BlockState state, boolean solid) {
        return seeThrough(state) && state.getFluidState().isEmpty() || solid;
    }

    /**
     * A see-through block has no occlusion shape, so unlike glass it would draw its face against stone, where nothing
     * can see it. Glass hides that face with the full occlusion shape it keeps; this hides it against any solid block.
     */
    static boolean hiddenAgainst(BlockState state, BlockState adjacent) {
        return seeThrough(state) && adjacent.isSolidRender(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
    }

    /** Glass's own value: a see-through block does not darken the corners around it. */
    static float shadeBrightness(BlockState state, float solid) {
        return seeThrough(state) ? 1.0F : solid;
    }

    /**
     * Two see-through blocks of one id hide the faces where they meet, as glass does. A see-through block has no
     * occlusion shape, so this compares what vanilla's {@link net.minecraft.world.level.block.Block#shouldRenderFace}
     * compares, with the blocks' own shapes in its place.
     */
    @Override
    default boolean hidesNeighborFace(BlockGetter level, BlockPos pos, BlockState state, BlockState neighborState, Direction dir) {
        if (!seeThrough(state) || !seeThrough(neighborState)) {
            return false;
        }
        BlockPos neighborPos = pos.relative(dir);
        String id = idAt(level, pos);
        if (id == null || !id.equals(idAt(level, neighborPos))) {
            return false;
        }
        VoxelShape neighborFace = Shapes.getFaceShape(neighborState.getShape(level, neighborPos), dir.getOpposite());
        return !neighborFace.isEmpty()
                && !Shapes.joinIsNotEmpty(neighborFace, Shapes.getFaceShape(state.getShape(level, pos), dir), BooleanOp.ONLY_FIRST);
    }

    /** Called on chunk workers, so it must not create a block entity. */
    @Nullable
    private static String idAt(BlockGetter level, BlockPos pos) {
        return level.getExistingBlockEntity(pos) instanceof AIBlockEntity blockEntity ? blockEntity.getTextureId() : null;
    }

    /** With no block entity here, such as on a thread that may not read it, a block acts like the defaults of a tag with no values. */
    private static AIBlockTool toolAt(BlockGetter level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof AIBlockEntity blockEntity ? blockEntity.tool() : AIBlockTool.PICKAXE;
    }

    private static AIBlockHardness hardnessAt(BlockGetter level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof AIBlockEntity blockEntity ? blockEntity.hardness() : AIBlockHardness.NORMAL;
    }

    private static SoundType soundAt(BlockGetter level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof AIBlockEntity blockEntity ? blockEntity.soundType() : AIBlockSound.STONE.soundType();
    }

    /** A flammable block burns like oak planks. Fire must not remove an unbreakable one. */
    private static boolean burnsAt(BlockGetter level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof AIBlockEntity blockEntity
                && blockEntity.flammable() && blockEntity.hardness() != AIBlockHardness.UNBREAKABLE;
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
        return soundAt(level, pos);
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

    @Override
    default int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return burnsAt(level, pos) ? Blocks.OAK_PLANKS.defaultBlockState().getFlammability(level, pos, direction) : 0;
    }

    @Override
    default int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return burnsAt(level, pos) ? Blocks.OAK_PLANKS.defaultBlockState().getFireSpreadSpeed(level, pos, direction) : 0;
    }
}
