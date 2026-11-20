package com.watermelon0117.aicraft.client.block;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import com.mojang.math.Vector3f;
import com.watermelon0117.aicraft.AICraftingTable;
import com.watermelon0117.aicraft.common.BlockFaces;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.BlockFaceUV;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.FaceBakery;
import net.minecraft.client.renderer.block.model.ItemModelGenerator;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReferenceArray;

/**
 * Chunk meshes can only use the block atlas, and rebuilding that takes a full resource reload. So one large gray sprite
 * reserves space in it, and AI faces are written into 16x16 tiles of that sprite at run time. A tile's UVs never change,
 * so new pixels show up without rebuilding any mesh.
 */
@OnlyIn(Dist.CLIENT)
public final class BlockTileAtlas {
    private static final Logger LOGGER = LogUtils.getLogger();
    static final ResourceLocation POOL_ID = new ResourceLocation(AICraftingTable.MODID, "block/ai_pool");
    private static final ResourceLocation TILE_ID = new ResourceLocation(AICraftingTable.MODID, "block/ai_tile");
    private static final int TILE_SIZE = BlockFaces.FACE_SIZE;
    private static final TextureAtlasSprite.Info TILE_INFO = new TextureAtlasSprite.Info(TILE_ID, TILE_SIZE, TILE_SIZE, AnimationMetadataSection.EMPTY);
    private static final int POOL_SIZE = TileAllocator.TILES_PER_ROW * TILE_SIZE;
    /** ABGR, the same gray as the pool image. */
    private static final int POOL_GRAY = 0xFF808080;
    private static final int TOP_FACE = 0;
    private static final int FRONT_FACE = 1;
    private static final int SIDE_FACE = 2;
    /** Every shape but the cube has no front, and keeps its underside there. */
    private static final int UNDERSIDE_FACE = FRONT_FACE;
    private static final String NO_ID = "";
    private static final FaceBakery BAKERY = new FaceBakery();
    private static final ItemModelGenerator ITEM_MODELS = new ItemModelGenerator();
    private static final String ITEM_LAYER = "layer0";

    /** The PNGs of the current world: what gets written back after the atlas is rebuilt. */
    private static final Map<String, BlockFaces> FACES = new ConcurrentHashMap<>();
    private static volatile Generation generation;

    private BlockTileAtlas() {
    }

    /** Render thread only. */
    public static void put(String id, BlockFaces faces) {
        FACES.put(id, faces);
        Generation current = generation;
        if (current != null) {
            current.upload(id, faces);
        }
    }

    /** Called with the new atlas on the render thread, before the new models are in place. */
    public static void onStitched(TextureAtlas atlas) {
        Generation previous = generation;
        Generation next = new Generation(atlas, atlas.getSprite(POOL_ID), previous == null ? new TileAllocator() : previous.allocator, mipLevelOf(atlas));
        generation = next;
        if (previous != null) {
            previous.close();
        }
        FACES.forEach(next::upload);
    }

    /** Leaving a world: its ids mean nothing in the next one. */
    public static void reset() {
        FACES.clear();
        Generation previous = generation;
        if (previous == null) {
            return;
        }
        generation = new Generation(previous.atlas, previous.pool, new TileAllocator(), previous.mipLevel);
        for (int tile = 1; tile <= previous.allocator.usedTiles(); tile++) {
            previous.uploadGray(tile);
        }
        previous.close();
    }

    /** Safe on chunk workers. @return the single quad of a side, empty before the first stitch */
    public static List<BakedQuad> quads(@Nullable String id, Direction facing, Direction side) {
        Generation current = generation;
        return current == null ? List.of() : current.quads(id, facing).get(side.ordinal());
    }

    /** Safe on chunk workers. @return the quads of a side, each showing the id's tile for the way it faces */
    public static List<BakedQuad> shapeQuads(ShapeTemplate template, @Nullable String id, @Nullable Direction side) {
        Generation current = generation;
        return current == null ? List.of() : ShapeTemplate.quadsOf(current.shapeQuads(template, id), side);
    }

