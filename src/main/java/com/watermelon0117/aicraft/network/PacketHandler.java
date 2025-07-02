package com.watermelon0117.aicraft.network;

import com.watermelon0117.aicraft.AICraftingTable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class PacketHandler {
    public static final SimpleChannel INSTANCE = NetworkRegistry.ChannelBuilder.named(
            new ResourceLocation(AICraftingTable.MODID, "main"))
            .serverAcceptedVersions((s)->true)
            .clientAcceptedVersions((s)->true)
            .networkProtocolVersion(()->"1")
            .simpleChannel();
    public static void register(){
        int index=0;
        INSTANCE.messageBuilder(SSelectIdeaPacket.class, index++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SSelectIdeaPacket::encode)
                .decoder(SSelectIdeaPacket::new)
                .consumerMainThread(SSelectIdeaPacket::handle)
                .add();
        INSTANCE.messageBuilder(SGenIdeaPacket.class, index++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SGenIdeaPacket::encode)
                .decoder(SGenIdeaPacket::new)
                .consumerMainThread(SGenIdeaPacket::handle)
                .add();
        INSTANCE.messageBuilder(CGenIdeaPacket.class, index++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(CGenIdeaPacket::encode)
                .decoder(CGenIdeaPacket::new)
                .consumerMainThread(CGenIdeaPacket::handle)
                .add();
        INSTANCE.messageBuilder(SPlaceRecipePacket.class, index++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SPlaceRecipePacket::encode)
                .decoder(SPlaceRecipePacket::new)
                .consumerMainThread(SPlaceRecipePacket::handle)
                .add();
        INSTANCE.messageBuilder(CPlaceGhostRecipePacket.class, index++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(CPlaceGhostRecipePacket::encode)
                .decoder(CPlaceGhostRecipePacket::new)
                .consumerMainThread(CPlaceGhostRecipePacket::handle)
                .add();
        INSTANCE.messageBuilder(CSyncSpecialItemsPacket.class, index++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(CSyncSpecialItemsPacket::encode)
                .decoder(CSyncSpecialItemsPacket::new)
                .consumerMainThread(CSyncSpecialItemsPacket::handle)
                .add();
        INSTANCE.messageBuilder(CSendAllTexturePacket.class, index++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(CSendAllTexturePacket::encode)
                .decoder(CSendAllTexturePacket::new)
                .consumerMainThread(CSendAllTexturePacket::handle)
                .add();
        INSTANCE.messageBuilder(CAddTexturePacket.class, index++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(CAddTexturePacket::encode)
                .decoder(CAddTexturePacket::new)
                .consumerMainThread(CAddTexturePacket::handle)
                .add();
        INSTANCE.messageBuilder(CSyncRecipesPacket.class, index++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(CSyncRecipesPacket::encode)
                .decoder(CSyncRecipesPacket::new)
                .consumerMainThread(CSyncRecipesPacket::handle)
                .add();
    }

    public static void sendToServer(Object msg){
        INSTANCE.send(PacketDistributor.SERVER.noArg(), msg);
    }
    public static void sendToPlayer(Object msg, ServerPlayer player){
        INSTANCE.send(PacketDistributor.PLAYER.with(()->player), msg);
    }
    public static void sendToAllClients(Object msg){
        INSTANCE.send(PacketDistributor.ALL.noArg(), msg);
    }
}
