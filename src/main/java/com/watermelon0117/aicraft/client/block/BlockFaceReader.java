package com.watermelon0117.aicraft.client.block;

import com.watermelon0117.aicraft.common.BlockFaces;
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
import net.minecraftforge.fml.ModList;
import net.minecraftforge.forgespi.language.IModFileInfo;

import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads a plain block's faces as the game draws them without resource packs, for the blocks the mod copies: vanilla's or any other mod's.
 * Only clients have the blocks' pictures. Resource packs are left out, so a block copied by any player looks the same, as every player has the same mods.
 */
public final class BlockFaceReader {
    private static final byte[] NONE = new byte[0];

    private BlockFaceReader() {
    }

    /**
     * The faces of the default state, for a slab, stairs, a fence or a wall. East is a side whichever way the block faces by default: a log's bark, a furnace's side.
     *
     * @return the faces, or null when a layer of one is not a plain picture of vanilla or a mod
     */
    @Nullable
    public static BlockFaces faces(Block block) {
        byte[] top = face(block, Direction.UP);
        byte[] underside = face(block, Direction.DOWN);
        byte[] side = face(block, Direction.EAST);
        return top.length == 0 || underside.length == 0 || side.length == 0 ? null : BlockFaces.ofShaped(top, underside, side);
    }

    /**
     * A tinted layer, as on grass or leaves, takes the color the game gives where there is no world, as in the inventory.
     *
     * @return the face as a PNG, or an empty array when some layer is not a plain picture of vanilla or a mod
     */
    private static byte[] face(Block block, Direction direction) {
        Minecraft minecraft = Minecraft.getInstance();
        BlockState state = block.defaultBlockState();
        var model = minecraft.getBlockRenderer().getBlockModel(state);
        List<BakedQuad> quads = model.getQuads(state, direction, RandomSource.create(42L), ModelData.EMPTY, null);
        if (quads.isEmpty()) {
            // A face that is never culled sits with the unculled quads instead.
            quads = model.getQuads(state, null, RandomSource.create(42L), ModelData.EMPTY, null).stream()
                    .filter(quad -> quad.getDirection() == direction).toList();
        }
        if (quads.isEmpty()) {
            return NONE;
        }
        List<FaceImages.Layer> layers = new ArrayList<>();
        for (BakedQuad quad : quads) {
            ResourceLocation sprite = quad.getSprite().contents().name();
            IoSupplier<InputStream> resource = picture(minecraft, new ResourceLocation(sprite.getNamespace(), "textures/" + sprite.getPath() + ".png"));
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

    /**
     * Vanilla's own picture comes first, so a vanilla block looks as the base game draws it even where a mod changes that picture.
     * Any other picture is read from the mod files themselves: first the mod named by its namespace, then any mod, as one mod may draw with another's namespace.
     * Forge's own table of mod packs is no help here: an integrated server fills it again with its data packs as a world opens.
     *
     * @return the picture, or null when neither vanilla nor a mod has it, as for a picture a model makes as the game loads
     */
    @Nullable
    private static IoSupplier<InputStream> picture(Minecraft minecraft, ResourceLocation file) {
        IoSupplier<InputStream> vanilla = minecraft.getVanillaPackResources().getResource(PackType.CLIENT_RESOURCES, file);
        if (vanilla != null) {
            return vanilla;
        }
        String[] path = ("assets/" + file.getNamespace() + "/" + file.getPath()).split("/");
        List<IModFileInfo> modFiles = new ArrayList<>();
        IModFileInfo named = ModList.get().getModFileById(file.getNamespace());
        if (named != null) {
            modFiles.add(named);
        }
        modFiles.addAll(ModList.get().getModFiles());
        for (IModFileInfo modFile : modFiles) {
            Path found = modFile.getFile().findResource(path);
            if (Files.isRegularFile(found)) {
                return () -> Files.newInputStream(found);
            }
        }
        return null;
    }
}
