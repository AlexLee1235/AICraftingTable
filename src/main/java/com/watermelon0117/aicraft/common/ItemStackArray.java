package com.watermelon0117.aicraft.common;

import com.watermelon0117.aicraft.init.ItemInit;
import com.watermelon0117.aicraft.items.MainItem;
import com.watermelon0117.aicraft.menu.AICraftingTableMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

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
        if (items == null || items.length != 9)
            throw new IllegalArgumentException();
        this.items = items;
    }
    public static ItemStackArray deepCopy(ItemStackArray other){
        ItemStackArray r=new ItemStackArray();
        for (int i = 0; i < 9; i++)
            r.items[i] = other.items[i].copy();
        return r;
    }

    public String[] getDisplayNames() {
        String[] list = new String[9];
        for (int i = 0; i < 9; i++) {
            if (items[i].isEmpty())
                list[i] = "empty";
            else {
                if (MainItem.isMainItem(items[i]))
                    list[i] = MainItem.getID(items[i]);
                else
                    list[i] = toTitleCase(ForgeRegistries.ITEMS.getKey(items[i].getItem()).getPath());//strip(items[i].getDisplayName().getString());
            }
        }
        return list;
    }
    public static String toTitleCase(String input) {
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true; // capitalize the first character
        for (char c : input.toCharArray()) {
            if (c == '_') {
                result.append(' ');
                capitalizeNext = true; // next char should be capitalized
            } else {
                if (capitalizeNext) {
                    result.append(Character.toUpperCase(c));
                    capitalizeNext = false;
                } else {
                    result.append(c);
                }
            }
        }
        return result.toString();
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
            if(!ItemStack.isSameItemSameTags(items[i], recipe.items[i]))
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
