package com.watermelon0117.aicraft.common;

import net.minecraftforge.common.ForgeConfigSpec;

public class AICraftingTableCommonConfigs {
    public static boolean useOpenAI;
    /** Spelled "OpanAI API Key" before 2.0; {@link MisspelledConfigKey} renames it in old files. */
    public static final String OPENAI_API_KEY_NAME = "OpenAI API Key";
    public static final String OPENAI_API_KEY_COMMENT = "Your own OpenAI API key. Leave it empty to use the free AI Crafting Table server. Restart the game after changing it.";
    public static final ForgeConfigSpec.Builder BUILDER=new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.ConfigValue<String> OPENAI_API_KEY;
    public static final ForgeConfigSpec.BooleanValue FORBID_UNBREAKABLE_BLOCKS;
    static {
        BUILDER.push("Configs for AICraftingTable");
        OPENAI_API_KEY=BUILDER.comment(OPENAI_API_KEY_COMMENT).define(OPENAI_API_KEY_NAME, "");
        FORBID_UNBREAKABLE_BLOCKS=BUILDER.comment("Set to true to stop new blocks from being unbreakable. A block that would be unbreakable is made as hard as an iron block instead.").define("Forbid Unbreakable Blocks", false);
        BUILDER.pop();
        SPEC=BUILDER.build();
    }
}
