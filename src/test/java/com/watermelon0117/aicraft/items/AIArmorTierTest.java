package com.watermelon0117.aicraft.items;

import com.watermelon0117.aicraft.gpt.GeneratedItem;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** The item step's tier answer, stored as tools store it, picks the vanilla armor set. */
class AIArmorTierTest {
    private static AIArmorTier of(String answer) {
        return AIArmorTier.ofToolTier(GeneratedItem.tierByte(answer));
    }

    @Test
    void eachTierAnswerGivesItsSet() {
        assertEquals(AIArmorTier.LEATHER, of("wooden"));
        assertEquals(AIArmorTier.CHAIN, of("stone"));
        assertEquals(AIArmorTier.IRON, of("iron"));
        assertEquals(AIArmorTier.GOLD, of("golden"));
        assertEquals(AIArmorTier.DIAMOND, of("diamond"));
        assertEquals(AIArmorTier.NETHERITE, of("netherite"));
        assertEquals(AIArmorTier.NETHERITE, of("bedrock"));
    }

    @Test
    void anUnknownOrMissingAnswerIsStoneAsForTools() {
        assertEquals(AIArmorTier.CHAIN, of("plastic"));
        assertEquals(AIArmorTier.CHAIN, of(null));
    }

    @Test
    void aByteOutsideTheTiersIsTheBase() {
        assertEquals(AIArmorTier.IRON, AIArmorTier.ofToolTier(-1));
        assertEquals(AIArmorTier.IRON, AIArmorTier.ofToolTier(6));
    }
}
