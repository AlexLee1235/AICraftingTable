package com.watermelon0117.aicraft.gpt;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.watermelon0117.aicraft.ImageGridProcessor;
import com.watermelon0117.aicraft.blockentities.AICraftingTableBlockEntity;
import com.watermelon0117.aicraft.init.ItemInit;
import com.watermelon0117.aicraft.items.MainItem;
import com.watermelon0117.aicraft.recipes.Recipe;
import com.watermelon0117.aicraft.recipes.RecipeManager;
import com.watermelon0117.aicraft.recipes.SpecialItemManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tiers;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

public class GPTItemGenerator {
    private static final String inst=
            "given a minecraft item, please answer following questions:\n" +
            "is_shapeless_crafting(true if item is structured or false if mixing)?\n" +
            "is_tool?\n" +
            "tier(equivalent in wooden, stone, iron, diamond, netherite, golden)?\n" +
            "is_suitable_for_breaking_stone(like pickaxe)?\n" +
            "is_suitable_for_breaking_woods(like axe)?\n" +
            "is_suitable_for_breaking_dirts(like shovel)?\n" +
            "is_suitable_to_plow(like hoe)?\n" +
            "is_melee_weapon?\n" +
            "damage(low, normal, high)?\n" +
            "attack_speed(slow, normal, fast)?\n" +
            "is_edible?\n" +
            "nutrition_value(0 to 20, apple is 4 for reference)?\n" +
            "food_is_solid_or_liquid?\n";
    private final OpenAIHttpClient client = new OpenAIHttpClient("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",
            "gpt-4o",  //gpt-4o gpt-4.1
            0.0,
            1024,
            "",
            "json_object");
    private final Gson gson;
    private final GPTImageGenerator imgClient = new GPTImageGenerator();
    public GPTItemGenerator(){
        this.gson = new GsonBuilder()
                // map Java camelCase ↔︎ JSON snake_case automatically
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .create();
    }
    private static String buildPrompt(String name, String[] recipe) {
        //String.format("Recipe: [[%s, %s, %s], [%s, %s, %s], [%s, %s, %s]]\n", recipe[0], recipe[1], recipe[2], recipe[3], recipe[4], recipe[5], recipe[6], recipe[7], recipe[8])
        return inst + String.format("Item: %s\n", name) +
                "Made of: " + Recipe.getUniqueNames(recipe) +
                "\nplease answer in json format";
    }
    private static void applyTexture(byte[] bytes, String name) {
        try {
            Files.write(Path.of("C:\\achieve\\AICraftingTable\\process\\source.png"), bytes);
            Files.write(Path.of("C:\\achieve\\AICraftingTable\\image\\" + name + ".png"), bytes);
            BufferedImage txt = ImageGridProcessor.process("C:\\achieve\\AICraftingTable\\process\\source.png");
            ImageGridProcessor.saveImage(txt, "C:\\achieve\\AICraftingTable\\data\\textures\\" + name + ".png");
            MainItem.renderer.loadNewFile(name);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public CompletableFuture<ItemStack> generate(String name, String[] recipe, AICraftingTableBlockEntity be, Predicate<AICraftingTableBlockEntity> predicate) {
        String prompt=buildPrompt(name,recipe);
        System.out.println(prompt);
        return client.chat(prompt).thenCompose(rawJson -> {
            System.out.println(rawJson);
            ItemResult json = gson.fromJson(rawJson, ItemResult.class);
            ItemStack itemStack = json.is_edible ? new ItemStack(ItemInit.MAIN_FOOD_ITEM.get()) : new ItemStack(ItemInit.MAIN_ITEM.get());
            itemStack.getOrCreateTag().putString("texture", name);
            setTags(itemStack, json);
            return imgClient.generateItem(name, recipe/*prompt from json*/).thenApply(textureBytes -> {
                if (predicate.test(be)) {
                    applyTexture(textureBytes, name);
                    RecipeManager.addRecipe(name, recipe, json.is_shapeless_crafting);
                    SpecialItemManager.addItem(name, itemStack);
                }
                return itemStack;
            });
        });
    }
    private static void setTags(ItemStack stack, ItemResult json) {
        CompoundTag tag = stack.getOrCreateTag();

        if (json.is_suitable_for_breaking_stone)
            tag.putBoolean("isPickaxe", true);
        if (json.is_suitable_for_breaking_woods)
            tag.putBoolean("isAxe", true);
        if (json.is_suitable_for_breaking_dirts)
            tag.putBoolean("isShovel", true);
        if (json.is_suitable_to_plow)
            tag.putBoolean("isHoe", true);
        if (json.is_tool || json.is_melee_weapon || json.is_suitable_for_breaking_stone || json.is_suitable_for_breaking_woods || json.is_suitable_for_breaking_dirts || json.is_suitable_to_plow) {
            tag.putByte("tier", getTier(json));
            //sword pickaxe axe shovel hoe
            //double[] num1={3,1,7,1.5,-1};
            //double[] num2={-2.4,-2.8,-3.2-3.0,-2.0};
            double damage = switch (json.damage == null ? "normal" : json.damage) {
                case "low"    -> 1;
                case "high"   -> 7;
                case "normal" -> 3;
                default       -> 3;
            };

            double attackSpeed = switch (json.attack_speed == null ? "normal" : json.attack_speed) {
                case "fast"   -> -2.0;
                case "slow"   -> -3.2;
                case "normal" -> -2.5;
                default       -> -2.5;
            };
            tag.putDouble("attackDamage", damage);
            tag.putDouble("attackSpeed", attackSpeed);
        }
        //food
        if(json.is_edible){
            tag.putByte("nutrition", (byte) json.nutrition_value);
            if(json.food_is_solid_or_liquid!=null &&json.food_is_solid_or_liquid.contentEquals("liquid"))
                tag.putBoolean("isDrink", true);
            else
                tag.putBoolean("isFood", true);
        }
    }
    private static byte getTier(ItemResult json) {
        if (json.tier == null) {
            System.out.println("found tool/weapon without tier!");
            return (byte) Tiers.STONE.ordinal();
        }
        if (json.tier.contentEquals("wooden"))
            return (byte) Tiers.WOOD.ordinal();
        else if (json.tier.contentEquals("stone"))
            return (byte) Tiers.STONE.ordinal();
        else if (json.tier.contentEquals("iron"))
            return (byte) Tiers.IRON.ordinal();
        else if (json.tier.contentEquals("diamond"))
            return (byte) Tiers.DIAMOND.ordinal();
        else if (json.tier.contentEquals("netherite"))
            return (byte) Tiers.NETHERITE.ordinal();
        else if (json.tier.contentEquals("golden"))
            return (byte) Tiers.GOLD.ordinal();
        return (byte) Tiers.STONE.ordinal();
    }
    private static final class ItemResult {
        boolean is_shapeless_crafting;
        boolean is_tool;
        String tier;
        boolean is_suitable_for_breaking_stone;
        boolean is_suitable_for_breaking_woods;
        boolean is_suitable_for_breaking_dirts;
        boolean is_suitable_to_plow;
        boolean is_melee_weapon;
        String damage;
        String attack_speed;
        boolean is_edible;
        int nutrition_value;
        String food_is_solid_or_liquid;
    }
}
