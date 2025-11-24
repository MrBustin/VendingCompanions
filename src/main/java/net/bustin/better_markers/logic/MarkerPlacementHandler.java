package net.bustin.better_markers.logic;

import net.bustin.better_markers.BetterMarkers;
import net.bustin.better_markers.networking.BetterMarkersNetwork;
import net.bustin.better_markers.networking.messages.MarkerPlacedMessage;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class MarkerPlacementHandler {

    public static void onMarkerPlaced(Level level, BlockPos pos, BlockState state, Player player, ItemStack stack) {

        if (level.isClientSide) {
            return; // run on server only
        }

        BetterMarkers.LOGGER.info(
                "[BetterMarkers] MarkerPlacementHandler called for {} at {}",
                player.getGameProfile().getName(), pos
        );

        // TODO: replace "Test Marker" with label from your GUI / item NBT
        String label = "Test Marker";

        BetterMarkersNetwork.sendToPlayer(
                new MarkerPlacedMessage(pos, label),
                (ServerPlayer) player
        );
    }
}