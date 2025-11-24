package net.bustin.better_markers.logic;

import iskallia.vault.init.ModTextureAtlases;
import net.bustin.better_markers.BetterMarkers;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;


@Mod.EventBusSubscriber(
        modid = BetterMarkers.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT
)
public class BetterMarkersClientTextures {

    @SubscribeEvent
    public static void onTextureStitchPre(TextureStitchEvent.Pre event) {
        ResourceLocation atlasLoc = event.getAtlas().location();

        BetterMarkers.LOGGER.info(
                "[BetterMarkers][DEBUG] TextureStitchEvent.Pre for atlas {}",
                atlasLoc
        );

        // For now, don't filter – just add to every atlas this is called for.
        event.addSprite(new ResourceLocation(BetterMarkers.MOD_ID, "gui/map/marker_default"));
        event.addSprite(new ResourceLocation(BetterMarkers.MOD_ID, "gui/map/gilded"));
        event.addSprite(new ResourceLocation(BetterMarkers.MOD_ID, "gui/map/living"));
        event.addSprite(new ResourceLocation(BetterMarkers.MOD_ID, "gui/map/ornate"));
        event.addSprite(new ResourceLocation(BetterMarkers.MOD_ID, "gui/map/wooden"));
    }
}


