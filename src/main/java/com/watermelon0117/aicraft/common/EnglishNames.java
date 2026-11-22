package com.watermelon0117.aicraft.common;

import net.minecraft.locale.Language;
import net.minecraftforge.fml.loading.FMLEnvironment;

import javax.annotation.Nullable;

/**
 * English text for names sent to the AI, whatever language the player picked.
 * A world on the client runs its server with the player's language, so the client reads its packs in English as well.
 */
public final class EnglishNames {
    private static volatile Language client;

    private EnglishNames() {}

    /** Set on every client resource reload, with the same packs and mods the client's own language comes from. */
    public static void setClient(Language english) {
        client = english;
    }

    /** English text for a translation key, or null when no English text is loaded for it. A dedicated server's own language is English. */
    @Nullable
    public static String get(String key) {
        Language language = client != null ? client : FMLEnvironment.dist.isDedicatedServer() ? Language.getInstance() : null;
        return language != null && language.has(key) ? language.getOrDefault(key) : null;
    }
}
