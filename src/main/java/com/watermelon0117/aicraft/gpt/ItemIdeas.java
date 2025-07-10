package com.watermelon0117.aicraft.gpt;

import com.watermelon0117.aicraft.gpt.opanai.BaseGPTIdeaGeneratorwParser;
import net.minecraft.network.FriendlyByteBuf;

public class ItemIdeas {
    public String[] id = new String[3];
    public String[] names = new String[3];

    public ItemIdeas() {
        id[0] = id[1] = id[2] = names[0] = names[1] = names[2] = "";
    }

    public ItemIdeas(BaseGPTIdeaGeneratorwParser.ItemResult itemResult) {
        id[0] = itemResult.items.get(0);
        id[1] = itemResult.items.get(1);
        id[2] = itemResult.items.get(2);
        names[0] = itemResult.translated.get(0);
        names[1] = itemResult.translated.get(1);
        names[2] = itemResult.translated.get(2);
    }

    public ItemIdeas(FriendlyByteBuf buf) {
        for (int i = 0; i < 3; i++)
            id[i] = buf.readUtf();
        for (int i = 0; i < 3; i++)
            names[i] = buf.readUtf();
    }

    public void write(FriendlyByteBuf buf) {
        for (int i = 0; i < 3; i++)
            buf.writeUtf(id[i]);
        for (int i = 0; i < 3; i++)
            buf.writeUtf(names[i]);
    }
}
