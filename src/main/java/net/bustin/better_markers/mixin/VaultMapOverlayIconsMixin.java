package net.bustin.better_markers.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import iskallia.vault.client.atlas.TextureAtlasRegion;
import iskallia.vault.client.gui.framework.ScreenRenderers;
import iskallia.vault.client.gui.framework.spatial.Spatials;
import iskallia.vault.client.gui.framework.spatial.spi.ISpatial;
import iskallia.vault.client.gui.overlay.VaultMapOverlay;
import iskallia.vault.client.render.HudPosition;
import iskallia.vault.client.render.IVaultOptions;
import iskallia.vault.core.vault.ClientVaults;
import iskallia.vault.core.vault.Vault;
import iskallia.vault.core.vault.WaypointsList;
import iskallia.vault.core.vault.WorldManager;
import iskallia.vault.core.vault.player.Listener;
import iskallia.vault.core.vault.stat.DiscoveredRoomStat;
import iskallia.vault.core.vault.stat.DiscoveredTunnelStat;
import iskallia.vault.core.world.generator.GridGenerator;
import iskallia.vault.event.InputEvents;
import iskallia.vault.init.ModTextureAtlases;
import iskallia.vault.util.VectorHelper;
import net.bustin.better_markers.BetterMarkers;
import net.bustin.better_markers.util.MarkerIconType;
import net.bustin.better_markers.util.MarkerIconTypeHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec2;
import net.minecraftforge.client.gui.ForgeIngameGui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.Optional;

@Mixin(value = VaultMapOverlay.class, remap = false)
public abstract class VaultMapOverlayIconsMixin {

    private static final int MINIMAP_ICON_SIZE = 5;
    private static final int BIGMAP_ICON_SIZE = 8;

    // -----------------------------------------------------
    //                  MINIMAP ICON RENDERING
    // -----------------------------------------------------

