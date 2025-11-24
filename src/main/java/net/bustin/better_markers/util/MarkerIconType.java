package net.bustin.better_markers.util;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;

public enum MarkerIconType {
    DEFAULT("default", "gui/map/marker_default", "bettermappers.marker.icon.default"),
    CHEST("chest", "gui/map/gilded", "bettermappers.marker.icon.chest");

    private final String id;
    private final String texturePath;
    private final String translationKey;

    MarkerIconType(String id, String texturePath, String translationKey) {
        this.id = id;
        this.texturePath = texturePath;
        this.translationKey = translationKey;
    }

    public String getId() {
        return id;
    }

    public ResourceLocation getTextureLocation(String modid) {
        return new ResourceLocation(modid, texturePath);
    }


    public Component getDisplayName() {
        return new TranslatableComponent(translationKey);
    }

    public String getTexturePath() {
        return texturePath;
    }
}
