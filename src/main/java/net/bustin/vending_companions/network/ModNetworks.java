package net.bustin.vending_companions.network;

import net.bustin.vending_companions.network.c2s.ChangeCompanionVariantC2SPacket;
import net.bustin.vending_companions.network.c2s.EquipCompanionC2SPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class ModNetworks {

    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation("vendingcompanions", "main"), // your modid
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;

    private static int nextId() {
        return packetId++;
    }

    public static void register() {
        CHANNEL.registerMessage(
                packetId++,
                ChangeCompanionVariantC2SPacket.class,
                ChangeCompanionVariantC2SPacket::toBytes,
                ChangeCompanionVariantC2SPacket::new,
                ChangeCompanionVariantC2SPacket::handle
        );
        CHANNEL.registerMessage(
                packetId++,
                EquipCompanionC2SPacket.class,
                EquipCompanionC2SPacket::toBytes,
                EquipCompanionC2SPacket::new,
                EquipCompanionC2SPacket::handle
        );
    }

    public static void onCommonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(ModNetworks::register);
    }
}
