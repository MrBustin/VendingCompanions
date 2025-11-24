package net.bustin.better_markers.networking.messages;

import iskallia.vault.core.vault.ClientVaults;
import iskallia.vault.core.vault.Vault;
import iskallia.vault.core.vault.WaypointsList;
import net.bustin.better_markers.BetterMarkers;
import net.bustin.better_markers.util.MarkerNameHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.TextComponent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkEvent;

import java.util.Optional;
import java.util.function.Supplier;

public class MarkerPlacedMessage {

    private final BlockPos pos;
    private final String label;

    public MarkerPlacedMessage(BlockPos pos, String label) {
        this.pos = pos;
        this.label = label;
    }

    // --- Encode / decode ---

    public static void encode(MarkerPlacedMessage msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeUtf(msg.label, 64); // 64-char limit is plenty for a label
    }

    public static MarkerPlacedMessage decode(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        String label = buf.readUtf(64);
        return new MarkerPlacedMessage(pos, label);
    }

    // --- Handle (client side) ---

    public static void handle(MarkerPlacedMessage msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();

        if (ctx.getDirection().getReceptionSide() != LogicalSide.CLIENT) {
            ctx.setPacketHandled(true);
            return;
        }

        ctx.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            ClientLevel level = mc.level;
            if (level == null) return;

            Optional<Vault> opt = ClientVaults.getActive();
            if (opt.isEmpty()) return;
            Vault vault = opt.get();

            Object waypointsObj = vault.get(Vault.MAP_WAYPOINTS);
            if (!(waypointsObj instanceof WaypointsList waypoints) || waypoints.isEmpty()) {
                return;
            }

            WaypointsList.Waypoint wp = waypoints.get(msg.pos);
            if (wp == null) {
                BetterMarkers.LOGGER.info(
                        "[BetterMarkers] No waypoint at {} yet when label '{}' arrived – skipping",
                        msg.pos, msg.label
                );
                return;
            }

            ((MarkerNameHolder) (Object) wp)
                    .bm$setCustomName(new TextComponent(msg.label));

            BetterMarkers.LOGGER.info(
                    "[BetterMarkers] Set label '{}' for waypoint at {}",
                    msg.label, msg.pos
            );
        });

        ctx.setPacketHandled(true);
    }
}
