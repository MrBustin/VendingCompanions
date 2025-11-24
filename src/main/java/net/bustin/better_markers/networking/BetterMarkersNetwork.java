package net.bustin.better_markers.networking;

import net.bustin.better_markers.BetterMarkers;
import net.bustin.better_markers.networking.messages.MarkerPlacedMessage;
import net.bustin.better_markers.networking.messages.OpenSelectionScreenMessage;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;

public class BetterMarkersNetwork {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(BetterMarkers.MOD_ID, "network"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int id = 0;

    private static int nextId() {
        return id++;
    }

    public static void register() {
        BetterMarkers.LOGGER.info("[BetterMarkers] Registering network channel");
        CHANNEL.registerMessage(
                nextId(),
                OpenSelectionScreenMessage.class,
                OpenSelectionScreenMessage::encode,
                OpenSelectionScreenMessage::decode,
                OpenSelectionScreenMessage::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
                nextId(),
                MarkerPlacedMessage.class,
                MarkerPlacedMessage::encode,
                MarkerPlacedMessage::decode,
                MarkerPlacedMessage::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
    }

    public static <MSG> void sendToPlayer(MSG msg, ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), msg);
    }
}