    /** Render thread only. @return the quads of a plant's flat item, whose edges follow its picture */
    public static List<BakedQuad> crossItemQuads(@Nullable String id) {
        Generation current = generation;
        return current == null ? List.of() : current.crossItemQuads(id);
    }

    /** The front, as on vanilla's furnace. Blocks without a front have their side there. */
    public static TextureAtlasSprite particleSprite(@Nullable String id) {
        return spriteOf(id, FRONT_FACE);
    }

    /** The side, as on vanilla's slabs and stairs. */
    public static TextureAtlasSprite shapeParticleSprite(@Nullable String id) {
        return spriteOf(id, SIDE_FACE);
    }

    private static TextureAtlasSprite spriteOf(@Nullable String id, int face) {
        Generation current = generation;
        if (current == null) {
            return Minecraft.getInstance().getModelManager().getMissingModel().getParticleIcon();
        }
        return current.sprite(id, face);
    }

    /** The atlas keeps its mip level to itself, but it made that the texture's last level, and Forge never lowers it below the option. */
    private static int mipLevelOf(TextureAtlas atlas) {
        RenderSystem.assertOnRenderThread();
        atlas.bind();
        return GL11.glGetTexParameteri(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LEVEL);
    }

    public static boolean hasFront(String id) {
        BlockFaces faces = FACES.get(id);
        return faces != null && faces.hasFront();
    }

    /** Everything derived from one stitched atlas. Replaced whole, so a chunk worker still holding the old one only wastes its work. */
    private static final class Generation {
        final TextureAtlas atlas;
        final TextureAtlasSprite pool;
        final TileAllocator allocator;
        final boolean usable;
        final int atlasWidth;
        final int atlasHeight;
        final int mipLevel;
        /** The tile sprites only give UVs, so they share one image, which they never upload. */
        final NativeImage sharedImage = new NativeImage(TILE_SIZE, TILE_SIZE, false);
        final AtomicReferenceArray<TextureAtlasSprite> sprites = new AtomicReferenceArray<>(TileAllocator.TILE_COUNT);
        final EnumMap<Direction, Map<String, List<List<BakedQuad>>>> quadsByFacing = new EnumMap<>(Direction.class);
        final Map<ShapeTemplate, Map<String, List<List<BakedQuad>>>> quadsByShape = new ConcurrentHashMap<>();
        final Map<String, CrossItem> crossItems = new ConcurrentHashMap<>();
        final AtomicBoolean warnedFull = new AtomicBoolean();

        Generation(TextureAtlas atlas, TextureAtlasSprite pool, TileAllocator allocator, int mipLevel) {
            this.atlas = atlas;
            this.pool = pool;
            this.allocator = allocator;
            // A resource pack could replace the image, and a missing sprite is not the pool either.
            this.usable = pool.getWidth() == POOL_SIZE && pool.getHeight() == POOL_SIZE;
            if (!usable) {
                LOGGER.error("{} is {}x{}, not {}x{}; AI blocks will look gray", POOL_ID, pool.getWidth(), pool.getHeight(), POOL_SIZE, POOL_SIZE);
            }
            // TextureAtlas keeps its size to itself, but the pool's share of it gives the same number.
            this.atlasWidth = Math.round(pool.getWidth() / (pool.getU1() - pool.getU0()));
            this.atlasHeight = Math.round(pool.getHeight() / (pool.getV1() - pool.getV0()));
            this.mipLevel = mipLevel;
            for (Direction facing : Direction.Plane.HORIZONTAL) {
                quadsByFacing.put(facing, new ConcurrentHashMap<>());
            }
        }

        void close() {
            sharedImage.close();
        }

