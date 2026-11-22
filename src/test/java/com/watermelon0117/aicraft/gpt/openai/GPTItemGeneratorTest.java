package com.watermelon0117.aicraft.gpt.openai;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The texture server asks the same, word for word (test/api.test.js), as an own key runs its flow here: the server's
 * questions for a mod that wears armor ("armor": true), so the armor_slot question ends the list.
 */
class GPTItemGeneratorTest {
    private static String questions(String basis) {
        return "given a minecraft item, please answer following questions:\n"
                + "visual_description(one sentence describe what the finished item(not block) itself looks like in-game. " + basis + ")?\n"
                + "is_shapeless_crafting(true if is mixing, combine, etc)?\n"
                + "is_tool?\n"
                + "tier(equivalent in wooden, stone, iron, diamond, netherite, bedrock, golden)?\n"
                + "is_suitable_for_breaking_stone(like pickaxe)?\n"
                + "is_suitable_for_breaking_woods(like axe)?\n"
                + "is_suitable_for_breaking_dirt(like shovel)?\n"
                + "is_suitable_to_plow(like hoe)?\n"
                + "is_melee_weapon?\n"
                + "damage(low, normal, high)?\n"
                + "attack_speed(slow, normal, fast)?\n"
                + "is_edible?\n"
                + "nutrition_value(0 to 20, apple is 4 for reference)?\n"
                + "food_is_solid_or_liquid?\n"
                + "armor_slot(helmet, chestplate, leggings, boots, or none if it is not worn as armor; "
                + "armor also answers tier with one of the tier words, leather being wooden and chainmail stone)?\n";
    }

    @Test
    void asksAboutACraftedItemByItsMaterialsWordForWordAsBefore() {
        String[] grid = {"Iron Ingot", "Iron Ingot", "Iron Ingot", "empty", "Stick", "empty", "empty", "Stick", "empty"};
        assertEquals(questions("Based on the arrangement of materials")
                        + "Item: Test Item\nRecipe:\n"
                        + "\tTop Left: Iron Ingot\n\tTop: Iron Ingot\n\tTop Right: Iron Ingot\n"
                        + "\tLeft: empty\n\tCenter: Stick\n\tRight: empty\n"
                        + "\tBottom Left: empty\n\tBottom: Stick\n\tBottom Right: empty\n"
                        + "\nplease answer in json format",
                GPTItemGenerator.buildPrompt("Test Item", grid));
    }

    /** Shown nine empty slots, gpt-4.1 described a Suluski as no item at all, since the recipe was empty. */
    @Test
    void asksAboutAnItemMadeByNameAloneAsANewItemWithNoRecipe() {
        String[] grid = new String[9];
        Arrays.fill(grid, "empty");
        assertEquals(questions("It is a new item: imagine its look from its name") + "Item: Suluski\n\nplease answer in json format",
                GPTItemGenerator.buildPrompt("Suluski", grid));
    }
}
