package com.watermelon0117.aicraft.blocks;

import com.watermelon0117.aicraft.blockentities.AIBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A plant whose outline is where its picture is drawn. Vanilla caches only collision and occlusion shapes, both empty
 * here through {@code noCollission}, and asks for the outline each time, so it can come from the block entity.
 */
public class AICrossBlock extends BushBlock implements AIPlacedBlock {
    /** A state, because whether it can stay where it is must be known before its block entity exists. */
    public static final EnumProperty<AIBlockAttach> ATTACH = EnumProperty.create("attach", AIBlockAttach.class);
    /** A sapling's, for a plant whose tag holds no bounds, such as one made before plants had them. */
    private static final VoxelShape SHAPE = Block.box(2.0D, 0.0D, 2.0D, 14.0D, 12.0D, 14.0D);

    public AICrossBlock() {
        super(AIPlacedBlock.properties().noCollission().pushReaction(PushReaction.DESTROY));
        registerDefaultState(defaultBlockState().setValue(LIGHT, AIBlockLight.NONE).setValue(ATTACH, AIBlockAttach.BELOW));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIGHT, ATTACH);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = AIPlacedBlock.withStateOfItem(defaultBlockState(), context);
        CompoundTag data = context.getItemInHand().getTagElement(AIBlockEntity.DATA_KEY);
        return data == null ? state : state.setValue(ATTACH, ATTACH.getValue(data.getString(ATTACH.getName())).orElse(AIBlockAttach.BELOW));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        // Never creates a block entity, as a caller may be on a thread that must not.
        VoxelShape outline = level.getExistingBlockEntity(pos) instanceof AIBlockEntity blockEntity ? blockEntity.outline() : null;
        return outline != null ? outline : SHAPE;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return switch (state.getValue(ATTACH)) {
            case BELOW -> super.canSurvive(state, level, pos);
            case ABOVE -> Block.canSupportCenter(level, pos.above(), Direction.DOWN);
            case ANY -> true;
        };
    }

    /**
     * Lets a lantern hang under a chain; nothing collides with it. Vanilla caches the answer per state, asked with no
     * block entity, so it sees {@link #SHAPE}. That answers as the real outline would: what an {@code any} cross
     * draws is always centered and on the bottom row, so its underside always covers the middle.
     */
    @Override
    public VoxelShape getBlockSupportShape(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getValue(ATTACH) == AIBlockAttach.ANY
                ? getShape(state, level, pos, CollisionContext.empty())
                : super.getBlockSupportShape(state, level, pos);
    }

    /** An AI plant may be from the Nether or made of crystal, so dirt alone would leave it with nowhere to grow and no hint why. */
    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return super.mayPlaceOn(state, level, pos) || state.isFaceSturdy(level, pos, Direction.UP);
    }

    @Override
    public float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        return AIPlacedBlock.destroyProgress(player, level, pos);
    }
}
