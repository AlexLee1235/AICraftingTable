package com.watermelon0117.aicraft.common;

import com.watermelon0117.aicraft.menu.AICraftingTableMenu;
import net.minecraft.world.item.ItemStack;

import java.util.Arrays;
import java.util.stream.Collectors;

public class ItemStackArray {
    public ItemStack[] items=new ItemStack[9];
    public ItemStackArray() {
        for (int i = 0; i < 9; i++) {
            items[i] = ItemStack.EMPTY;
        }
    }
    public ItemStackArray(AICraftingTableMenu menu){
        for (int i = 0; i < 9; i++)
            items[i] = menu.slots.get(i+1).getItem();
    }
    public ItemStackArray(ItemStack[] items) {
        this.items = items;
    }
    public static ItemStackArray deepCopy(ItemStackArray other){
        ItemStackArray r=new ItemStackArray();
        for (int i = 0; i < 9; i++)
            r.items[i] = other.items[i].copy();
        return r;
    }
    public void setItem(int i, ItemStack itemStack) {
        items[i] = itemStack;
    }
    private static String strip(String s){
        return s.replace("[","").replace("]","");
    }

    public String[] getDisplayNames() {
        String[] list = new String[9];
        for (int i = 0; i < 9; i++) {
            if (items[i].isEmpty())
                list[i] = "empty";
            else
                list[i] = strip(items[i].getDisplayName().getString());
        }
        return list;
    }
    public String[] getEnglishNames() {
        String[] list = new String[9];
        for (int i = 0; i < 9; i++) {
            if (items[i].isEmpty())
                list[i] = "empty";
            else
                list[i] = strip(items[i].getDisplayName().getString());
        }
        return list;
    }
    public static String getUniqueNames(String[] input) {
        return Arrays.stream(input)
                .distinct()
                .filter(s -> !s.contentEquals("empty"))
                .collect(Collectors.joining(", "));
    }
    private static boolean stackEqual(ItemStack self, ItemStack other){
        return self.getItem() == other.getItem() && ItemStack.tagMatches(self, other);
    }
    public boolean isEmpty(){
        for (int i = 0; i < 9; i++)
            if(!items[i].isEmpty())
                return false;
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ItemStackArray recipe = (ItemStackArray) o;
        for (int i = 0; i < 9; i++) {
            if(!stackEqual(items[i], recipe.items[i]))
                return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(items);
    }

    @Override
    public String toString() {
        return "Recipe{" +
                "items=" + Arrays.toString(getDisplayNames()) +
                '}';
    }


}
