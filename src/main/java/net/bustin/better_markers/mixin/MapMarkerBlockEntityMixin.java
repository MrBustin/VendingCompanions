package net.bustin.better_markers.mixin;


import iskallia.vault.block.entity.MapMarkerBlockEntity;
import net.bustin.better_markers.util.MarkerIconType;
import net.bustin.better_markers.util.MarkerIconTypeHolder;
import net.bustin.better_markers.util.MarkerNameHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;

@Mixin(MapMarkerBlockEntity.class)
public abstract class MapMarkerBlockEntityMixin extends BlockEntity implements MarkerNameHolder, MarkerIconTypeHolder {

    @Nullable
    private Component bm$customName;

    @Unique
    private MarkerIconType bm$iconType = MarkerIconType.DEFAULT;

    private MapMarkerBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    // --- MarkerNameHolder impl

    @Override
    public @Nullable Component bm$getCustomName() {
        return bm$customName;
    }

    @Override
    public void bm$setCustomName(@Nullable Component name) {
        this.bm$customName = name;
        setChanged();
    }

    // --- MarkerIconTypeHolder impl (NEW) ---

    @Override
    public MarkerIconType bm$getIconType() {
        return bm$iconType;
    }

    @Override
    public void bm$setIconType(MarkerIconType type) {
        this.bm$iconType = (type == null ? MarkerIconType.DEFAULT : type);
        setChanged();
    }

    // --- NBT save/load (extend your existing ones) ---

    @Inject(method = "saveAdditional", at = @At("TAIL"))
    private void bettermarkers$saveNameAndIcon(CompoundTag tag, CallbackInfo ci) {
        if (bm$customName != null) {
            tag.putString("BM_CustomName", Component.Serializer.toJson(bm$customName));
        }
        tag.putString("BM_IconType", bm$iconType.name());
    }

    @Inject(method = "load", at = @At("TAIL"))
    private void bettermarkers$loadNameAndIcon(CompoundTag tag, CallbackInfo ci) {
        if (tag.contains("BM_CustomName", Tag.TAG_STRING)) {
            bm$customName = Component.Serializer.fromJson(tag.getString("BM_CustomName"));
        }
        if (tag.contains("BM_IconType", Tag.TAG_STRING)) {
            try {
                bm$iconType = MarkerIconType.valueOf(tag.getString("BM_IconType"));
            } catch (IllegalArgumentException e) {
                bm$iconType = MarkerIconType.DEFAULT;
            }
        }
    }

    @Inject(method = "getUpdateTag", at = @At("RETURN"), cancellable = true)
    private void bettermarkers$addNameAndIconToUpdateTag(CallbackInfoReturnable<CompoundTag> cir) {
        CompoundTag tag = cir.getReturnValue();
        if (bm$customName != null) {
            tag.putString("BM_CustomName", Component.Serializer.toJson(bm$customName));
        }
        tag.putString("BM_IconType", bm$iconType.name());
        cir.setReturnValue(tag);
    }
}
