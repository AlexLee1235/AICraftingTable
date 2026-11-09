package com.watermelon0117.aicraft.common;

import com.watermelon0117.aicraft.blockentities.AIBlockEntity;
import com.watermelon0117.aicraft.blocks.AIBlockAttach;
import com.watermelon0117.aicraft.blocks.AIBlockHardness;
import com.watermelon0117.aicraft.blocks.AIBlockLight;
import com.watermelon0117.aicraft.blocks.AIBlockShape;
import com.watermelon0117.aicraft.blocks.AIBlockSound;
import com.watermelon0117.aicraft.blocks.AIBlockTool;
import com.watermelon0117.aicraft.blocks.AIPlacedBlock;
import com.watermelon0117.aicraft.gpt.GeneratedBlock;
import com.watermelon0117.aicraft.gpt.GenerationException;
import com.watermelon0117.aicraft.gpt.ItemIdSanitizer;
import com.watermelon0117.aicraft.init.BlockInit;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Vanilla's own recipe for a slab, stairs, a fence or a wall, made of one full block, under a name of that same shape.
 * The block is a cube AI block or a vanilla one. The mod makes such a block itself instead of asking the texture server:
 * it shows the material's own top, bottom and side and keeps its properties.
 * The layouts and shape words are the texture server's ({@code matchBlockShape} and {@code SHAPE_WORDS}).
 *
 * @param material one of the grid's material stacks
 */
public record ShapedBlockRecipe(AIBlockShape shape, ItemStack material) {
    /** Each layout cut down to its own rows and columns: I is the material and S a stick. */
    private static final Map<List<String>, AIBlockShape> LAYOUTS = Map.of(
            List.of("III"), AIBlockShape.SLAB,
            List.of("I  ", "II ", "III"), AIBlockShape.STAIRS,
            List.of("  I", " II", "III"), AIBlockShape.STAIRS,
            List.of("III", "III"), AIBlockShape.WALL,
            List.of("ISI", "ISI"), AIBlockShape.FENCE);
    /** Whole words only, so a Wallpaper is no wall. */
    private static final Map<AIBlockShape, Pattern> WORDS = Map.of(
            AIBlockShape.SLAB, Pattern.compile("\\bslabs?\\b", Pattern.CASE_INSENSITIVE),
            AIBlockShape.STAIRS, Pattern.compile("\\bstair(s|case|way)?\\b", Pattern.CASE_INSENSITIVE),
            AIBlockShape.FENCE, Pattern.compile("\\bfences?\\b", Pattern.CASE_INSENSITIVE),
            AIBlockShape.WALL, Pattern.compile("\\bwalls?\\b", Pattern.CASE_INSENSITIVE));

    /** @return the shape these 9 cells make wherever they sit in the grid, reading ' ' as empty, 'S' as a stick and 'I' as the material */
    public static Optional<AIBlockShape> layoutOf(String cells) {
        if (cells == null || cells.length() != 9) {
            return Optional.empty();
        }
        int top = 3, left = 3, bottom = -1, right = -1;
        for (int i = 0; i < 9; i++) {
            if (cells.charAt(i) != ' ') {
                top = Math.min(top, i / 3);
                bottom = Math.max(bottom, i / 3);
                left = Math.min(left, i % 3);
                right = Math.max(right, i % 3);
            }
        }
        if (bottom < 0) {
            return Optional.empty();
        }
        List<String> rows = new ArrayList<>();
        for (int row = top; row <= bottom; row++) {
            rows.add(cells.substring(row * 3 + left, row * 3 + right + 1));
        }
        return Optional.ofNullable(LAYOUTS.get(rows));
    }

    /** Whether an idea's id names this shape. The id is the English name, which is what the texture server checks too. */
    public static boolean names(AIBlockShape shape, String id) {
        Pattern word = WORDS.get(shape);
        return word != null && id != null && word.matcher(id).find();
    }

    /** @return the recipe, if the grid is one of these layouts of one full block and the id names that same shape */
    public static Optional<ShapedBlockRecipe> match(ItemStack[] grid, String id) {
        if (grid == null || grid.length != 9) {
            return Optional.empty();
        }
        ItemStack material = null;
        StringBuilder cells = new StringBuilder(9);
        for (ItemStack stack : grid) {
            if (stack == null || stack.isEmpty()) {
                cells.append(' ');
            } else if (stack.is(Items.STICK)) {
                cells.append('S');
            } else if ((isCube(stack) || isVanillaFullBlock(stack)) && (material == null || ItemStack.isSameItemSameTags(material, stack))) {
                material = material == null ? stack.copyWithCount(1) : material;
                cells.append('I');
            } else {
                return Optional.empty();
            }
        }
        // Every layout holds the material, so a matched one has found it.
        ItemStack found = material;
        return layoutOf(cells.toString()).filter(shape -> names(shape, id)).map(shape -> new ShapedBlockRecipe(shape, found));
    }

