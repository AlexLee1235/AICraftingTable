package com.watermelon0117.aicraft.recipes;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.world.item.ItemStack;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class SpecialItemManager {
    private static final File DIR = new File("C:\\achieve\\AICraftingTable\\data\\tags");
    private static final Map<String, CompoundTag> itemMap = new HashMap<>();

    /** Save item to file and map */
    public static void addItem(String name, ItemStack itemStack) {
        CompoundTag tag = itemStack.save(new CompoundTag());
        itemMap.put(name, tag);
        saveToFile(name, tag);
    }

    /** Retrieve item by name */
    public static ItemStack getItem(String name) {
        CompoundTag tag = itemMap.get(name);
        return tag != null ? ItemStack.of(tag) : ItemStack.EMPTY;
    }
    public static ArrayList<ItemStack> getAllItems() {
        ArrayList<ItemStack> result = new ArrayList<>();
        for (CompoundTag tag : itemMap.values()) {
            result.add(ItemStack.of(tag));
        }
        return result;
    }
    /** Load all items from files */
    public static void loadFromFile() {
        itemMap.clear();
        if (!DIR.exists()) return;
        File[] files = DIR.listFiles((dir, name) -> name.endsWith(".nbt"));
        if (files == null) return;

        for (File file : files) {
            String name = file.getName().replaceFirst("\\.nbt$", "");
            try (DataInputStream in = new DataInputStream(new FileInputStream(file))) {
                CompoundTag tag = NbtIo.readCompressed(in);
                itemMap.put(name, tag);
            } catch (IOException ignored) {
                // skip broken files
            }
        }
    }

    /** Save NBT to file */
    private static void saveToFile(String name, CompoundTag tag) {
        ensureDir();
        File file = new File(DIR, name + ".nbt");
        try (DataOutputStream out = new DataOutputStream(new FileOutputStream(file))) {
            NbtIo.writeCompressed(tag, out);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save item: " + name, e);
        }
    }

    /** Ensure save directory exists */
    private static void ensureDir() {
        if (!DIR.exists() && !DIR.mkdirs()) {
            throw new RuntimeException("Failed to create directory: " + DIR);
        }
    }
}
