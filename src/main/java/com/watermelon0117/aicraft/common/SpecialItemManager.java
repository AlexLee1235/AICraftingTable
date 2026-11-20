package com.watermelon0117.aicraft.common;

import com.watermelon0117.aicraft.items.MainItem;
import com.watermelon0117.aicraft.network.CSyncSpecialItemsPacket;
import com.watermelon0117.aicraft.network.PacketHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Side-agnostic read/write API.  Mod code should depend on this only.
 *
 * Use SpecialItemManager.get(level) to obtain the correct impl for the current side.
 *
 * Both sides hold the items oldest first, in the order they were made, and list them newest first.
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

    /** @return every AI item, the one made most recently first */
    public List<ItemStack> list() {
        return newestFirst(backing());
    }

    /** A new id goes last, as the newest. A regenerated id keeps its place, as putting an existing key leaves it where it is. */
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
                ? ClientSide.cache
                : ServerSide.data().data;
    }

    private static List<ItemStack> newestFirst(Map<String, CompoundTag> oldestFirst) {
        List<ItemStack> stacks = new ArrayList<>(oldestFirst.size());
        oldestFirst.values().forEach(tag -> stacks.add(ItemStack.of(tag)));
        Collections.reverse(stacks);
        return Collections.unmodifiableList(stacks);
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
        /** Oldest first, as the server sent it. Replaced whole, so a reader sees one sync or the next and never a mix. */
        private static volatile Map<String, CompoundTag> cache = Map.of();

        /* Packet handler fills the cache */
        public static void refill(Map<String, CompoundTag> fresh) {
            cache = Collections.unmodifiableMap(new LinkedHashMap<>(fresh));
        }

        public static List<ItemStack> list() {
            return newestFirst(cache);
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
        /** The ids oldest first. A CompoundTag keeps no order, and {@code items} stays one so older copies of the mod still read it. */
        private static final String ORDER_KEY = "order";

        /**
         * Oldest first. Only the server thread touches it: every packet is handled there, and generation finishes through
         * {@code server.execute}.
         */
        public final Map<String, CompoundTag> data = new LinkedHashMap<>();

        static GlobalData get(MinecraftServer srv) {
            ServerLevel overworld = srv.overworld();
            Path aicraftFolder = srv.getWorldPath(LevelResource.ROOT).resolve("aicraft");
            return overworld.getDataStorage().computeIfAbsent(nbt -> load(nbt, aicraftFolder),
                    GlobalData::new,
                    FILE_ID);
        }

        /* ----- SavedData (serialize) ----- */
        @Override
        public CompoundTag save(CompoundTag nbt) {
            CompoundTag all = new CompoundTag();
            ListTag order = new ListTag();
            data.forEach((k, v) -> {
                all.put(k, v);
                order.add(StringTag.valueOf(k));
            });
            nbt.put(TAG_KEY, all);
            nbt.put(ORDER_KEY, order);
            return nbt;
        }

        static GlobalData load(CompoundTag nbt, Path aicraftFolder) {
            GlobalData d = new GlobalData();
            CompoundTag all = nbt.getCompound(TAG_KEY);
            ListTag order = nbt.getList(ORDER_KEY, Tag.TAG_STRING);
            List<String> saved = new ArrayList<>(order.size());
            for (int i = 0; i < order.size(); i++) {
                saved.add(order.getString(i));
            }
            for (String id : CreationOrder.oldestFirst(all.getAllKeys(), saved, id -> CreationOrder.firstFileTime(aicraftFolder, id))) {
                d.data.put(id, all.getCompound(id));
            }
            // Saves a guessed place at once, as the files it came from change when the world is copied.
            if (!saved.containsAll(all.getAllKeys())) {
                d.setDirty();
            }
            return d;
        }
    }
}
