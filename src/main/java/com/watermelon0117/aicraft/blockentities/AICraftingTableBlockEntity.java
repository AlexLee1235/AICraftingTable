package com.watermelon0117.aicraft.blockentities;

import com.watermelon0117.aicraft.init.BlockEntityInit;
import com.watermelon0117.aicraft.menu.AICraftingTableMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class AICraftingTableBlockEntity extends BlockEntity implements MenuProvider {
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

    @Override
    public Component getDisplayName() {
        return Component.literal("AI Crafting Table");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new AICraftingTableMenu(id,inventory,this);
    }
}
