package net.bustin.better_markers.mixin;

import iskallia.vault.core.vault.WaypointsList;
import net.bustin.better_markers.util.MarkerNameHolder;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;


@Mixin(value = WaypointsList.Waypoint.class, remap = false)
public class WaypointMixin implements MarkerNameHolder {

    @Unique
    private Component bm$customName;

    @Override
    public Component bm$getCustomName() {
        return bm$customName;
    }

    @Override
    public void bm$setCustomName(Component name) {
        this.bm$customName = name;
    }
}
