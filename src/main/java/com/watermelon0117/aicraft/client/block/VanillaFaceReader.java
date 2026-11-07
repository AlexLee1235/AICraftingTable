package com.watermelon0117.aicraft.client.block;

import com.watermelon0117.aicraft.common.FaceImages;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads a vanilla block's side as the base game draws it, for the blocks the mod copies. Only clients have vanilla's pictures.
 * Resource packs are left out, so a block copied by any player looks the same.
 */
public final class VanillaFaceReader {
    private static final byte[] NONE = new byte[0];

    private VanillaFaceReader() {
    }

    /**
     * The east face of the default state, which is a side whichever way the block faces by default: a log's bark, a furnace's side.
     * A tinted layer, as on grass or leaves, takes the color the game gives where there is no world, as in the inventory.
     *
     * @return the face as a PNG, or an empty array when some layer is not a plain vanilla picture
     */
    public static byte[] side(Block block) {
        Minecraft minecraft = Minecraft.getInstance();
        BlockState state = block.defaultBlockState();
        var model = minecraft.getBlockRenderer().getBlockModel(state);
        List<BakedQuad> quads = model.getQuads(state, Direction.EAST, RandomSource.create(42L), ModelData.EMPTY, null);
        if (quads.isEmpty()) {
            // A face that is never culled sits with the unculled quads instead.
            quads = model.getQuads(state, null, RandomSource.create(42L), ModelData.EMPTY, null).stream()
                    .filter(quad -> quad.getDirection() == Direction.EAST).toList();
        }
        if (quads.isEmpty()) {
            return NONE;
        }
        List<FaceImages.Layer> layers = new ArrayList<>();
        for (BakedQuad quad : quads) {
            ResourceLocation sprite = quad.getSprite().contents().name();
            IoSupplier<InputStream> resource = minecraft.getVanillaPackResources().getResource(PackType.CLIENT_RESOURCES,
                    new ResourceLocation(sprite.getNamespace(), "textures/" + sprite.getPath() + ".png"));
            if (resource == null) {
                return NONE;
            }
            BufferedImage frame;
            try (InputStream in = resource.get()) {
                frame = FaceImages.firstFrame(ImageIO.read(in));
            } catch (IOException e) {
                return NONE;
            }
            if (frame == null) {
                return NONE;
            }
            int tint = quad.isTinted() ? minecraft.getBlockColors().getColor(state, null, null, quad.getTintIndex()) : -1;
            layers.add(new FaceImages.Layer(frame, tint));
        }
        return FaceImages.png(FaceImages.compose(layers));
    }
}
