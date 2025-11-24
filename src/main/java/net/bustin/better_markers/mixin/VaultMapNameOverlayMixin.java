package net.bustin.better_markers.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import iskallia.vault.client.gui.overlay.VaultMapOverlay;
import iskallia.vault.client.render.HudPosition;
import iskallia.vault.client.render.IVaultOptions;
import iskallia.vault.core.vault.ClientVaults;
import iskallia.vault.core.vault.Vault;
import iskallia.vault.core.vault.WaypointsList;
import iskallia.vault.core.vault.WorldManager;
import iskallia.vault.core.vault.stat.DiscoveredRoomStat;
import iskallia.vault.core.vault.stat.DiscoveredTunnelStat;
import iskallia.vault.core.world.generator.GridGenerator;
import iskallia.vault.event.InputEvents;
import iskallia.vault.init.ModItems;
import iskallia.vault.init.ModKeybinds;
import iskallia.vault.util.VectorHelper;
import net.bustin.better_markers.BetterMarkers;
import net.bustin.better_markers.util.MarkerNameHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec2;
import net.minecraftforge.client.gui.ForgeIngameGui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Mixin(value = VaultMapOverlay.class, remap = false)
public abstract class VaultMapNameOverlayMixin {

    @Inject(method = "render", at = @At("TAIL"))
    private void bettermarkers$renderMinimapWaypointNames(ForgeIngameGui gui, PoseStack poseStack, float partialTick, int width, int height, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        IVaultOptions options = (IVaultOptions) mc.options;

        if (options.getMapVisibilityOption() == VaultMapOverlay.MapVisibilityOption.NEVER
                || options.getMapVisibilityOption() == VaultMapOverlay.MapVisibilityOption.XAEROS) {
            return;
        }
        if (options.getMapVisibilityOption() == VaultMapOverlay.MapVisibilityOption.TAB
                && !InputEvents.isIsTabDown()) {
            return;
        }

        Optional<Vault> vaultOpt = ClientVaults.getActive();
        if (vaultOpt.isEmpty()) return;
        Vault vault = vaultOpt.get();

        Object waypointsObj = vault.get(Vault.MAP_WAYPOINTS);
        if (!(waypointsObj instanceof WaypointsList waypoints) || waypoints.isEmpty()) {
            return;
        }



        if (mc.level != null) {
            for (Map.Entry<BlockPos, WaypointsList.Waypoint> entry : waypoints.entrySet()) {
                BlockPos pos = entry.getKey();
                WaypointsList.Waypoint wp = entry.getValue();

                BlockEntity be = mc.level.getBlockEntity(pos);

                if (be == null) {

                    continue;
                }

                if (!(be instanceof MarkerNameHolder beHolder)) {

                    continue;
                }

                Component name = beHolder.bm$getCustomName();


                if (name != null) {
                    ((MarkerNameHolder) (Object) wp).bm$setCustomName(name);

                }
            }
        }

        HudPosition minimapPos = options.getMinimapPosition();
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();

        poseStack.pushPose();
        poseStack.translate(minimapPos.getScaledX(sw), minimapPos.getScaledY(sh), 0.0F);
        float mapScale = options.getMapScale();
        poseStack.scale(mapScale, mapScale, 1.0F);

        Direction facing = vault.getOptional(Vault.WORLD)
                .map(mgr -> (Direction) mgr.getOr(WorldManager.FACING, Direction.NORTH))
                .orElse(Direction.NORTH);
        float rotationOffset = 180.0F - facing.toYRot();

        Object worldMgrObj = vault.get(Vault.WORLD);
        if (!(worldMgrObj instanceof WorldManager worldManager)) {
            poseStack.popPose();
            return;
        }
        Object generatorObj = worldManager.get(WorldManager.GENERATOR);
        if (!(generatorObj instanceof GridGenerator generator)) {
            poseStack.popPose();
            return;
        }

        BlockPos playerPos = player.blockPosition();
        float exactX = (float) (playerPos.getX() - (Integer) generator.get(GridGenerator.CELL_X) / 2);
        float exactZ = (float) (playerPos.getZ() - (Integer) generator.get(GridGenerator.CELL_Z) / 2);
        float interpX = exactX / 47.0F;
        float interpZ = exactZ / 47.0F;
        Vec2 playerOffset = VectorHelper.rotateDegrees(new Vec2(interpX, interpZ), rotationOffset);

        float zoom = options.getMapZoom();

        poseStack.pushPose();
        poseStack.translate(
                34.0F - 4.0F * zoom - playerOffset.x * 8.0F * zoom,
                34.0F - 4.0F * zoom - playerOffset.y * 8.0F * zoom,
                0.0F
        );
        poseStack.scale(zoom, zoom, 1.0F);

        Font font = mc.font;
        float scaleFactor = 8.0F;

        for (Map.Entry<BlockPos, WaypointsList.Waypoint> entry : waypoints.entrySet()) {
            BlockPos pos = entry.getKey();
            WaypointsList.Waypoint wp = entry.getValue();

            MarkerNameHolder holder = (MarkerNameHolder) (Object) wp;
            Component customName = holder.bm$getCustomName();



            if (customName == null || customName.getString().isEmpty()) {

                continue;
            }

            float relX = (float) pos.getX() / 47.0F * scaleFactor;
            float relZ = (float) pos.getZ() / 47.0F * scaleFactor;

            Vec2 waypointOffset = VaultMapOverlay.getWaypointOffset(facing);
            waypointOffset = new Vec2(
                    waypointOffset.x * scaleFactor / 8.0F,
                    waypointOffset.y * scaleFactor / 8.0F
            );

            Vec2 rotatedPosition = VectorHelper.rotateDegrees(new Vec2(relX, relZ), rotationOffset);
            Vec2 rotatedOffset   = VectorHelper.rotateDegrees(waypointOffset, rotationOffset);

            relX = rotatedPosition.x + rotatedOffset.x;
            relZ = rotatedPosition.y + rotatedOffset.y;

            poseStack.pushPose();
            poseStack.translate(relX + 4.0F, relZ - 6.0F, 15.0F);
            float textScale = 0.6F;
            poseStack.scale(textScale, textScale, 1.0F);

            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

            int colour = ((WaypointsList.Waypoint)entry.getValue()).getColour();
            font.draw(poseStack, customName, 0.0F, 0.0F, colour);

            poseStack.popPose();
        }

        poseStack.popPose();
        poseStack.popPose();
    }


