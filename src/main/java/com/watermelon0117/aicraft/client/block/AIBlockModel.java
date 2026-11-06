package com.watermelon0117.aicraft.client.block;

import com.watermelon0117.aicraft.blockentities.AIBlockEntity;
import com.watermelon0117.aicraft.items.MainItem;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.client.model.IDynamicBakedModel;
import net.minecraftforge.client.model.data.ModelData;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A cube that gets its faces from {@link BlockTileAtlas}. It implements the model interface directly because
 * BakedModelWrapper's applyTransform returns the wrapped model, which would drop this one from item rendering.
 */
@OnlyIn(Dist.CLIENT)
public final class AIBlockModel implements IDynamicBakedModel {
    private static final ChunkRenderTypeSet SOLID = ChunkRenderTypeSet.of(RenderType.solid());

    private final Direction facing;
    private final ChunkRenderTypeSet renderTypes;
    private final ItemTransforms transforms;
    @Nullable
    private final String id;
    private final ItemOverrides overrides;

    private AIBlockModel(Direction facing, ChunkRenderTypeSet renderTypes, ItemTransforms transforms, @Nullable String id, ItemOverrides overrides) {
        this.facing = facing;
        this.renderTypes = renderTypes;
        this.transforms = transforms;
        this.id = id;
        this.overrides = overrides;
    }

    /** One block state: the id comes from the block entity, through model data, and the layer from the state's transparency. */
    public static AIBlockModel forState(Direction facing, ChunkRenderTypeSet renderTypes, ItemTransforms transforms) {
        return new AIBlockModel(facing, renderTypes, transforms, null, ItemOverrides.EMPTY);
    }

    /**
     * One item: swaps itself for a model of the id in the stack, with its front on the north like a furnace item.
     * Vanilla turns a furnace in first person so that its front shows, which the other transforms are for.
     */
    public static AIBlockModel forItem(ItemTransforms cubeTransforms, ItemTransforms frontTransforms) {
        return new AIBlockModel(Direction.NORTH, SOLID, cubeTransforms, null, new ByIdOverrides(cubeTransforms, frontTransforms));
    }

    @Override
    public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource rand,
                                             @NotNull ModelData data, @Nullable RenderType renderType) {
        if (side == null) {
            return List.of();
        }
        return BlockTileAtlas.quads(idFrom(data), facing, side);
    }

    @Nullable
    private String idFrom(ModelData data) {
        return id != null ? id : data.get(AIBlockEntity.ID_PROPERTY);
    }

    @Override
    public @NotNull ChunkRenderTypeSet getRenderTypes(@NotNull BlockState state, @NotNull RandomSource rand, @NotNull ModelData data) {
        return renderTypes;
    }

    /** An item has no state, so a partly clear one says so in its tag; the default sheet already cuts out clear pixels. */
    @Override
    public @NotNull List<RenderType> getRenderTypes(@NotNull ItemStack stack, boolean fabulous) {
        return SeeThroughItems.renderTypes(stack, fabulous, () -> IDynamicBakedModel.super.getRenderTypes(stack, fabulous));
    }

    @Override
    public TextureAtlasSprite getParticleIcon(@NotNull ModelData data) {
        return BlockTileAtlas.particleSprite(idFrom(data));
    }

    @Override
    public TextureAtlasSprite getParticleIcon() {
        return BlockTileAtlas.particleSprite(id);
    }

    @Override
    public ItemTransforms getTransforms() {
        return transforms;
    }

    @Override
    public ItemOverrides getOverrides() {
        return overrides;
    }

    @Override
    public boolean useAmbientOcclusion() {
        return true;
    }

    @Override
    public boolean isGui3d() {
        return true;
    }

    @Override
    public boolean usesBlockLight() {
        return true;
    }

    @Override
    public boolean isCustomRenderer() {
        return false;
    }

    private static final class ByIdOverrides extends ItemOverrides {
        private final ItemTransforms cubeTransforms;
        private final ItemTransforms frontTransforms;
        private final Map<String, AIBlockModel> cubes = new ConcurrentHashMap<>();
        private final Map<String, AIBlockModel> fronted = new ConcurrentHashMap<>();

        ByIdOverrides(ItemTransforms cubeTransforms, ItemTransforms frontTransforms) {
            this.cubeTransforms = cubeTransforms;
            this.frontTransforms = frontTransforms;
        }

        @Nullable
        @Override
        public BakedModel resolve(BakedModel model, ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed) {
            String stackId = MainItem.getID(stack);
            if (stackId == null) {
                return model;
            }
            // Asked every time, because an item can be on screen before its faces arrive.
            return BlockTileAtlas.hasFront(stackId)
                    ? fronted.computeIfAbsent(stackId, key -> new AIBlockModel(Direction.NORTH, SOLID, frontTransforms, key, ItemOverrides.EMPTY))
                    : cubes.computeIfAbsent(stackId, key -> new AIBlockModel(Direction.NORTH, SOLID, cubeTransforms, key, ItemOverrides.EMPTY));
        }
    }
}
