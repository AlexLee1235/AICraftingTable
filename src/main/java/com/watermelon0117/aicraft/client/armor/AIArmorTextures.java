package com.watermelon0117.aicraft.client.armor;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import com.watermelon0117.aicraft.AICraftingTable;
import com.watermelon0117.aicraft.common.ArmorTexture;
import com.watermelon0117.aicraft.common.ArmorUV;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Each AI armor piece's worn picture as a texture of its own, and its model. Render thread only: packets are handled there and
 * armor is drawn there. A piece whose picture has not arrived, or does not fit its slot, is drawn plain gray, as a block's gray tile.
 */
@OnlyIn(Dist.CLIENT)
public final class AIArmorTextures {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ResourceLocation PLAIN = new ResourceLocation(AICraftingTable.MODID, "armor/plain");
    private static final int PLAIN_COLOR = 0xFF8B8B8B;

    /** Names stay with their id for the session, so a regenerated picture replaces the one drawn under that name. */
    private static final Map<String, ResourceLocation> NAMES = new HashMap<>();
    private static final Map<String, Entry> ENTRIES = new HashMap<>();
    private static final Map<ArmorUV.Slot, HumanoidModel<LivingEntity>> PLAIN_MODELS = new EnumMap<>(ArmorUV.Slot.class);
    private static boolean plainRegistered;

    private record Entry(ResourceLocation name, ArmorUV uv, HumanoidModel<LivingEntity> model) {
    }

    private AIArmorTextures() {
    }

    /** Takes a piece's picture, replacing one it had. A picture that cannot be drawn is logged and left out. */
    public static void put(String id, ArmorTexture texture) {
        ArmorUV uv;
        NativeImage image;
        try {
            uv = ArmorUV.parse(texture.uv());
            if (texture.png().length > ArmorTexture.MAX_PNG_BYTES) {
                throw new IllegalArgumentException("The armor picture is " + texture.png().length + " bytes");
            }
            image = NativeImage.read(new ByteArrayInputStream(texture.png()));
        } catch (IOException | IllegalArgumentException e) {
            LOGGER.error("Cannot draw the armor picture of {}", id, e);
            return;
        }
        if (image.getWidth() != uv.width() || image.getHeight() != uv.height()) {
            LOGGER.error("The armor picture of {} is {}x{} but its table says {}x{}", id, image.getWidth(), image.getHeight(), uv.width(), uv.height());
            image.close();
            return;
        }
        ResourceLocation name = NAMES.computeIfAbsent(id, key -> new ResourceLocation(AICraftingTable.MODID, "armor/" + NAMES.size()));
        // Registering under a name already taken closes the texture that had it.
        Minecraft.getInstance().getTextureManager().register(name, new DynamicTexture(image));
        ENTRIES.put(id, new Entry(name, uv, AIArmorModel.build(uv)));
    }

    /** @return the texture to draw the piece with, as the path Forge's getArmorTexture returns */
    public static String textureName(@Nullable String id, ArmorUV.Slot slot) {
        Entry entry = fitting(id, slot);
        return entry != null ? entry.name.toString() : plainName().toString();
    }

    public static HumanoidModel<LivingEntity> model(@Nullable String id, ArmorUV.Slot slot) {
        Entry entry = fitting(id, slot);
        return entry != null ? entry.model : PLAIN_MODELS.computeIfAbsent(slot, s -> AIArmorModel.build(ArmorUV.plain(s)));
    }

    @Nullable
    private static Entry fitting(@Nullable String id, ArmorUV.Slot slot) {
        Entry entry = id == null ? null : ENTRIES.get(id);
        return entry != null && entry.uv.slot() == slot ? entry : null;
    }

    private static ResourceLocation plainName() {
        if (!plainRegistered) {
            NativeImage image = new NativeImage(1, 1, false);
            image.setPixelRGBA(0, 0, PLAIN_COLOR);
            Minecraft.getInstance().getTextureManager().register(PLAIN, new DynamicTexture(image));
            plainRegistered = true;
        }
        return PLAIN;
    }

    /** For a player leaving a world: the next one has its own pieces, and may use the same ids for others. */
    public static void reset() {
        ENTRIES.values().forEach(entry -> Minecraft.getInstance().getTextureManager().release(entry.name));
        ENTRIES.clear();
    }

    /** How many pieces have their picture, for checks. */
    public static int size() {
        return ENTRIES.size();
    }
}
