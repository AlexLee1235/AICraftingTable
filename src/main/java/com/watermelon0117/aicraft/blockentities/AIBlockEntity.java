package com.watermelon0117.aicraft.blockentities;

import com.watermelon0117.aicraft.blocks.AIBlockHardness;
import com.watermelon0117.aicraft.blocks.AIBlockTool;
import com.watermelon0117.aicraft.init.BlockEntityInit;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/** Holds the same aicraft tag as the item it was placed from, so the block keeps its identity and drops by itself. */
public class AIBlockEntity extends BlockEntity {
    public static final String DATA_KEY = "aicraft";
    public static final String TOOL_KEY = "tool";
    public static final String HARDNESS_KEY = "hardness";
    public static final ModelProperty<String> ID_PROPERTY = new ModelProperty<>();

    private CompoundTag aicraft = new CompoundTag();
    private AIBlockTool tool = AIBlockTool.byName("");
    private AIBlockHardness hardness = AIBlockHardness.byName("");

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

    public CompoundTag getAicraftTag() {
        return aicraft;
    }

    public void setAicraftTag(CompoundTag tag) {
        readAicraftTag(tag);
        setChanged();
        requestModelDataUpdate();
    }

    /** Parsed once, because mining and explosions ask many times. */
    private void readAicraftTag(CompoundTag tag) {
        aicraft = tag.copy();
        tool = AIBlockTool.byName(aicraft.getString(TOOL_KEY));
        hardness = AIBlockHardness.byName(aicraft.getString(HARDNESS_KEY));
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
        return id == null ? ModelData.EMPTY : ModelData.builder().with(ID_PROPERTY, id).build();
    }
}
