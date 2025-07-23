package com.watermelon0117.aicraft.common;

import com.watermelon0117.aicraft.items.MainItem;
import com.watermelon0117.aicraft.network.CSyncSpecialItemsPacket;
import com.watermelon0117.aicraft.network.PacketHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.util.thread.EffectiveSide;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Side-agnostic read/write API.  Mod code should depend on this only.
 *
 * Use SpecialItemManager.get(level) to obtain the correct impl for the current side.
 */
public final class SpecialItemManager {
    private final boolean isServer;

    SpecialItemManager(boolean isServer) {
        this.isServer = isServer;
    }

    /* -----------------------------------------------------------
     *  1.  Public, side-agnostic API
     * ----------------------------------------------------------- */
    public static SpecialItemManager get(Level level) {
        if (level.isClientSide)
            return ClientSide.INSTANCE;          // client singleton
        return ServerSide.INSTANCE;              // server singleton (initialised in FMLServerStartingEvent)
    }

    public ItemStack getItem(String id) {
        var tag = backing().get(id);
        if (tag == null) throw new RuntimeException("SpecialItemManager: get non-exist item");
        return ItemStack.of(tag);
    }

    public boolean hasItem(String id) {
        return backing().containsKey(id);
    }

    public List<ItemStack> list() {
        return backing().values().stream().map(ItemStack::of).toList();
    }

    public void put(ItemStack stack) {
        checkServer();
        String id = MainItem.getID(stack);
        backing().put(id, stack.save(new CompoundTag()));
        dirtyAndSync();
    }

    public void remove(String id) {
        checkServer();
        if (!backing().containsKey(id)) throw new RuntimeException("SpecialItemManager: remove non-exist item");
        backing().remove(id);
        dirtyAndSync();
    }

    /* -----------------------------------------------------------
     *  2.  Internal helpers
     * ----------------------------------------------------------- */

    private Map<String, CompoundTag> backing() {
        return !isServer
                ? ClientSide.CACHE
                : ServerSide.data().data;
    }

    private void dirtyAndSync() {
        if (isServer) {
            ServerSide.data().setDirty();
            PacketHandler.sendToAllClients(new CSyncSpecialItemsPacket(ServerSide.data().data));
        }
    }

    private void checkServer() {
        if (!isServer)
            throw new UnsupportedOperationException("Cannot mutate SpecialItemManager on the client");
    }

    public static final class ClientSide {
        private static final SpecialItemManager INSTANCE = new SpecialItemManager(false);
        private static final Map<String, CompoundTag> CACHE = new ConcurrentHashMap<>();

        /* Packet handler fills the cache */
        public static void refill(Map<String, CompoundTag> fresh) {
            CACHE.clear();
            CACHE.putAll(fresh);
        }
    }

    public static final class ServerSide {
        private static SpecialItemManager INSTANCE;
        private static MinecraftServer SERVER;

        public static void init(MinecraftServer srv) {
            SERVER = srv;
            INSTANCE = new SpecialItemManager(true);
        }

        public static GlobalData data() {
            return GlobalData.get(SERVER);
        }
    }

    /* SavedData that lives in $WORLD/data/aicraft_special_items.dat */
    public static final class GlobalData extends SavedData {
        private static final String FILE_ID = "aicraft_special_items";
        private static final String TAG_KEY = "items";

        public final Map<String, CompoundTag> data = new ConcurrentHashMap<>();

        static GlobalData get(MinecraftServer srv) {
            ServerLevel overworld = srv.overworld();
            return overworld.getDataStorage().computeIfAbsent(GlobalData::load,
                    GlobalData::new,
                    FILE_ID);
        }

        /* ----- SavedData (serialize) ----- */
        @Override
        public CompoundTag save(CompoundTag nbt) {
            CompoundTag all = new CompoundTag();
            data.forEach((k, v) -> all.put(k, v));
            nbt.put(TAG_KEY, all);
            return nbt;
        }

        static GlobalData load(CompoundTag nbt) {
            GlobalData d = new GlobalData();
            CompoundTag all = nbt.getCompound(TAG_KEY);
            all.getAllKeys().forEach(k -> d.data.put(k, all.getCompound(k)));
            return d;
        }
    }
}