    /** The material's id becomes a file name, so it must be one the idea step could have handed out. */
    private static boolean isCube(ItemStack stack) {
        CompoundTag data = stack.getTagElement(AIBlockEntity.DATA_KEY);
        return stack.is(BlockInit.AI_BLOCK_ITEM.get()) && data != null && ItemIdSanitizer.isClean(data.getString("id"));
    }

    /**
     * A plain vanilla block that fills its space and is drawn from its model, so its side is a picture to copy.
     * A renamed one is left to the texture server, as its name may mean something else.
     */
    private static boolean isVanillaFullBlock(ItemStack stack) {
        if (!(stack.getItem() instanceof BlockItem item) || stack.getTag() != null && !stack.getTag().isEmpty()) {
            return false;
        }
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(stack.getItem());
        BlockState state = item.getBlock().defaultBlockState();
        return key != null && key.getNamespace().equals("minecraft") && state.getRenderShape() == RenderShape.MODEL
                && Block.isShapeFullBlock(state.getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO));
    }

    /** @return the vanilla block the material is, or null when it is an AI block */
    @Nullable
    public Block vanillaBlock() {
        return material.is(BlockInit.AI_BLOCK_ITEM.get()) ? null : ((BlockItem) material.getItem()).getBlock();
    }

    /** What vanilla's own recipe for the shape makes, as the texture server gives for it too. */
    public int count() {
        return switch (shape) {
            case SLAB, WALL -> 6;
            case STAIRS -> 4;
            case FENCE -> 3;
            default -> throw new IllegalStateException("No vanilla recipe makes " + shape);
        };
    }

    /**
     * Shows the material's own faces. A machine's front is left out: these shapes have no front of their own, so its side goes all the way round.
     *
     * @param vanillaFaces a vanilla material's faces, read by the client that crafts, since only clients have vanilla's pictures; unused for an AI material
     * @throws IOException if this world has no faces for an AI material
     * @throws IllegalArgumentException if a face is not a usable picture
     */
    public GeneratedBlock block(Path textureFolder, @Nullable BlockFaces vanillaFaces) throws IOException {
        Block vanilla = vanillaBlock();
        return vanilla == null ? fromAIBlock(textureFolder) : fromVanilla(vanilla, vanillaFaces);
    }

    /** The vanilla block's properties, each turned into the nearest the mod has, and its own sound. */
    private GeneratedBlock fromVanilla(Block block, BlockFaces read) {
        BlockFaces faces = read.validated();
        BlockState state = block.defaultBlockState();
        return new GeneratedBlock(shape,
                AIBlockTool.of(state),
                AIBlockHardness.nearest(state.getDestroySpeed(EmptyBlockGetter.INSTANCE, BlockPos.ZERO)),
                AIBlockLight.nearest(state.getLightEmission(EmptyBlockGetter.INSTANCE, BlockPos.ZERO)),
                AIBlockSound.of(state.getSoundType()),
                ForgeRegistries.BLOCKS.getKey(block).toString(),
                state.getFlammability(EmptyBlockGetter.INSTANCE, BlockPos.ZERO, Direction.UP) > 0,
                faces.transparency(), count(), false, AIBlockAttach.BELOW, faces);
    }

    private GeneratedBlock fromAIBlock(Path textureFolder) throws IOException {
        CompoundTag data = material.getTagElement(AIBlockEntity.DATA_KEY);
        String materialId = data.getString("id");
        BlockFaces material;
        try {
            material = BlockTextureStore.load(textureFolder, materialId);
        } catch (NoSuchFileException e) {
            throw new GenerationException("This world has lost the pictures of " + materialId + ". Regenerate it, then try again.", e);
        }
        BlockFaces faces = material.shaped().validated();
        return new GeneratedBlock(shape,
                AIBlockTool.byName(data.getString(AIBlockEntity.TOOL_KEY)),
                AIBlockHardness.byName(data.getString(AIBlockEntity.HARDNESS_KEY)),
                AIPlacedBlock.LIGHT.getValue(data.getString(AIPlacedBlock.LIGHT.getName())).orElse(AIBlockLight.NONE),
                AIBlockSound.byName(data.getString(AIBlockEntity.SOUND_KEY)),
                data.contains(AIBlockEntity.SOUND_OF_KEY) ? data.getString(AIBlockEntity.SOUND_OF_KEY) : null,
                data.getBoolean(AIBlockEntity.FLAMMABLE_KEY),
                faces.transparency(), count(), false, AIBlockAttach.BELOW, faces);
    }
}
