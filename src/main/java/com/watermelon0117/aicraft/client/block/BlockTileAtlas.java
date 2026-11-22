package com.watermelon0117.aicraft.client.block;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import com.watermelon0117.aicraft.AICraftingTable;
import com.watermelon0117.aicraft.blocks.PlantBounds;
import com.watermelon0117.aicraft.common.BlockFaces;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.BlockFaceUV;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.FaceBakery;
import net.minecraft.client.renderer.block.model.ItemModelGenerator;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector3f;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReferenceArray;

/**
 * Chunk meshes can only use the block atlas, and rebuilding that takes a full resource reload. So one large gray sprite
 * reserves space in it, and AI faces are written into 16x16 tiles of that sprite at run time. A tile's UVs never change,
 * so new pixels show up without rebuilding any mesh.
 *
 * <p>A plant whose picture keeps its item sprite's 24 or 32 pixels is written into a cell of two by two tiles instead,
 * from its top left corner. Its block entity says how large the picture is, so its UVs are known before it arrives too.
 */
@OnlyIn(Dist.CLIENT)
public final class BlockTileAtlas {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ResourceLocation POOL_ID = new ResourceLocation(AICraftingTable.MODID, "block/ai_pool");
    private static final ResourceLocation TILE_ID = new ResourceLocation(AICraftingTable.MODID, "block/ai_tile");
    private static final int TILE_SIZE = BlockFaces.FACE_SIZE;
    private static final int CELL_SIZE = TileAllocator.CELL_TILES * TILE_SIZE;
    /** The sides a plant's picture has when it needs a cell. */
    private static final int[] CELL_PICTURE_SIDES = {24, 32};
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
    /** How wide each id's top face is, read once when it arrives. */
    private static final Map<String, Integer> PICTURE_SIDES = new ConcurrentHashMap<>();
    private static volatile Generation generation;

    private BlockTileAtlas() {
    }

    /** Render thread only. */
    public static void put(String id, BlockFaces faces) {
        PICTURE_SIDES.put(id, pictureSideOf(faces));
        FACES.put(id, faces);
        Generation current = generation;
        if (current != null) {
            current.upload(id, faces);
        }
    }

    /** Called with the new atlas, before the new models are in place. */
    public static void onStitched(TextureAtlas atlas) {
        Generation previous = generation;
        Generation next = new Generation(atlas, atlas.getSprite(POOL_ID), previous == null ? new TileAllocator() : previous.allocator);
        generation = next;
        if (previous != null) {
            previous.close();
        }
        FACES.forEach(next::upload);
    }

