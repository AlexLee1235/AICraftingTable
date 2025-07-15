package com.watermelon0117.aicraft.common;

import net.minecraftforge.common.ForgeConfigSpec;

public class AICraftingTableCommonConfigs {
    public static boolean useOpenAI;
    public static final ForgeConfigSpec.Builder BUILDER=new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.ConfigValue<String> OPENAI_API_KEY;
    static {
        BUILDER.push("Configs for AICraftingTable");
        OPENAI_API_KEY=BUILDER.comment("Add APIKEY to use your own OpanAI account").define("OpanAI API Key", "");
        BUILDER.pop();
        SPEC=BUILDER.build();
    }
}
