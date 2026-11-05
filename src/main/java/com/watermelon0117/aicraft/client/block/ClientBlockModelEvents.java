package com.watermelon0117.aicraft.client.block;

import com.watermelon0117.aicraft.AICraftingTable;
import com.watermelon0117.aicraft.blocks.AIBlock;
import com.watermelon0117.aicraft.blocks.AIBlockTool;
import com.watermelon0117.aicraft.init.BlockInit;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Map;

@Mod.EventBusSubscriber(modid = AICraftingTable.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientBlockModelEvents {
    private ClientBlockModelEvents() {
    }

    /** Runs on a worker thread, before the new atlas exists, so the models must not look up any sprite yet. */
    @SubscribeEvent
    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        Map<ResourceLocation, BakedModel> models = event.getModels();
        ItemTransforms furnaceTransforms = transformsOf(models.get(new ModelResourceLocation(ForgeRegistries.ITEMS.getKey(Items.FURNACE), "inventory")));
        for (AIBlockTool tool : AIBlockTool.values()) {
            Block block = BlockInit.aiBlock(tool);
            for (BlockState state : block.getStateDefinition().getPossibleStates()) {
                ResourceLocation key = BlockModelShaper.stateToModelLocation(state);
                models.put(key, AIBlockModel.forState(state.getValue(AIBlock.FACING), transformsOf(models.get(key))));
            }
            ResourceLocation itemKey = new ModelResourceLocation(ForgeRegistries.ITEMS.getKey(BlockInit.aiBlockItem(tool)), "inventory");
            models.put(itemKey, AIBlockModel.forItem(transformsOf(models.get(itemKey)), furnaceTransforms));
        }
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