        List<List<BakedQuad>> quads(@Nullable String id, Direction facing) {
            return quadsByFacing.get(facing).computeIfAbsent(id == null ? NO_ID : id, key -> bake(id, facing));
        }

        List<List<BakedQuad>> shapeQuads(ShapeTemplate template, @Nullable String id) {
            return quadsByShape.computeIfAbsent(template, key -> new ConcurrentHashMap<>())
                    .computeIfAbsent(id == null ? NO_ID : id, key -> template.on(facing -> sprite(id, shapeFaceOf(facing))));
        }

        private static int shapeFaceOf(Direction facing) {
            return switch (facing) {
                case UP -> TOP_FACE;
                case DOWN -> UNDERSIDE_FACE;
                default -> SIDE_FACE;
            };
        }

        /** Asked every time, because an item can be on screen before its picture arrives. */
        List<BakedQuad> crossItemQuads(@Nullable String id) {
            String key = id == null ? NO_ID : id;
            BlockFaces faces = FACES.get(key);
            CrossItem item = crossItems.get(key);
            if (item == null || item.faces() != faces) {
                item = new CrossItem(faces, bakeCrossItem(id, faces));
                crossItems.put(key, item);
            }
            return item.quads();
        }

        private List<BakedQuad> bakeCrossItem(@Nullable String id, @Nullable BlockFaces faces) {
            TextureAtlasSprite sprite = sprite(id, TOP_FACE);
            // Only read for which pixels are clear; the quads take the tile's UVs.
            TileSprite picture = new TileSprite(atlas, TILE_INFO, 0, atlasWidth, atlasHeight, 0, 0, crossItemImage(faces));
            try {
                List<BakedQuad> quads = new ArrayList<>();
                for (BlockElement element : ITEM_MODELS.processFrames(BlockElementFace.NO_TINT, ITEM_LAYER, picture)) {
                    element.faces.forEach((direction, face) ->
                            quads.add(BlockModel.bakeFace(element, face, sprite, direction, BlockModelRotation.X0_Y0, TILE_ID)));
                }
                return List.copyOf(quads);
            } finally {
                picture.close();
            }
        }

        /** With nothing to read, a gray square, the same as the tile shows then. */
        private static NativeImage crossItemImage(@Nullable BlockFaces faces) {
            if (faces != null) {
                try {
                    NativeImage image = NativeImage.read(new ByteArrayInputStream(faces.top()));
                    if (image.getWidth() == TILE_SIZE && image.getHeight() == TILE_SIZE) {
                        return image;
                    }
                    image.close();
                } catch (IOException e) {
                    LOGGER.warn("Unreadable AI plant picture", e);
                }
            }
            return grayImage();
        }

        /** Laid out like vanilla's orientable model: the front is north, then the whole model turns to face its direction. */
        private List<List<BakedQuad>> bake(@Nullable String id, Direction facing) {
            BlockModelRotation rotation = BlockModelRotation.by(0, yRotationOf(facing));
            List<List<BakedQuad>> bySide = new ArrayList<>(Collections.nCopies(Direction.values().length, null));
            for (Direction modelSide : Direction.values()) {
                TextureAtlasSprite sprite = sprite(id, faceOf(modelSide));
                // FaceBakery briefly changes the UV object it is given, and many threads bake at once.
                BlockElementFace face = new BlockElementFace(modelSide, BlockElementFace.NO_TINT, NO_ID,
                        new BlockFaceUV(new float[]{0, 0, TILE_SIZE, TILE_SIZE}, 0));
                BakedQuad quad = BAKERY.bakeQuad(new Vector3f(0, 0, 0), new Vector3f(TILE_SIZE, TILE_SIZE, TILE_SIZE),
                        face, sprite, modelSide, rotation, null, true, TILE_ID);
                bySide.set(quad.getDirection().ordinal(), List.of(quad));
            }
            return List.copyOf(bySide);
        }

