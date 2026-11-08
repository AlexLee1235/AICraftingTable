package com.watermelon0117.aicraft.client.block;

import com.watermelon0117.aicraft.blockentities.AIBlockEntity;
import com.watermelon0117.aicraft.blocks.AIBlockTransparency;
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

/** The shape of a vanilla model, showing one tile of {@link BlockTileAtlas}. Implements the model interface directly for the same reason as {@link AIBlockModel}. */
@OnlyIn(Dist.CLIENT)
public final class AIShapedBlockModel implements IDynamicBakedModel {
    private final ShapeTemplate template;
    private final ChunkRenderTypeSet renderTypes;
    private final ItemTransforms transforms;
    @Nullable
    private final String id;
    private final ItemOverrides overrides;

    private AIShapedBlockModel(ShapeTemplate template, ChunkRenderTypeSet renderTypes, ItemTransforms transforms, @Nullable String id, ItemOverrides overrides) {
        this.template = template;
        this.renderTypes = renderTypes;
        this.transforms = transforms;
        this.id = id;
        this.overrides = overrides;
    }

    /** One block state: the id comes from the block entity, through model data. */
    public static AIShapedBlockModel forState(ShapeTemplate template, ChunkRenderTypeSet renderTypes, ItemTransforms transforms) {
        return new AIShapedBlockModel(template, renderTypes, transforms, null, ItemOverrides.EMPTY);
    }

    /** One item: swaps itself for a model of the id in the stack. */
    public static AIShapedBlockModel forItem(ShapeTemplate template, ShapeTemplate seeThroughTemplate, ChunkRenderTypeSet renderTypes, ItemTransforms transforms) {
        return new AIShapedBlockModel(template, renderTypes, transforms, null, new ByIdOverrides(template, seeThroughTemplate, renderTypes, transforms));
    }

    @Override
    public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource rand,
                                             @NotNull ModelData data, @Nullable RenderType renderType) {
        return BlockTileAtlas.shapeQuads(template, idFrom(data), side);
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
        return template.ambientOcclusion();
    }

    @Override
    public boolean isGui3d() {
        return template.gui3d();
    }

    @Override
    public boolean usesBlockLight() {
        return template.blockLight();
    }

    @Override
    public boolean isCustomRenderer() {
        return false;
    }

    private static final class ByIdOverrides extends ItemOverrides {
        private final ShapeTemplate template;
        private final ShapeTemplate seeThroughTemplate;
        private final ChunkRenderTypeSet renderTypes;
        private final ItemTransforms transforms;
        private final Map<String, AIShapedBlockModel> models = new ConcurrentHashMap<>();
        private final Map<String, AIShapedBlockModel> seeThroughModels = new ConcurrentHashMap<>();

        ByIdOverrides(ShapeTemplate template, ShapeTemplate seeThroughTemplate, ChunkRenderTypeSet renderTypes, ItemTransforms transforms) {
            this.template = template;
            this.seeThroughTemplate = seeThroughTemplate;
            this.renderTypes = renderTypes;
            this.transforms = transforms;
        }

        @Nullable
        @Override
        public BakedModel resolve(BakedModel model, ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed) {
            String stackId = MainItem.getID(stack);
            if (stackId == null) {
                return model;
            }
            boolean seeThrough = AIBlockTransparency.ofStack(stack).seeThrough();
            ShapeTemplate shape = seeThrough ? seeThroughTemplate : template;
            return (seeThrough ? seeThroughModels : models).computeIfAbsent(stackId, key -> new AIShapedBlockModel(shape, renderTypes, transforms, key, ItemOverrides.EMPTY));
        }
    }
}