    @Inject(method = "render", at = @At("TAIL"))
    private void bettermarkers$renderMinimapIcons(
            ForgeIngameGui gui,
            PoseStack poseStack,
            float partialTick,
            int width,
            int height,
            CallbackInfo ci
    ) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) return;

        IVaultOptions options = (IVaultOptions) mc.options;

        if (options.getMapVisibilityOption() == VaultMapOverlay.MapVisibilityOption.NEVER ||
                options.getMapVisibilityOption() == VaultMapOverlay.MapVisibilityOption.XAEROS)
            return;

        if (options.getMapVisibilityOption() == VaultMapOverlay.MapVisibilityOption.TAB &&
                !InputEvents.isIsTabDown())
            return;

        Optional<Vault> vaultOpt = ClientVaults.getActive();
        if (vaultOpt.isEmpty()) return;
        Vault vault = vaultOpt.get();

        Object waypointsObj = vault.get(Vault.MAP_WAYPOINTS);
        if (!(waypointsObj instanceof WaypointsList waypoints) || waypoints.isEmpty()) return;

        // --- minimap transform
        HudPosition minimapPos = options.getMinimapPosition();
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();

        poseStack.pushPose();
        poseStack.translate(minimapPos.getScaledX(sw), minimapPos.getScaledY(sh), 0);
        poseStack.scale(options.getMapScale(), options.getMapScale(), 1);

        Direction facing = vault.getOptional(Vault.WORLD)
                .map(mgr -> (Direction) mgr.getOr(WorldManager.FACING, Direction.NORTH))
                .orElse(Direction.NORTH);
        float rotationOffset = 180.0F - facing.toYRot();

        Object generatorObj = vault.get(Vault.WORLD).get(WorldManager.GENERATOR);
        if (!(generatorObj instanceof GridGenerator generator)) {
            poseStack.popPose();
            return;
        }

        BlockPos playerPos = player.blockPosition();
        float ex = (float) (playerPos.getX() - (Integer) generator.get(GridGenerator.CELL_X) / 2);
        float ez = (float) (playerPos.getZ() - (Integer) generator.get(GridGenerator.CELL_Z) / 2);
        Vec2 playerOffset = VectorHelper.rotateDegrees(new Vec2(ex / 47f, ez / 47f), rotationOffset);

        float zoom = options.getMapZoom();

        poseStack.pushPose();
        poseStack.translate(
                34f - 4f * zoom - playerOffset.x * 8f * zoom,
                34f - 4f * zoom - playerOffset.y * 8f * zoom,
                0
        );
        poseStack.scale(zoom, zoom, 1);

        float scaleFactor = 8.0F;

        // ---------- RENDER ICONS ----------
        for (Map.Entry<BlockPos, WaypointsList.Waypoint> entry : waypoints.entrySet()) {
            BlockPos wpPos = entry.getKey();

            BlockEntity be = mc.level.getBlockEntity(wpPos);
            if (!(be instanceof MarkerIconTypeHolder iconHolder)) continue;

            MarkerIconType type = iconHolder.bm$getIconType();
            if (type == MarkerIconType.DEFAULT) continue;

            // calculate minimap position
            float relX = (float) wpPos.getX() / 47f * scaleFactor;
            float relZ = (float) wpPos.getZ() / 47f * scaleFactor;

            Vec2 wpOff = VaultMapOverlay.getWaypointOffset(facing);
            wpOff = new Vec2(wpOff.x * scaleFactor / 8f, wpOff.y * scaleFactor / 8f);

            Vec2 rotatedPos = VectorHelper.rotateDegrees(new Vec2(relX, relZ), rotationOffset);
            Vec2 rotatedOff = VectorHelper.rotateDegrees(wpOff, rotationOffset);

            relX = rotatedPos.x + rotatedOff.x;
            relZ = rotatedPos.y + rotatedOff.y;

            // ---------- DIRECT TEXTURE BLIT ----------
            ResourceLocation iconTex = getIconTexture(type);

            BetterMarkers.LOGGER.info(
                    "[BetterMarkers][DEBUG] Blitting MINIMAP icon {} with texture {}",
                    type.name(), iconTex
            );

            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.setShaderTexture(0, iconTex);
            RenderSystem.setShaderColor(1, 1, 1, 1);

            int drawX = (int) (relX - MINIMAP_ICON_SIZE / 2f);
            int drawY = (int) (relZ - MINIMAP_ICON_SIZE / 2f);

            GuiComponent.blit(
                    poseStack,
                    drawX, drawY,
                    0, 0,
                    MINIMAP_ICON_SIZE, MINIMAP_ICON_SIZE,
                    MINIMAP_ICON_SIZE, MINIMAP_ICON_SIZE
            );
        }

        poseStack.popPose();
        poseStack.popPose();
    }


    // -----------------------------------------------------
    //                  BIG MAP ICON RENDERING
    // -----------------------------------------------------

    @Inject(method = "renderIngameLargeMapOverlay", at = @At("TAIL"))
    private void bettermarkers$renderBigMapIcons(
            PoseStack poseStack,
            Vault vault,
            DiscoveredRoomStat room,
            DiscoveredTunnelStat tunnels,
            CallbackInfo ci
    ) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) return;

        Object waypointsObj = vault.get(Vault.MAP_WAYPOINTS);
        if (!(waypointsObj instanceof WaypointsList waypoints) || waypoints.isEmpty()) return;

        Direction facing = vault.getOptional(Vault.WORLD)
                .map(mgr -> (Direction) mgr.getOr(WorldManager.FACING, Direction.NORTH))
                .orElse(Direction.NORTH);
        float rotationOffset = 180.0F - facing.toYRot();

        Object generatorObj = vault.get(Vault.WORLD).get(WorldManager.GENERATOR);
        if (!(generatorObj instanceof GridGenerator generator)) return;

        BlockPos playerPos = player.blockPosition();
        float ex = (float) (playerPos.getX() - (Integer) generator.get(GridGenerator.CELL_X) / 2);
        float ez = (float) (playerPos.getZ() - (Integer) generator.get(GridGenerator.CELL_Z) / 2);
        Vec2 centerOffset = VectorHelper.rotateDegrees(new Vec2(ex / 47f, ez / 47f), rotationOffset);

        poseStack.pushPose();
        float scaleFactor = 8f;
        poseStack.translate(-centerOffset.x * scaleFactor, -centerOffset.y * scaleFactor, 0);

        // ---------- DRAW BIG MAP ICONS ----------
        for (Map.Entry<BlockPos, WaypointsList.Waypoint> entry : waypoints.entrySet()) {
            BlockPos wpPos = entry.getKey();

            BlockEntity be = mc.level.getBlockEntity(wpPos);
            if (!(be instanceof MarkerIconTypeHolder iconHolder)) continue;

            MarkerIconType type = iconHolder.bm$getIconType();
            if (type == MarkerIconType.DEFAULT) continue;

            float relX = (float) wpPos.getX() / 47f * scaleFactor;
            float relZ = (float) wpPos.getZ() / 47f * scaleFactor;

            Vec2 wpOff = VaultMapOverlay.getWaypointOffset(facing);
            wpOff = new Vec2(wpOff.x * scaleFactor / 8f, wpOff.y * scaleFactor / 8f);

            Vec2 rotatedPos = VectorHelper.rotateDegrees(new Vec2(relX, relZ), rotationOffset);
            Vec2 rotatedOff = VectorHelper.rotateDegrees(wpOff, rotationOffset);

            relX = rotatedPos.x + rotatedOff.x;
            relZ = rotatedPos.y + rotatedOff.y;

            // --- DIRECT BLIT ---
            ResourceLocation iconTex = getIconTexture(type);

            BetterMarkers.LOGGER.info(
                    "[BetterMarkers][DEBUG] Blitting BIG MAP icon {} with texture {}",
                    type.name(), iconTex
            );

            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.setShaderTexture(0, iconTex);
            RenderSystem.setShaderColor(1, 1, 1, 1);

            int drawX = (int) (relX - BIGMAP_ICON_SIZE / 2f);
            int drawY = (int) (relZ - BIGMAP_ICON_SIZE / 2f);

            GuiComponent.blit(
                    poseStack,
                    drawX, drawY,
                    0, 0,
                    BIGMAP_ICON_SIZE, BIGMAP_ICON_SIZE,
                    BIGMAP_ICON_SIZE, BIGMAP_ICON_SIZE
            );
        }

        poseStack.popPose();
    }


    // -----------------------------------------------------
    //     Helper: get actual texture PNG from enum type
    // -----------------------------------------------------

    private ResourceLocation getIconTexture(MarkerIconType type) {
        String path = "textures/" + type.getTexturePath() + ".png";
        return new ResourceLocation(BetterMarkers.MOD_ID, path);
    }
}


