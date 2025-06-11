package com.watermelon0117.aicraft.blockentities;

import com.watermelon0117.aicraft.init.BlockEntityInit;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class AICraftingTableBlockEntity extends BlockEntity {
    public AICraftingTableBlockEntity(BlockPos p_155229_, BlockState p_155230_) {
        super(BlockEntityInit.AI_CRAFTING_TABLE_BE.get(), p_155229_, p_155230_);
    }

    private void read(CompoundTag nbt) {}
    private CompoundTag write(CompoundTag nbt) {
        return nbt;
    }
    @Override
    public void load(CompoundTag nbt) {
        read(nbt);
        super.load(nbt);
    }
    @Override
    protected void saveAdditional(CompoundTag nbt) {
        super.saveAdditional(nbt);
        write(nbt);
    }
    @Override
    public CompoundTag getUpdateTag() {
        return write(new CompoundTag());
    }
    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        super.onDataPacket(net, pkt);
    }
}
