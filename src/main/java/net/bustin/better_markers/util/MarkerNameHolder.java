package net.bustin.better_markers.util;


import javax.annotation.Nullable;

public interface MarkerNameHolder {
    @Nullable
    net.minecraft.network.chat.Component bm$getCustomName();

    void bm$setCustomName(@Nullable net.minecraft.network.chat.Component name);
}
