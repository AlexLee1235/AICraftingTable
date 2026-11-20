package com.watermelon0117.aicraft.blockentities;

import com.watermelon0117.aicraft.blocks.AIBlockHardness;
import com.watermelon0117.aicraft.blocks.AIBlockSound;
import com.watermelon0117.aicraft.blocks.AIBlockTool;
import com.watermelon0117.aicraft.blocks.PlantBounds;
import com.watermelon0117.aicraft.init.BlockEntityInit;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/** Holds the same aicraft tag as the item it was placed from, so the block keeps its identity and drops by itself. */
public class AIBlockEntity extends BlockEntity {
    public static final String DATA_KEY = "aicraft";
    public static final String TOOL_KEY = "tool";
    public static final String HARDNESS_KEY = "hardness";
    public static final String SOUND_KEY = "sound";
    public static final String FLAMMABLE_KEY = "flammable";
    /** The vanilla block a block was made from, whose own sound it keeps, as most vanilla sounds are not among the server's. */
    public static final String SOUND_OF_KEY = "sound_of";
    /** Where a plant's picture is drawn, as {@link PlantBounds} keeps it. */
    public static final String PLANT_BOUNDS_KEY = "plant_bounds";
    public static final ModelProperty<String> ID_PROPERTY = new ModelProperty<>();
    /** How many clear rows a plant's picture has above what it draws. */
    public static final ModelProperty<Integer> PICTURE_TOP_PROPERTY = new ModelProperty<>();

    private CompoundTag aicraft = new CompoundTag();
    private AIBlockTool tool = AIBlockTool.byName("");
    private AIBlockHardness hardness = AIBlockHardness.byName("");
    private AIBlockSound sound = AIBlockSound.byName("");
    private boolean flammable;
    @Nullable
    private SoundType soundOf;
    @Nullable
    private VoxelShape outline;
    @Nullable
    private VoxelShape outlineAtTop;
    private int pictureTop;

    public AIBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityInit.AI_BLOCK_BE.get(), pos, state);
    }

    @Nullable
    public String getTextureId() {
        return aicraft.contains("id", Tag.TAG_STRING) ? aicraft.getString("id") : null;
    }

    public AIBlockTool tool() {
        return tool;
    }

    public AIBlockHardness hardness() {
        return hardness;
    }

    public AIBlockSound sound() {
        return sound;
    }

    public boolean flammable() {
        return flammable;
    }

    /** The vanilla block's own sound when the block was made from one, otherwise the server's choice. */
    public SoundType soundType() {
        return soundOf != null ? soundOf : sound.soundType();
    }

    /**
     * A plant's outline from where its picture is drawn, or null when the tag holds no usable bounds.
     *
     * @param atTop for a plant whose picture is shown against the top of its cell
     */
    @Nullable
    public VoxelShape outline(boolean atTop) {
        return atTop ? outlineAtTop : outline;
    }

    public CompoundTag getAicraftTag() {
        return aicraft;
    }

    public void setAicraftTag(CompoundTag tag) {
        readAicraftTag(tag);
        setChanged();
        requestModelDataUpdate();
    }

    /** Parsed once, because mining, explosions, steps and fire ask many times. */
    private void readAicraftTag(CompoundTag tag) {
        aicraft = tag.copy();
        tool = AIBlockTool.byName(aicraft.getString(TOOL_KEY));
        hardness = AIBlockHardness.byName(aicraft.getString(HARDNESS_KEY));
        sound = AIBlockSound.byName(aicraft.getString(SOUND_KEY));
        flammable = aicraft.getBoolean(FLAMMABLE_KEY);
        soundOf = soundOfBlock(aicraft.getString(SOUND_OF_KEY));
        PlantBounds bounds = PlantBounds.fromTag(aicraft.getIntArray(PLANT_BOUNDS_KEY));
        outline = outlineOf(bounds);
        outlineAtTop = bounds == null ? null : outlineOf(bounds.atTop());
        pictureTop = bounds == null ? 0 : bounds.top();
    }

    @Nullable
    private static VoxelShape outlineOf(@Nullable PlantBounds bounds) {
        if (bounds == null) {
            return null;
        }
        int[] box = bounds.box();
        return Block.box(box[0], box[1], box[2], box[3], box[4], box[5]);
    }

    /** @return the sound of the block with this id, or null when there is no such block */
    @Nullable
    private static SoundType soundOfBlock(String id) {
        ResourceLocation key = id.isEmpty() ? null : ResourceLocation.tryParse(id);
        Block block = key == null ? null : ForgeRegistries.BLOCKS.getValue(key);
        return block == null || block == Blocks.AIR ? null : block.defaultBlockState().getSoundType();
    }

    @Override
    public void load(CompoundTag nbt) {
        super.load(nbt);
        readAicraftTag(nbt.getCompound(DATA_KEY));
    }

    @Override
    protected void saveAdditional(CompoundTag nbt) {
        super.saveAdditional(nbt);
        if (!aicraft.isEmpty()) {
            nbt.put(DATA_KEY, aicraft.copy());
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        super.handleUpdateTag(tag);
        requestModelDataUpdate();
    }

    /** Vanilla only loads the data here, so a block whose id changes keeps its old mesh unless the section is rebuilt. */
    @Override
    public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet) {
        String before = getTextureId();
        super.onDataPacket(connection, packet);
        requestModelDataUpdate();
        if (level != null && !Objects.equals(before, getTextureId())) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public @NotNull ModelData getModelData() {
        String id = getTextureId();
        return id == null ? ModelData.EMPTY : ModelData.builder().with(ID_PROPERTY, id).with(PICTURE_TOP_PROPERTY, pictureTop).build();
    }
}
