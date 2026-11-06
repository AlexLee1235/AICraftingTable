package com.watermelon0117.aicraft.client.block;

import com.watermelon0117.aicraft.AICraftingTable;
import com.watermelon0117.aicraft.blocks.AIBlock;
import com.watermelon0117.aicraft.blocks.AIPlacedBlock;
import com.watermelon0117.aicraft.init.BlockInit;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Mod.EventBusSubscriber(modid = AICraftingTable.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientBlockModelEvents {
    private static final ChunkRenderTypeSet SOLID = ChunkRenderTypeSet.of(RenderType.solid());
    private static final ChunkRenderTypeSet CUTOUT = ChunkRenderTypeSet.of(RenderType.cutout());
    private static final ChunkRenderTypeSet TRANSLUCENT = ChunkRenderTypeSet.of(RenderType.translucent());

    private ClientBlockModelEvents() {
    }

    /** Runs on a worker thread, before the new atlas exists, so the models must not look up any sprite yet. */
    @SubscribeEvent
    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        Map<ResourceLocation, BakedModel> models = event.getModels();
        ItemTransforms furnaceTransforms = transformsOf(models.get(new ModelResourceLocation(ForgeRegistries.ITEMS.getKey(Items.FURNACE), "inventory")));
        for (BlockState state : BlockInit.AI_BLOCK.get().getStateDefinition().getPossibleStates()) {
            ResourceLocation key = BlockModelShaper.stateToModelLocation(state);
            models.put(key, AIBlockModel.forState(state.getValue(AIBlock.FACING), layerOf(state), transformsOf(models.get(key))));
        }
        ResourceLocation itemKey = new ModelResourceLocation(ForgeRegistries.ITEMS.getKey(BlockInit.AI_BLOCK_ITEM.get()), "inventory");
        models.put(itemKey, AIBlockModel.forItem(transformsOf(models.get(itemKey)), furnaceTransforms));

        shapeLike(models, BlockInit.AI_SLAB.get(), Blocks.OAK_SLAB, ClientBlockModelEvents::layerOf);
        shapeLike(models, BlockInit.AI_STAIRS.get(), Blocks.OAK_STAIRS, ClientBlockModelEvents::layerOf);
        shapeLike(models, BlockInit.AI_FENCE.get(), Blocks.OAK_FENCE, ClientBlockModelEvents::layerOf);
        shapeLike(models, BlockInit.AI_WALL.get(), Blocks.COBBLESTONE_WALL, ClientBlockModelEvents::layerOf);
        shapeLike(models, BlockInit.AI_CROSS.get(), Blocks.OAK_SAPLING, state -> CUTOUT);
        models.put(itemModelKey(BlockInit.AI_CROSS.get()), AICrossItemModel.forItem(models.get(itemModelKey(Blocks.OAK_SAPLING)), CUTOUT));
    }

    /** Glass is cut out and stained glass translucent; a translucent block would hide rain and particles behind its clear pixels. */
    private static ChunkRenderTypeSet layerOf(BlockState state) {
        return switch (state.getValue(AIPlacedBlock.TRANSPARENCY)) {
            case OPAQUE -> SOLID;
            case CUTOUT -> CUTOUT;
            case TRANSLUCENT -> TRANSLUCENT;
        };
    }

    /**
     * Gives an AI block the look of a vanilla one, state for state. The models read their quads now, and a sprite's numbers are already settled here.
     * The item is solid: it has no state, and {@link SeeThroughItems} picks its sheet from its tag.
     */
    private static void shapeLike(Map<ResourceLocation, BakedModel> models, Block block, Block vanilla, Function<BlockState, ChunkRenderTypeSet> layers) {
        Map<BlockState, ShapeTemplate> templates = new HashMap<>();
        for (BlockState state : block.getStateDefinition().getPossibleStates()) {
            ResourceLocation key = BlockModelShaper.stateToModelLocation(state);
            ShapeTemplate template = templates.computeIfAbsent(vanillaStateOf(state, vanilla),
                    vanillaState -> ShapeTemplate.of(models.get(BlockModelShaper.stateToModelLocation(vanillaState)), vanillaState));
            models.put(key, AIShapedBlockModel.forState(template, layers.apply(state), transformsOf(models.get(key))));
        }
        BakedModel vanillaItem = models.get(itemModelKey(vanilla));
        models.put(itemModelKey(block), AIShapedBlockModel.forItem(ShapeTemplate.of(vanillaItem, null), layers.apply(block.defaultBlockState()),
                transformsOf(vanillaItem)));
    }

    private static BlockState vanillaStateOf(BlockState state, Block vanilla) {
        BlockState result = vanilla.defaultBlockState();
        for (Property<?> property : state.getProperties()) {
            if (result.hasProperty(property) && property != BlockStateProperties.WATERLOGGED) {
                result = withValueOf(state, result, property);
            }
        }
        return result;
    }

    private static <T extends Comparable<T>> BlockState withValueOf(BlockState from, BlockState to, Property<T> property) {
        return to.setValue(property, from.getValue(property));
    }

    private static ResourceLocation itemModelKey(Block block) {
        return new ModelResourceLocation(ForgeRegistries.ITEMS.getKey(block.asItem()), "inventory");
    }

    private static ItemTransforms transformsOf(BakedModel model) {
        return model == null ? ItemTransforms.NO_TRANSFORMS : model.getTransforms();
    }

    @SubscribeEvent
    public static void onTextureStitched(TextureStitchEvent.Post event) {
        if (event.getAtlas().location().equals(InventoryMenu.BLOCK_ATLAS)) {
            BlockTileAtlas.onStitched(event.getAtlas());
        }
    }

    @Mod.EventBusSubscriber(modid = AICraftingTable.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static final class ForgeBus {
        private ForgeBus() {
        }

        @SubscribeEvent
        public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
            BlockTileAtlas.reset();
        }
    }
}
