package com.watermelon0117.aicraft.blockentities;

import com.watermelon0117.aicraft.AICraftingTable;
import com.watermelon0117.aicraft.init.BlockEntityInit;
import com.watermelon0117.aicraft.init.ItemInit;
import com.watermelon0117.aicraft.menu.AICraftingTableMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.TickingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AICraftingTableBlockEntity extends BlockEntity implements MenuProvider {
    private int progress=0;
    public int taskID=0;
    private final ItemStackHandler inventory = new ItemStackHandler(10) {
        @Override
        protected void onContentsChanged(int slot) {
            super.onContentsChanged(slot);
            AICraftingTableBlockEntity.this.setChanged();
        }
    };
    private final LazyOptional<ItemStackHandler> optional = LazyOptional.of(()->this.inventory);
    public AICraftingTableBlockEntity(BlockPos p_155229_, BlockState p_155230_) {
        super(BlockEntityInit.AI_CRAFTING_TABLE_BE.get(), p_155229_, p_155230_);
    }


    @Override
    public void load(CompoundTag nbt) {
        super.load(nbt);
        var tag = nbt.getCompound(AICraftingTable.MODID);
        this.inventory.deserializeNBT(tag.getCompound("Inventory"));
        this.progress=tag.getInt("Progress");
    }
    @Override
    protected void saveAdditional(CompoundTag nbt) {
        super.saveAdditional(nbt);
        var tag=new CompoundTag();
        tag.put("Inventory", this.inventory.serializeNBT());
        tag.putInt("Progress", this.progress);
        nbt.put(AICraftingTable.MODID, tag);
    }
    @Override
    public CompoundTag getUpdateTag() {
        var tag= new CompoundTag();
        saveAdditional(tag);
        return tag;
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
    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap) {
        return cap == ForgeCapabilities.ITEM_HANDLER ? this.optional.cast() : super.getCapability(cap);
    }
    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        this.optional.invalidate();
    }
    public ItemStackHandler getInventory(){
        return this.inventory;
    }
    public LazyOptional<ItemStackHandler> getOptional(){
        return this.optional;
    }
    public NonNullList<ItemStack> getDropList(){
        NonNullList<ItemStack> ret=NonNullList.create();
        for (int i = 1; i < inventory.getSlots(); i++) {
            ret.add(inventory.getStackInSlot(i));
        }
        return ret;
    }
    public void tick() {
        if (level != null && !level.isClientSide) {
            if (this.progress != 0 && this.progress<580)
                this.progress += 1;
            level.sendBlockUpdated(getBlockPos(), level.getBlockState(getBlockPos()), level.getBlockState(getBlockPos()), Block.UPDATE_ALL);
        }
    }

    public int getProgress() {
        return progress;
    }
    public void setProgress(int v) {
        progress = v;
        level.sendBlockUpdated(getBlockPos(), level.getBlockState(getBlockPos()), level.getBlockState(getBlockPos()), Block.UPDATE_ALL);
    }
}
