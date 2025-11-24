package net.bustin.better_markers.mixin;


import iskallia.vault.block.MapMarkerBlock;
import iskallia.vault.core.vault.Vault;
import iskallia.vault.core.vault.VaultUtils;
import iskallia.vault.core.vault.WaypointsList;
import iskallia.vault.world.data.ServerVaults;
import net.bustin.better_markers.BetterMarkers;
import net.bustin.better_markers.util.MarkerNameHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;


@Mixin(MapMarkerBlock.class)
public class MapMarkerBlockMixin {

    @Inject(method = "setPlacedBy", at = @At("TAIL"))
    private void bettermarkers$onPlaced(Level pLevel, BlockPos pPos, BlockState pState,
                                        LivingEntity placer, ItemStack stack, CallbackInfo ci) {

        // 1) Copy custom name from item → block entity (your original logic)
        if (!pLevel.isClientSide && stack.hasCustomHoverName()) {
            Component name = stack.getHoverName();

            // 🔹 DEBUG: log the item name used to place the block
            BetterMarkers.LOGGER.info(
                    "[BetterMarkers] Map Marker placed at {} with item name: {}",
                    pPos, name.getString()
            );

            BlockEntity be = pLevel.getBlockEntity(pPos);
            if (be instanceof MarkerNameHolder holder) {
                holder.bm$setCustomName(name);

                // 🔹 DEBUG: confirm we stored it on the BE
                Component stored = holder.bm$getCustomName();
                BetterMarkers.LOGGER.info(
                        "[BetterMarkers] Stored custom marker name on BE at {}: {}",
                        pPos,
                        stored != null ? stored.getString() : "<null>"
                );
            } else {
                // 🔹 DEBUG: helps if the BE isn't what we expect
                BetterMarkers.LOGGER.warn(
                        "[BetterMarkers] Expected MarkerNameHolder at {}, but got {}",
                        pPos,
                        be != null ? be.getClass().getName() : "null"
                );
            }
        }

        // 2) Your existing vault / waypoint sync block – untouched
        if (!pLevel.isClientSide && VaultUtils.isVaultLevel(pLevel)) {

            Optional<Vault> vaultOpt = ServerVaults.get(pLevel);
            if (vaultOpt.isEmpty()) {
                return;
            }
            Vault vault = vaultOpt.get();

            BlockEntity be = pLevel.getBlockEntity(pPos);
            if (!(be instanceof MarkerNameHolder markerNameBE)) {
                return;
            }

            vault.modifyIfPresent(Vault.MAP_WAYPOINTS, waypoints -> {
                WaypointsList.Waypoint wp = waypoints.get(pPos);

                // 🔹 DEBUG: see if the waypoint exists yet
                BetterMarkers.LOGGER.info(
                        "[BetterMarkers] modifyIfPresent: waypoint at {} is {}",
                        pPos,
                        wp != null ? "present" : "null"
                );

                if (wp instanceof MarkerNameHolder holder) {
                    Component customName = markerNameBE.bm$getCustomName();

                    BetterMarkers.LOGGER.info(
                            "[BetterMarkers] Attempting to push custom name '{}' into waypoint at {}",
                            customName != null ? customName.getString() : "<null>",
                            pPos
                    );

                    holder.bm$setCustomName(customName);
                }
                return waypoints;
            });
        }
    }
}