package net.bustin.better_markers.logic;

import iskallia.vault.block.MapMarkerBlock;
import net.bustin.better_markers.BetterMarkers;
import net.bustin.better_markers.networking.BetterMarkersNetwork;
import net.bustin.better_markers.networking.messages.OpenSelectionScreenMessage;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

@Mod.EventBusSubscriber(modid = BetterMarkers.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class MarkerRightClickHandler {

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {

        // Only main hand
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }

        Player player = event.getPlayer();
        Level level = event.getWorld();     // if this errors, use event.getWorld()
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);

        // Only our target block
        if (!(state.getBlock() instanceof MapMarkerBlock)) {
            return;
        }

        // Server side: send packet to open screen
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            BetterMarkers.LOGGER.info("[BetterMarkers] Right-clicked Map Marker at {}", pos);

            BetterMarkersNetwork.CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> serverPlayer),
                    new OpenSelectionScreenMessage(pos)
            );
        }

        // Mark the interaction as handled
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }
}
