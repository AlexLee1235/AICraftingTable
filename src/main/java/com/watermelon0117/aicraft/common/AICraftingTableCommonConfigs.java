package com.watermelon0117.aicraft.common;

import net.minecraftforge.common.ForgeConfigSpec;

public class AICraftingTableCommonConfigs {
    public static boolean useOpenAI;
    public static final ForgeConfigSpec.Builder BUILDER=new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.ConfigValue<String> OPENAI_API_KEY;
    public static final ForgeConfigSpec.BooleanValue FORBID_UNBREAKABLE_BLOCKS;
    static {
        BUILDER.push("Configs for AICraftingTable");
        OPENAI_API_KEY=BUILDER.comment("Add APIKEY to use your own OpanAI account").define("OpanAI API Key", "");
        FORBID_UNBREAKABLE_BLOCKS=BUILDER.comment("Set to true to stop new blocks from being unbreakable. A block that would be unbreakable is made as hard as an iron block instead.").define("Forbid Unbreakable Blocks", false);
        BUILDER.pop();
        SPEC=BUILDER.build();
    }
}
