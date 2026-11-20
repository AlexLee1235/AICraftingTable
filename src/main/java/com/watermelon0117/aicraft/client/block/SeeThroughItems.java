package com.watermelon0117.aicraft.client.block;

import com.watermelon0117.aicraft.blocks.AIBlockTransparency;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.RenderTypeHelper;

import java.util.List;
import java.util.function.Supplier;

/** How an AI block item is drawn when its block is partly clear. */
@OnlyIn(Dist.CLIENT)
final class SeeThroughItems {
    private SeeThroughItems() {
    }

    /** @param otherwise the default, whose cutout sheet already suits an opaque or clear-or-solid block */
    static List<RenderType> renderTypes(ItemStack stack, boolean fabulous, Supplier<List<RenderType>> otherwise) {
        return AIBlockTransparency.ofStack(stack) == AIBlockTransparency.TRANSLUCENT
                ? List.of(RenderTypeHelper.getEntityRenderType(RenderType.translucent(), fabulous))
                : otherwise.get();
    }
}