    @Inject(method = "renderIngameLargeMapOverlay", at = @At("TAIL"))
    private void bettermarkers$renderBigMapWaypointNames(
            PoseStack poseStack,
            Vault vault,
            DiscoveredRoomStat room,
            DiscoveredTunnelStat tunnels,
            CallbackInfo ci
    ) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        IVaultOptions options = (IVaultOptions) mc.options;

        // ---- get waypoints ----
        Object waypointsObj = vault.get(Vault.MAP_WAYPOINTS);
        if (!(waypointsObj instanceof WaypointsList waypoints) || waypoints.isEmpty()) {
            return;
        }
        if (mc.level == null) return;

        // ---- BE -> waypoint sync (same idea as minimap) ----
        for (Map.Entry<BlockPos, WaypointsList.Waypoint> entry : waypoints.entrySet()) {
            BlockPos pos = entry.getKey();
            WaypointsList.Waypoint wp = entry.getValue();

            BlockEntity be = mc.level.getBlockEntity(pos);
            if (!(be instanceof MarkerNameHolder beHolder)) {
                continue;
            }

            Component nameOnBE = beHolder.bm$getCustomName();
            if (nameOnBE != null) {
                ((MarkerNameHolder) (Object) wp).bm$setCustomName(nameOnBE);
            }
        }

        // ---- rebuild big-map transforms (copied from VH logic) ----
        Direction facing = vault.getOptional(Vault.WORLD)
                .map(mgr -> (Direction) mgr.getOr(WorldManager.FACING, Direction.NORTH))
                .orElse(Direction.NORTH);
        float rotationOffset = 180.0F - facing.toYRot();

