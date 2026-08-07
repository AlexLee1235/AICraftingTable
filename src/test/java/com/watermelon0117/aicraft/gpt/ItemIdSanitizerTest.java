package com.watermelon0117.aicraft.gpt;

import com.mojang.brigadier.StringReader;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ItemIdSanitizerTest {
    @Test
    void preservesEveryAllowedCharacter() {
        String id = "AZ az 09 Sword +1.5-Test";
        assertEquals(id, ItemIdSanitizer.sanitize(id));
    }

    @Test
    void removesEveryCharacterOutsideTheAllowlist() {
        String id = "Miner's_Hammer<>:\"/\\|?*#$%&(),;=@[]^`{}~";
        assertEquals("MinersHammer", ItemIdSanitizer.sanitize(id));
    }

    @Test
    void removesUnicodeEmojiControlAndFormatCharacters() {
        String id = "A\nB" + (char) 0 + "C\u200BD\u4E2D\u6587\uD83D\uDE00";
        assertEquals("ABCD", ItemIdSanitizer.sanitize(id));
    }

    @Test
    void matchesDocumentedExamples() {
        assertEquals("Miners Hammer", ItemIdSanitizer.sanitize("Miner's Hammer"));
        assertEquals("IronBlade", ItemIdSanitizer.sanitize("Iron_Blade:*?"));
        assertEquals("Sword +1.5", ItemIdSanitizer.sanitize("Sword +1.5"));
    }

    @Test
    void rejectsBatchWhenAnIdBecomesBlank() {
        ItemIdeas ideas = ideas("'_\u4E2D\u6587\uD83D\uDE00", "Valid", "Other");
        assertThrows(IllegalArgumentException.class, ideas::sanitizeIds);
    }

    @Test
    void rejectsBatchWhenAnIdIsNull() {
        ItemIdeas ideas = ideas("Valid", null, "Other");
        assertThrows(IllegalArgumentException.class, ideas::sanitizeIds);
    }

    @Test
    void allowsDuplicatesAfterSanitizing() {
        ItemIdeas ideas = ideas("A/B", "AB", "Third").sanitizeIds();
        assertArrayEquals(new String[]{"AB", "AB", "Third"}, ideas.id);
    }

    @Test
    void sanitizedIdRoundTripsThroughExistingCommandEncoding() {
        String storedId = ItemIdSanitizer.sanitize("Miner's Sword +1.5");
        String commandArgument = storedId.replace(' ', '_');

        String parsed = new StringReader(commandArgument).readUnquotedString().replace('_', ' ');

        assertEquals(storedId, parsed);
    }

    private static ItemIdeas ideas(String first, String second, String third) {
        ItemIdeas ideas = new ItemIdeas();
        ideas.id = new String[]{first, second, third};
        return ideas;
    }
}