        private static int yRotationOf(Direction facing) {
            return switch (facing) {
                case EAST -> 90;
                case SOUTH -> 180;
                case WEST -> 270;
                default -> 0;
            };
        }

        private static int faceOf(Direction modelSide) {
            return switch (modelSide) {
                case UP, DOWN -> TOP_FACE;
                case NORTH -> FRONT_FACE;
                default -> SIDE_FACE;
            };
        }

        TextureAtlasSprite sprite(@Nullable String id, int face) {
            int firstTile = id == null ? TileAllocator.NO_TILES : firstTileOf(id);
            return tileSprite(firstTile == TileAllocator.NO_TILES ? TileAllocator.FALLBACK_TILE : firstTile + face);
        }

        private int firstTileOf(String id) {
            int firstTile = allocator.firstTile(id);
            if (firstTile == TileAllocator.NO_TILES && warnedFull.compareAndSet(false, true)) {
                LOGGER.warn("More than {} kinds of AI block; {} and any after it will look gray", TileAllocator.MAX_BLOCKS, id);
            }
            return firstTile;
        }

        private TextureAtlasSprite tileSprite(int tile) {
            if (!usable) {
                return pool;
            }
            TextureAtlasSprite sprite = sprites.get(tile);
            if (sprite == null) {
                sprite = new TileSprite(atlas, TILE_INFO, 0, atlasWidth, atlasHeight, tileX(tile), tileY(tile), sharedImage);
                if (!sprites.compareAndSet(tile, null, sprite)) {
                    sprite = sprites.get(tile);
                }
            }
            return sprite;
        }

        private int tileX(int tile) {
            return pool.getX() + tile % TileAllocator.TILES_PER_ROW * TILE_SIZE;
        }

        private int tileY(int tile) {
            return pool.getY() + tile / TileAllocator.TILES_PER_ROW * TILE_SIZE;
        }

        void upload(String id, BlockFaces faces) {
            int firstTile = firstTileOf(id);
            if (firstTile == TileAllocator.NO_TILES) {
                return;
            }
            uploadPng(firstTile + TOP_FACE, faces.top());
            uploadPng(firstTile + FRONT_FACE, faces.front());
            uploadPng(firstTile + SIDE_FACE, faces.side());
        }

        private void uploadPng(int tile, byte[] png) {
            try {
                upload(tile, NativeImage.read(new ByteArrayInputStream(png)));
            } catch (IOException e) {
                LOGGER.warn("Unreadable AI block face", e);
            }
        }

        void uploadGray(int tile) {
            upload(tile, grayImage());
        }

        private static NativeImage grayImage() {
            NativeImage image = new NativeImage(TILE_SIZE, TILE_SIZE, false);
            image.fillRect(0, 0, TILE_SIZE, TILE_SIZE, POOL_GRAY);
            return image;
        }

        /** Closes the image. A sprite of its own each time, which makes the image's mip levels and writes each where the tile is. */
        private void upload(int tile, NativeImage image) {
            if (!usable || image.getWidth() != TILE_SIZE || image.getHeight() != TILE_SIZE) {
                image.close();
                return;
            }
            TileSprite tileImage = new TileSprite(atlas, TILE_INFO, mipLevel, atlasWidth, atlasHeight, tileX(tile), tileY(tile), image);
            try {
                atlas.bind();
                tileImage.uploadFirstFrame();
            } finally {
                tileImage.close();
            }
        }
    }

    private record CrossItem(@Nullable BlockFaces faces, List<BakedQuad> quads) {
    }

    /** Vanilla's constructor is protected. This sprite is not registered in the atlas, which only ticks and closes its own. */
    private static final class TileSprite extends TextureAtlasSprite {
        TileSprite(TextureAtlas atlas, Info info, int mipLevel, int atlasWidth, int atlasHeight, int x, int y, NativeImage image) {
            super(atlas, info, mipLevel, atlasWidth, atlasHeight, x, y, image);
        }
    }
}
