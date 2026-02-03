package com.snog.temporalengineering.common.network;

import com.snog.temporalengineering.TemporalEngineering;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.SimpleChannel;

public final class ModNetwork
{
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
        new ResourceLocation(TemporalEngineering.MODID, "main"),
        () -> PROTOCOL_VERSION,
        PROTOCOL_VERSION::equals,
        PROTOCOL_VERSION::equals
    );

    private static int nextId = 0;

    private ModNetwork()
    {
    }

    public static void register()
    {
        CHANNEL.messageBuilder(ToggleFieldAreaPacket.class, nextId++)
            .encoder(ToggleFieldAreaPacket::encode)
            .decoder(ToggleFieldAreaPacket::decode)
            .consumerMainThread(ToggleFieldAreaPacket::handle)
            .add();
    }
}