        Object worldMgrObj = vault.get(Vault.WORLD);
        if (!(worldMgrObj instanceof WorldManager worldManager)) {
            return;
        }
        Object generatorObj = worldManager.get(WorldManager.GENERATOR);
        if (!(generatorObj instanceof GridGenerator generator)) {
            return;
        }

        BlockPos playerPos = player.blockPosition();
        float exactXPlayer = (float) (playerPos.getX() - (Integer) generator.get(GridGenerator.CELL_X) / 2);
        float exactZPlayer = (float) (playerPos.getZ() - (Integer) generator.get(GridGenerator.CELL_Z) / 2);
        float interpX = exactXPlayer / 47.0F;
        float interpZ = exactZPlayer / 47.0F;
        Vec2 centerOffset = VectorHelper.rotateDegrees(new Vec2(interpX, interpZ), rotationOffset);

        poseStack.pushPose();

        HudPosition overlayPos = options.mapOverlayPosition();
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();

        IVaultOptions.ScaleMode scaleMode = options.getMapOverlayScale();
        if (scaleMode == IVaultOptions.ScaleMode.SMALL) {
            poseStack.scale(0.5F, 0.5F, 1.0F);
            poseStack.translate(
                    overlayPos.getScaledX(sw) / 0.5F,
                    overlayPos.getScaledY(sh) / 0.5F,
                    0.0F
            );
        } else if (scaleMode == IVaultOptions.ScaleMode.TINY) {
            poseStack.scale(0.25F, 0.25F, 1.0F);
            poseStack.translate(
                    overlayPos.getScaledX(sw) / 0.25F,
                    overlayPos.getScaledY(sh) / 0.25F,
                    0.0F
            );
        } else {
            poseStack.translate(
                    overlayPos.getScaledX(sw),
                    overlayPos.getScaledY(sh),
                    0.0F
            );
        }

        float scaleFactor = 8.0F;
        poseStack.translate(-centerOffset.x * scaleFactor, -centerOffset.y * scaleFactor, 0.0F);

        Font font = mc.font;

        // ---- draw labels on the big overlay ----
        for (Map.Entry<BlockPos, WaypointsList.Waypoint> entry : waypoints.entrySet()) {
            BlockPos pos = entry.getKey();
            WaypointsList.Waypoint wp = entry.getValue();

            MarkerNameHolder holder = (MarkerNameHolder) (Object) wp;
            Component customName = holder.bm$getCustomName();
            if (customName == null || customName.getString().isEmpty()) {
                continue;
            }

            float relX = (float) pos.getX() / 47.0F * scaleFactor;
            float relZ = (float) pos.getZ() / 47.0F * scaleFactor;

            Vec2 waypointOffset = VaultMapOverlay.getWaypointOffset(facing);
            // big map uses same 8px-per-room basis; scale offsets up
            waypointOffset = new Vec2(
                    waypointOffset.x * scaleFactor / 8.0F,
                    waypointOffset.y * scaleFactor / 8.0F
            );

            Vec2 rotatedPosition = VectorHelper.rotateDegrees(new Vec2(relX, relZ), rotationOffset);
            Vec2 rotatedOffset   = VectorHelper.rotateDegrees(waypointOffset, rotationOffset);

            relX = rotatedPosition.x + rotatedOffset.x;
            relZ = rotatedPosition.y + rotatedOffset.y;

            poseStack.pushPose();
            // tweak offsets so label sits nicely beside the marker
            poseStack.translate(relX + 6.0F, relZ - 6.0F, 11.0F);
            float textScale = 0.9F;
            poseStack.scale(textScale, textScale, 1.0F);

            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

            int colour = ((WaypointsList.Waypoint)entry.getValue()).getColour();
            font.draw(poseStack, customName, 0.0F, 0.0F,colour);

            poseStack.popPose();
        }

        poseStack.popPose();
    }
}