    /** Leaving a world: its ids mean nothing in the next one. */
    public static void reset() {
        FACES.clear();
        PICTURE_SIDES.clear();
        Generation previous = generation;
        if (previous == null) {
            return;
        }
        generation = new Generation(previous.atlas, previous.pool, new TileAllocator());
        for (int tile = 1; tile <= previous.allocator.usedTiles(); tile++) {
            previous.uploadGray(tile);
        }
        for (int cellTile : previous.allocator.usedCellTiles()) {
            previous.uploadGrayCell(cellTile);
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

    /**
     * Safe on chunk workers.
     *
     * @param pictureSide how wide and high the plant's picture is, as its block entity says
     * @return the quads of a side of a plant, showing as much of the atlas as its picture covers
     */
    public static List<BakedQuad> plantQuads(ShapeTemplate template, @Nullable String id, int pictureSide, @Nullable Direction side) {
        if (!needsCell(pictureSide)) {
            return shapeQuads(template, id, side);
        }
        Generation current = generation;
        return current == null ? List.of() : ShapeTemplate.quadsOf(current.plantQuads(template, id, pictureSide), side);
    }

    /** The plant's picture, from as much of the atlas as it covers. */
    public static TextureAtlasSprite plantSprite(@Nullable String id, int pictureSide) {
        if (!needsCell(pictureSide)) {
            return shapeParticleSprite(id);
        }
        Generation current = generation;
        return current == null ? Minecraft.getInstance().getModelManager().getMissingModel().getParticleIcon() : current.cellSprite(id, pictureSide);
    }

    /** For a plant's item, which has no block entity: the picture's own size, once it has arrived. */
    public static TextureAtlasSprite plantSprite(@Nullable String id) {
        return plantSprite(id, id == null ? TILE_SIZE : PICTURE_SIDES.getOrDefault(id, TILE_SIZE));
    }

    private static boolean needsCell(int pictureSide) {
        return pictureSide > TILE_SIZE;
    }

    /** @return the width of the top face, which only a plant's picture makes larger than a tile; a tile's when it cannot be read */
    private static int pictureSideOf(BlockFaces faces) {
        try (NativeImage image = NativeImage.read(faces.top())) {
            return image.getWidth();
        } catch (IOException e) {
            return TILE_SIZE;
        }
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
        /** Only its size is read, so all the tile sprites share it. */
        final SpriteContents sharedContents;
        /** The same for the plant pictures in cells, one for each side they can have. */
        final Map<Integer, SpriteContents> cellContents = new HashMap<>();
        final AtomicReferenceArray<TextureAtlasSprite> sprites = new AtomicReferenceArray<>(TileAllocator.TILE_COUNT);
        /** By the cell's top left tile and the picture's side. */
        final Map<Integer, TextureAtlasSprite> cellSprites = new ConcurrentHashMap<>();
        final EnumMap<Direction, Map<String, List<List<BakedQuad>>>> quadsByFacing = new EnumMap<>(Direction.class);
        final Map<ShapeTemplate, Map<String, List<List<BakedQuad>>>> quadsByShape = new ConcurrentHashMap<>();
        /** By the id and the picture's side, as a block entity's tag may say another side than the picture has. */
        final Map<ShapeTemplate, Map<String, List<List<BakedQuad>>>> quadsByPlant = new ConcurrentHashMap<>();
        final Map<String, CrossItem> crossItems = new ConcurrentHashMap<>();
        final AtomicBoolean warnedFull = new AtomicBoolean();

        Generation(TextureAtlas atlas, TextureAtlasSprite pool, TileAllocator allocator) {
            this.atlas = atlas;
            this.pool = pool;
            this.allocator = allocator;
            SpriteContents poolContents = pool.contents();
            // A resource pack could replace the image, and a missing sprite is not the pool either.
            this.usable = poolContents.width() == POOL_SIZE && poolContents.height() == POOL_SIZE;
            if (!usable) {
                LOGGER.error("{} is {}x{}, not {}x{}; AI blocks will look gray", POOL_ID, poolContents.width(), poolContents.height(), POOL_SIZE, POOL_SIZE);
            }
            // TextureAtlas keeps its size to itself, but the pool's share of it gives the same number.
            this.atlasWidth = Math.round(poolContents.width() / (pool.getU1() - pool.getU0()));
            this.atlasHeight = Math.round(poolContents.height() / (pool.getV1() - pool.getV0()));
            this.mipLevel = poolContents.byMipLevel.length - 1;
            this.sharedContents = sizeOnly(TILE_SIZE);
            // Made here on the render thread, and never changed after, so chunk workers may read the map.
            for (int side : CELL_PICTURE_SIDES) {
                cellContents.put(side, sizeOnly(side));
            }
            for (Direction facing : Direction.Plane.HORIZONTAL) {
                quadsByFacing.put(facing, new ConcurrentHashMap<>());
            }
        }

        private static SpriteContents sizeOnly(int side) {
            return new SpriteContents(TILE_ID, new FrameSize(side, side), new NativeImage(side, side, false), AnimationMetadataSection.EMPTY, null);
        }

        void close() {
            sharedContents.close();
            cellContents.values().forEach(SpriteContents::close);
        }

        List<List<BakedQuad>> quads(@Nullable String id, Direction facing) {
            return quadsByFacing.get(facing).computeIfAbsent(id == null ? NO_ID : id, key -> bake(id, facing));
        }

        List<List<BakedQuad>> shapeQuads(ShapeTemplate template, @Nullable String id) {
            return quadsByShape.computeIfAbsent(template, key -> new ConcurrentHashMap<>())
                    .computeIfAbsent(id == null ? NO_ID : id, key -> template.on(facing -> sprite(id, shapeFaceOf(facing))));
        }

        List<List<BakedQuad>> plantQuads(ShapeTemplate template, @Nullable String id, int pictureSide) {
            return quadsByPlant.computeIfAbsent(template, key -> new ConcurrentHashMap<>())
                    .computeIfAbsent((id == null ? NO_ID : id) + "@" + pictureSide, key -> template.on(facing -> cellSprite(id, pictureSide)));
        }

        /** The top left pictureSide pixels of the id's cell, or the gray fallback tile when there is no room or no such side. */
        TextureAtlasSprite cellSprite(@Nullable String id, int pictureSide) {
            if (!usable) {
                return pool;
            }
            SpriteContents contents = cellContents.get(pictureSide);
            int cellTile = id == null || contents == null ? TileAllocator.NO_TILES : cellTileOf(id);
            if (cellTile == TileAllocator.NO_TILES) {
                return tileSprite(TileAllocator.FALLBACK_TILE);
            }
            return cellSprites.computeIfAbsent(cellTile * (CELL_SIZE + 1) + pictureSide,
                    key -> new TileSprite(InventoryMenu.BLOCK_ATLAS, contents, atlasWidth, atlasHeight, tileX(cellTile), tileY(cellTile)));
        }

        private int cellTileOf(String id) {
            int cellTile = allocator.cellTile(id);
            if (cellTile == TileAllocator.NO_TILES) {
                warnFull(id);
            }
            return cellTile;
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

        /** The edges follow the picture at its own size; before it arrives, the gray fallback tile, so no space is taken for a picture of unknown size. */
        private List<BakedQuad> bakeCrossItem(@Nullable String id, @Nullable BlockFaces faces) {
            NativeImage image = crossItemImage(faces);
            int side = image.getWidth();
            TextureAtlasSprite sprite = faces == null ? tileSprite(TileAllocator.FALLBACK_TILE)
                    : needsCell(side) ? cellSprite(id, side) : sprite(id, TOP_FACE);
            SpriteContents contents = new SpriteContents(TILE_ID, new FrameSize(side, side), image, AnimationMetadataSection.EMPTY, null);
            try {
                List<BakedQuad> quads = new ArrayList<>();
                for (BlockElement element : ITEM_MODELS.processFrames(BlockElementFace.NO_TINT, ITEM_LAYER, contents)) {
                    element.faces.forEach((direction, face) ->
                            quads.add(BlockModel.bakeFace(element, face, sprite, direction, BlockModelRotation.X0_Y0, TILE_ID)));
                }
                return List.copyOf(quads);
            } finally {
                contents.close();
            }
        }

        /** With nothing to read, a gray square, the same as the tile shows then. */
        private static NativeImage crossItemImage(@Nullable BlockFaces faces) {
            if (faces != null) {
                try {
                    NativeImage image = NativeImage.read(faces.top());
                    if (image.getWidth() == image.getHeight() && PlantBounds.isPictureSide(image.getWidth())) {
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
            if (firstTile == TileAllocator.NO_TILES) {
                warnFull(id);
            }
            return firstTile;
        }

        private void warnFull(String id) {
            if (warnedFull.compareAndSet(false, true)) {
                LOGGER.warn("The atlas space for AI blocks is full, about {} kinds of block or fewer with large plants; {} and any after it will look gray",
                        TileAllocator.MAX_BLOCKS, id);
            }
        }

        private TextureAtlasSprite tileSprite(int tile) {
            if (!usable) {
                return pool;
            }
            TextureAtlasSprite sprite = sprites.get(tile);
            if (sprite == null) {
                sprite = new TileSprite(InventoryMenu.BLOCK_ATLAS, sharedContents, atlasWidth, atlasHeight, tileX(tile), tileY(tile));
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

        /** Only a plant's picture is larger than a tile, and it goes to the plant's cell instead of three tiles. */
        void upload(String id, BlockFaces faces) {
            NativeImage top = read(faces.top());
            if (top != null && needsCell(top.getWidth())) {
                uploadPlant(id, top);
                return;
            }
            int firstTile = firstTileOf(id);
            if (firstTile == TileAllocator.NO_TILES) {
                if (top != null) {
                    top.close();
                }
                return;
            }
            if (top != null) {
                upload(firstTile + TOP_FACE, top, TILE_SIZE);
            }
            uploadPng(firstTile + FRONT_FACE, faces.front());
            uploadPng(firstTile + SIDE_FACE, faces.side());
        }

        @Nullable
        private static NativeImage read(byte[] png) {
            try {
                return NativeImage.read(png);
            } catch (IOException e) {
                LOGGER.warn("Unreadable AI block face", e);
                return null;
            }
        }

        private void uploadPng(int tile, byte[] png) {
            NativeImage image = read(png);
            if (image != null) {
                upload(tile, image, TILE_SIZE);
            }
        }

        /** Into the top left of a clear cell: only the picture's own pixels are ever shown. Closes the picture. */
        private void uploadPlant(String id, NativeImage picture) {
            try (picture) {
                int side = picture.getWidth();
                if (picture.getHeight() != side || !cellContents.containsKey(side)) {
                    LOGGER.warn("An AI plant picture is {}x{}, which no plant has; {} will look gray", picture.getWidth(), picture.getHeight(), id);
                    return;
                }
                int cellTile = cellTileOf(id);
                if (cellTile == TileAllocator.NO_TILES) {
                    return;
                }
                NativeImage cell = new NativeImage(CELL_SIZE, CELL_SIZE, true);
                for (int y = 0; y < side; y++) {
                    for (int x = 0; x < side; x++) {
                        cell.setPixelRGBA(x, y, picture.getPixelRGBA(x, y));
                    }
                }
                upload(cellTile, cell, CELL_SIZE);
            }
        }

        void uploadGray(int tile) {
            upload(tile, grayImage(TILE_SIZE), TILE_SIZE);
        }

        void uploadGrayCell(int cellTile) {
            upload(cellTile, grayImage(CELL_SIZE), CELL_SIZE);
        }

        private static NativeImage grayImage() {
            return grayImage(TILE_SIZE);
        }

        private static NativeImage grayImage(int side) {
            NativeImage image = new NativeImage(side, side, false);
            image.fillRect(0, 0, side, side, POOL_GRAY);
            return image;
        }

        /**
         * Closes the image. A new SpriteContents each time, because it would otherwise keep mip levels the atlas may no longer have.
         *
         * @param size a tile's side, or a cell's starting at a cell's top left tile
         */
        private void upload(int tile, NativeImage image, int size) {
            if (!usable || image.getWidth() != size || image.getHeight() != size) {
                image.close();
                return;
            }
            SpriteContents contents = new SpriteContents(TILE_ID, new FrameSize(size, size), image, AnimationMetadataSection.EMPTY, null);
            try {
                contents.increaseMipLevel(mipLevel);
                atlas.bind();
                contents.uploadFirstFrame(tileX(tile), tileY(tile));
            } finally {
                contents.close();
            }
        }
    }

    private record CrossItem(@Nullable BlockFaces faces, List<BakedQuad> quads) {
    }

    /** Vanilla's constructor is protected. This sprite is not registered in the atlas, which only ticks and closes its own. */
    private static final class TileSprite extends TextureAtlasSprite {
        TileSprite(ResourceLocation atlas, SpriteContents contents, int atlasWidth, int atlasHeight, int x, int y) {
            super(atlas, contents, atlasWidth, atlasHeight, x, y);
        }
    }
}
