package com.watermelon0117.aicraft.client.block;

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

/** A plant as an item: flat, with edges that follow its picture, so unlike a block its quads differ for each id. */
@OnlyIn(Dist.CLIENT)
public final class AICrossItemModel implements IDynamicBakedModel {
    private final BakedModel vanilla;
    private final ChunkRenderTypeSet renderTypes;
    @Nullable
    private final String id;
    private final ItemOverrides overrides;

    private AICrossItemModel(BakedModel vanilla, ChunkRenderTypeSet renderTypes, @Nullable String id, ItemOverrides overrides) {
        this.vanilla = vanilla;
        this.renderTypes = renderTypes;
        this.id = id;
        this.overrides = overrides;
    }

    /** @param vanilla the vanilla item model to take the transforms and lighting from */
    public static AICrossItemModel forItem(BakedModel vanilla, ChunkRenderTypeSet renderTypes) {
        return new AICrossItemModel(vanilla, renderTypes, null, new ByIdOverrides(vanilla, renderTypes));
    }

    @Override
    public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource rand,
                                             @NotNull ModelData data, @Nullable RenderType renderType) {
        return side == null ? BlockTileAtlas.crossItemQuads(id) : List.of();
    }

    @Override
    public @NotNull ChunkRenderTypeSet getRenderTypes(@NotNull BlockState state, @NotNull RandomSource rand, @NotNull ModelData data) {
        return renderTypes;
    }

    @Override
    public TextureAtlasSprite getParticleIcon(@NotNull ModelData data) {
        return BlockTileAtlas.particleSprite(id);
    }

    @Override
    public TextureAtlasSprite getParticleIcon() {
        return BlockTileAtlas.particleSprite(id);
    }

    @Override
    public ItemTransforms getTransforms() {
        return vanilla.getTransforms();
    }

    @Override
    public ItemOverrides getOverrides() {
        return overrides;
    }

    @Override
    public boolean useAmbientOcclusion() {
        return vanilla.useAmbientOcclusion();
    }

    @Override
    public boolean isGui3d() {
        return vanilla.isGui3d();
    }

    @Override
    public boolean usesBlockLight() {
        return vanilla.usesBlockLight();
    }

    @Override
    public boolean isCustomRenderer() {
        return false;
    }

    private static final class ByIdOverrides extends ItemOverrides {
        private final BakedModel vanilla;
        private final ChunkRenderTypeSet renderTypes;
        private final Map<String, AICrossItemModel> models = new ConcurrentHashMap<>();

        ByIdOverrides(BakedModel vanilla, ChunkRenderTypeSet renderTypes) {
            this.vanilla = vanilla;
            this.renderTypes = renderTypes;
        }

        @Nullable
        @Override
        public BakedModel resolve(BakedModel model, ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed) {
            String stackId = MainItem.getID(stack);
            if (stackId == null) {
                return model;
            }
            return models.computeIfAbsent(stackId, key -> new AICrossItemModel(vanilla, renderTypes, key, ItemOverrides.EMPTY));
        }
    }
}
